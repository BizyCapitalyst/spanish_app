#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
src = root / "app" / "src" / "main" / "java" / "com" / "james" / "conjugaciondiaria"
files = list(src.rglob("*.kt"))

old_decl = "fun setBuilderConfig(config: SessionConfig)"
new_decl = "fun updateBuilderConfig(config: SessionConfig)"
declaration_count = 0
call_count = 0

for path in files:
    text = path.read_text(encoding="utf-8")
    if old_decl in text:
        text = text.replace(old_decl, new_decl, 1)
        declaration_count += 1
    occurrences = text.count(".setBuilderConfig(")
    if occurrences:
        text = text.replace(".setBuilderConfig(", ".updateBuilderConfig(")
        call_count += occurrences
    path.write_text(text, encoding="utf-8")

if declaration_count != 1:
    raise SystemExit(f"Expected one setBuilderConfig declaration; found {declaration_count}")
remaining = []
for path in files:
    text = path.read_text(encoding="utf-8")
    if "setBuilderConfig(" in text:
        remaining.append(str(path.relative_to(root)))
if remaining:
    raise SystemExit(f"Old setBuilderConfig references remain: {remaining}")
print(f"Renamed builder configuration method and {call_count} call sites")
