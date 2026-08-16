#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
path = root / "app" / "build.gradle.kts"
source = path.read_text(encoding="utf-8")
old = 'implementation("androidx.core:core-ktx:1.19.0")'
new = 'implementation("androidx.core:core-ktx:1.17.0")'
if old not in source:
    raise SystemExit("Expected AndroidX Core dependency not found")
source = source.replace(old, new, 1)
path.write_text(source, encoding="utf-8")
print("AndroidX Core pinned to 1.17.0 for compileSdk 36 / AGP 8.13 compatibility")
