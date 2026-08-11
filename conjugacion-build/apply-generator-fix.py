#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])

py = root / "tools" / "generate_library.py"
s = py.read_text(encoding="utf-8")
old = "not (v[:-2] if v.endswith('se') else v).endswith((\"ar\", \"er\", \"ir\"))"
new = "not (v[:-2] if v.endswith('se') else v).endswith((\"ar\", \"er\", \"ir\", \"ír\"))"
if old not in s:
    raise SystemExit("Python validation target not found")
py.write_text(s.replace(old, new, 1), encoding="utf-8")

js = root / "tools" / "generate_paradigms.js"
s = js.read_text(encoding="utf-8")
anchor = "function resultTables(verb, region) {"
if anchor not in s:
    raise SystemExit("JS helper anchor missing")
helpers = r'''
const ACUTE_TO_PLAIN = { 'á':'a', 'é':'e', 'í':'i', 'ó':'o', 'ú':'u', 'Á':'A', 'É':'E', 'Í':'I', 'Ó':'O', 'Ú':'U' };
const PLAIN_TO_ACUTE = { 'a':'á', 'e':'é', 'i':'í', 'o':'ó', 'u':'ú', 'A':'Á', 'E':'É', 'I':'Í', 'O':'Ó', 'U':'Ú' };

function stripAcute(word) {
  return [...word].map(ch => ACUTE_TO_PLAIN[ch] || ch).join('');
}
function isVowel(ch) {
  return /[aeiouüáéíóúAEIOUÜÁÉÍÓÚ]/.test(ch || '');
}
function isStrongVowel(ch) {
  return /[aáeéoóAÁEÉOÓ]/.test(ch || '');
}
function isAccentedWeak(ch) {
  return /[íúÍÚ]/.test(ch || '');
}
function vowelNuclei(word) {
  const nuclei = [];
  let previousVowelIndex = -2;
  for (let i = 0; i < word.length; i++) {
    const ch = word[i];
    if (!isVowel(ch)) {
      previousVowelIndex = -2;
      continue;
    }
    const previous = i > 0 ? word[i - 1] : '';
    const startsNew = nuclei.length === 0 || previousVowelIndex !== i - 1 ||
      isAccentedWeak(ch) || isAccentedWeak(previous) ||
      (isStrongVowel(ch) && isStrongVowel(previous));
    if (startsNew) nuclei.push([i]); else nuclei[nuclei.length - 1].push(i);
    previousVowelIndex = i;
  }
  return nuclei;
}
function naturalStressNucleus(word, nuclei) {
  if (!nuclei.length) return -1;
  const plain = stripAcute(word).toLowerCase();
  const penultimate = /[aeiouns]$/.test(plain);
  return penultimate && nuclei.length > 1 ? nuclei.length - 2 : nuclei.length - 1;
}
function stressCarrier(word) {
  const accentedIndex = [...word].findIndex(ch => /[áéíóúÁÉÍÓÚ]/.test(ch));
  if (accentedIndex >= 0) {
    return { index: accentedIndex, forceWeakAccent: isAccentedWeak(word[accentedIndex]) };
  }
  const nuclei = vowelNuclei(word);
  const nucleusIndex = naturalStressNucleus(word, nuclei);
  if (nucleusIndex < 0) return { index: Math.max(0, word.length - 1), forceWeakAccent: false };
  const nucleus = nuclei[nucleusIndex];
  const strong = nucleus.find(index => isStrongVowel(word[index]));
  return { index: strong === undefined ? nucleus[nucleus.length - 1] : strong, forceWeakAccent: false };
}
function attachPreservingStress(command, pronoun, dropFinal = '') {
  const original = clean(command);
  if (!original) return null;
  const stress = stressCarrier(original);
  let stem = stripAcute(original);
  if (dropFinal && stem.toLowerCase().endsWith(dropFinal.toLowerCase())) stem = stem.slice(0, -dropFinal.length);
  let combined = stem + pronoun;
  const nuclei = vowelNuclei(combined);
  const targetNucleus = nuclei.findIndex(nucleus => nucleus.includes(stress.index));
  const natural = naturalStressNucleus(combined, nuclei);
  if ((targetNucleus !== natural || stress.forceWeakAccent) && PLAIN_TO_ACUTE[combined[stress.index]]) {
    combined = combined.slice(0, stress.index) + PLAIN_TO_ACUTE[combined[stress.index]] + combined.slice(stress.index + 1);
  }
  return combined;
}
function reflexivePronoun(person) {
  return { YO:'me', TU:'te', USTED:'se', NOSOTROS:'nos', USTEDES:'se', VOS:'te', VOSOTROS:'os' }[person] || 'se';
}
function reflexivizeValue(key, value, baseVerb) {
  const [tense, person] = key.split('|');
  if (tense === 'NONFINITE_INFINITIVE') return [baseVerb + 'se'];
  if (tense === 'NONFINITE_GERUND') return [attachPreservingStress(value, 'se')];
  if (tense === 'NONFINITE_PARTICIPLE') return [value];
  const pronoun = reflexivePronoun(person);
  if (tense === 'IMPERATIVE_NEGATIVE') {
    const bare = value.replace(/^no\s+/i, '').trim();
    return [`no ${pronoun} ${bare}`];
  }
  if (tense === 'IMPERATIVE_AFFIRMATIVE') {
    if (person === 'NOSOTROS') return [attachPreservingStress(value, pronoun, 's')];
    if (person === 'VOSOTROS') {
      if (baseVerb === 'ir') return ['idos', 'iros'];
      return [attachPreservingStress(value, pronoun, 'd')];
    }
    return [attachPreservingStress(value, pronoun)];
  }
  return [`${pronoun} ${value}`];
}
function reflexivizeForms(forms, baseVerb) {
  const transformed = {};
  for (const [key, values] of Object.entries(forms)) {
    for (const value of values) {
      for (const reflexive of reflexivizeValue(key, value, baseVerb)) add(transformed, key, reflexive);
    }
  }
  return transformed;
}

'''
s = s.replace(anchor, helpers + anchor, 1)
old_loop = """for (const item of candidates) {
  const verb = item.infinitive;
  const latin = resultTables(verb, 'canarias');
  const formal = resultTables(verb, 'formal');
  const castilian = resultTables(verb, 'castellano');
  const voseo = resultTables(verb, 'voseo');"""
