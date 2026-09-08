# Android Build Protocol — Canonical Reference

For all stand-alone Android APK work in this repository, use the canonical protocol:

- `docs/Android Stand-Alone App Build Protocol.md`
- `scripts/android-verify-apk.sh`
- `scripts/android-emulator-smoke-test.sh`
- `docs/android-build-workflow.example.yml`

Do not invent a replacement build process, binary-patch an existing APK into a new application, or hand off an APK after compilation alone.

The required acceptance sequence is:

**source review → clean Gradle build → APK signature/package verification → fresh install → cold launch → process survival/logcat check → force-stop → second launch → exact passing artifact handoff**

The frozen baseline toolchain is documented in the canonical protocol. Change it only deliberately and only after validating the replacement baseline through the same acceptance gates.
