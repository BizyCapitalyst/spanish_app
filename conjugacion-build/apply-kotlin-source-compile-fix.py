#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
src = root / "app" / "src" / "main" / "java" / "com" / "james" / "conjugaciondiaria"

# Newer Compose exposes weight only as a RowScope/ColumnScope member and the
# archived source's explicit import resolves to an internal implementation.
# Use an adaptive width fraction instead so the portrait layout remains usable
# without relying on that removed source-level API shape.
layout_files = [
    src / "HomeBuilderScreens.kt",
    src / "PracticeScreen.kt",
    src / "ManagementScreens.kt",
]
for path in layout_files:
    text = path.read_text(encoding="utf-8")
    text = text.replace("import androidx.compose.foundation.layout.weight\n", "")
    text, count = re.subn(r"\.weight\([^)]*\)", ".fillMaxWidth(0.48f)", text)
    if "import androidx.compose.foundation.layout.fillMaxWidth\n" not in text:
        anchor = "import androidx.compose.foundation.layout."
        index = text.find(anchor)
        if index < 0:
            raise SystemExit(f"No Compose layout import anchor in {path.name}")
        end = text.find("\n", index) + 1
        text = text[:end] + "import androidx.compose.foundation.layout.fillMaxWidth\n" + text[end:]
    path.write_text(text, encoding="utf-8")
    print(f"{path.name}: replaced {count} weight calls")

repository = src / "Repository.kt"
text = repository.read_text(encoding="utf-8")
old_familiar = """            val familiar = eligibleVerbs.shuffled().asSequence().flatMap { cardsForVerb(it, config, stateByKey, memberships, categoryNames).asSequence() }
            for (card in familiar) {
"""
new_familiar = """            val familiar = mutableListOf<PracticeCard>()
            for (verb in eligibleVerbs.shuffled()) {
                familiar += cardsForVerb(verb, config, stateByKey, memberships, categoryNames)
            }
            for (card in familiar) {
"""
if old_familiar not in text:
    raise SystemExit("Familiar-card suspend sequence block not found")
text = text.replace(old_familiar, new_familiar, 1)

old_custom = """        val cards = eligibleVerbs.asSequence()
            .flatMap { cardsForVerb(it, config, stateByKey, memberships, categoryNames).asSequence() }
            .filter { card ->
"""
new_custom = """        val generatedCards = mutableListOf<PracticeCard>()
        for (verb in eligibleVerbs) {
            generatedCards += cardsForVerb(verb, config, stateByKey, memberships, categoryNames)
        }
        val cards = generatedCards.asSequence()
            .filter { card ->
"""
if old_custom not in text:
    raise SystemExit("Custom-session suspend sequence block not found")
text = text.replace(old_custom, new_custom, 1)
repository.write_text(text, encoding="utf-8")

remaining_weight = []
for path in layout_files:
    if re.search(r"\.weight\(", path.read_text(encoding="utf-8")):
        remaining_weight.append(path.name)
if remaining_weight:
    raise SystemExit(f"Unpatched weight calls remain: {remaining_weight}")
if "flatMap { cardsForVerb" in repository.read_text(encoding="utf-8"):
    raise SystemExit("Suspend cardsForVerb call remains inside flatMap")
print("Kotlin application-source compilation fixes applied")