new_loop = """for (const item of candidates) {
  const verb = item.infinitive;
  const isReflexive = verb.endsWith('se');
  const base = isReflexive ? verb.slice(0, -2) : verb;
  const latin = resultTables(base, 'canarias');
  const formal = resultTables(base, 'formal');
  const castilian = resultTables(base, 'castellano');
  const voseo = resultTables(base, 'voseo');"""
if old_loop not in s:
    raise SystemExit("JS main-loop target missing")
s = s.replace(old_loop, new_loop, 1)
s = s.replace("  const forms = {};\n", "  let forms = {};\n", 1)
needle = "  addImperative(forms, castilian, 'Negativo', 'IMPERATIVE_NEGATIVE', 'VOSOTROS', 4);\n\n  const info = latin[0].info || {};"
replacement = "  addImperative(forms, castilian, 'Negativo', 'IMPERATIVE_NEGATIVE', 'VOSOTROS', 4);\n\n  if (isReflexive) forms = reflexivizeForms(forms, base);\n\n  const info = latin[0].info || {};"
if needle not in s:
    raise SystemExit("JS reflexive insertion target missing")
s = s.replace(needle, replacement, 1)
old_base = "  const base = verb.endsWith('se') ? verb.slice(0, -2) : verb;\n"
if old_base not in s:
    raise SystemExit("JS duplicate base target missing")
s = s.replace(old_base, "", 1)
js.write_text(s, encoding="utf-8")
print("generator fixes applied")
