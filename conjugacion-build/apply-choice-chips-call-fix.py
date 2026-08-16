#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
path = root / "app" / "src" / "main" / "java" / "com" / "james" / "conjugaciondiaria" / "ManagementScreens.kt"
text = path.read_text(encoding="utf-8")

replacements = [
    (
        'ChoiceChips(VerbFlag.entries, flags, { it.label }) { flag -> flags = flags.toMutableSet().apply { if (!add(flag)) remove(flag) } }',
        'ChoiceChips(VerbFlag.entries, flags, { it.label }, onToggle = { flag -> flags = flags.toMutableSet().apply { if (!add(flag)) remove(flag) } })',
    ),
    (
        '''ChoiceChips(categories, categories.filter { it.id in categoryIds }.toSet(), { it.name }) { category ->
                        categoryIds = categoryIds.toMutableSet().apply { if (!add(category.id)) remove(category.id) }
                    }''',
        '''ChoiceChips(categories, categories.filter { it.id in categoryIds }.toSet(), { it.name }, onToggle = { category ->
                        categoryIds = categoryIds.toMutableSet().apply { if (!add(category.id)) remove(category.id) }
                    })''',
    ),
    (
        'ChoiceChips(categories, categories.filter { it.id in selected }.toSet(), { it.name }) { category -> selected = selected.toMutableSet().apply { if (!add(category.id)) remove(category.id) } }',
        'ChoiceChips(categories, categories.filter { it.id in selected }.toSet(), { it.name }, onToggle = { category -> selected = selected.toMutableSet().apply { if (!add(category.id)) remove(category.id) } })',
    ),
    (
        'ChoiceChips(ProgressSection.entries, setOf(section), { it.label }) { section = it }',
        'ChoiceChips(ProgressSection.entries, setOf(section), { it.label }, onToggle = { selectedSection -> section = selectedSection })',
    ),
    (
        'ChoiceChips(PromptStyle.entries, setOf(prefs.promptStyle), { it.label }) { style -> update { it.copy(promptStyle = style) } }',
        'ChoiceChips(PromptStyle.entries, setOf(prefs.promptStyle), { it.label }, onToggle = { style -> update { value -> value.copy(promptStyle = style) } })',
    ),
    (
        '''ChoiceChips(Tense.entries.filter { it.mood == mood }, prefs.enabledTenses, { it.bilingual }) { tense ->
                    update { value -> value.copy(enabledTenses = value.enabledTenses.toMutableSet().apply { if (!add(tense)) remove(tense) }) }
                }''',
        '''ChoiceChips(Tense.entries.filter { it.mood == mood }, prefs.enabledTenses, { it.bilingual }, onToggle = { tense ->
                    update { value -> value.copy(enabledTenses = value.enabledTenses.toMutableSet().apply { if (!add(tense)) remove(tense) }) }
                })''',
    ),
    (
        'ChoiceChips(available, prefs.enabledPersons, { it.display }) { person -> update { value -> value.copy(enabledPersons = value.enabledPersons.toMutableSet().apply { if (!add(person)) remove(person) }) } }',
        'ChoiceChips(available, prefs.enabledPersons, { it.display }, onToggle = { person -> update { value -> value.copy(enabledPersons = value.enabledPersons.toMutableSet().apply { if (!add(person)) remove(person) }) } })',
    ),
]

for index, (old, new) in enumerate(replacements, start=1):
    if old not in text:
        raise SystemExit(f"ChoiceChips replacement {index} source pattern not found")
    text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")

for old, _ in replacements:
    if old in text:
        raise SystemExit("An obsolete ChoiceChips call remains")
if text.count("onToggle =") < len(replacements):
    raise SystemExit("Named onToggle arguments were not fully applied")
print(f"Fixed {len(replacements)} ChoiceChips calls")
