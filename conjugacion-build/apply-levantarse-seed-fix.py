#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
path = root / "tools" / "generate_library.py"
source = path.read_text(encoding="utf-8")
marker = "\n}\n\nCURATED_EXAMPLES = {"
insert = "\n    \"levantarse\": [\"to get up\", \"to rise\"],\n}\n\nCURATED_EXAMPLES = {"
if marker not in source:
    raise SystemExit("Manual meanings insertion point not found")
source = source.replace(marker, insert, 1)
path.write_text(source, encoding="utf-8")
print("levantarse seed meaning added")
