#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1])
path = root / "app" / "build.gradle.kts"
source = path.read_text(encoding="utf-8")

old = '''    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xannotation-default-target=param-property")
    }
'''
if old not in source:
    raise SystemExit("Legacy kotlinOptions block not found")
source = source.replace(old, "", 1)

marker = "\nksp {"
replacement = '''

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {'''
if marker not in source:
    raise SystemExit("KSP insertion marker not found")
source = source.replace(marker, replacement, 1)

# Keep the installable test build on the requested permanent application ID.
source = source.replace('            applicationIdSuffix = ".debug"\n', "")
source = source.replace('            versionNameSuffix = "-debug"\n', "")

path.write_text(source, encoding="utf-8")
print("Kotlin compiler DSL migrated; debug package suffix removed")
