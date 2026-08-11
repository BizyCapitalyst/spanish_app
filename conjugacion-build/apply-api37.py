#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
build = root / "app" / "build.gradle.kts"
text = build.read_text(encoding="utf-8")
text, compile_count = re.subn(r"compileSdk\s*=\s*36\b", "compileSdk = 37", text, count=1)
text, target_count = re.subn(r"targetSdk\s*=\s*36\b", "targetSdk = 37", text, count=1)
if compile_count != 1 or target_count != 1:
    raise SystemExit(f"Unable to patch API level: compile={compile_count}, target={target_count}")
build.write_text(text, encoding="utf-8")

props = root / "gradle.properties"
props_text = props.read_text(encoding="utf-8") if props.exists() else ""
if "android.suppressUnsupportedCompileSdk=37" not in props_text:
    props_text = props_text.rstrip() + "\nandroid.suppressUnsupportedCompileSdk=37\n"
props.write_text(props_text, encoding="utf-8")

for path in (root / "docs").glob("*.md"):
    s = path.read_text(encoding="utf-8")
    s = s.replace("target API 36", "target API 37")
    s = s.replace("Target API: 36", "Target API: 37")
    s = s.replace("target SDK 36", "target SDK 37")
    s = s.replace("targetSdk 36", "targetSdk 37")
    s = s.replace("targetSdkVersion:'36'", "targetSdkVersion:'37'")
    s = s.replace("Platform 36", "Platform 37")
    path.write_text(s, encoding="utf-8")

print("compileSdk and targetSdk updated to 37")
