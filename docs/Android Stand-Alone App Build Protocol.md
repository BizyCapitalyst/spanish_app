# Android Stand-Alone App Build Protocol

Version: 1.0
Status: CANONICAL
Scope: Small stand-alone Android applications built for direct APK installation, including language-learning, vocabulary, conjugation, drill, utility, and similar offline-first apps.

## 1. Definition of a successful build

A build is not considered successful merely because Gradle produced an APK.

A distributable APK must pass all three gates:

1. BUILD VALIDITY — source compiles and packages normally with the Android toolchain.
2. INSTALLABILITY — the exact produced APK is correctly signed, has the intended package/launcher metadata, and installs through Android package manager.
3. RUNTIME SURVIVAL — the installed app launches, remains alive after startup, and produces no fatal startup exception.

Do not give an APK to the user until all required gates pass on the exact artifact being delivered.

## 2. Non-negotiable architecture rules

### 2.1 Build from source

For a new application, build a new Android project from source. Do not create the app by modifying, renaming, binary-patching, repackaging, DEX-rewriting, or transplanting another app's APK.

Existing APKs may be inspected diagnostically, but they are not a safe source base for a different application.

### 2.2 Isolate every app

Each stand-alone app must have its own:

- Android project directory;
- `applicationId`;
- source namespace/package;
- launcher label and icon;
- version code/version name;
- persistence namespace/files;
- build artifact.

Do not share a Room database, seed database, compiled runtime, application class, or startup initialization path merely because two apps are related.

### 2.3 Keep simple apps simple

For small stand-alone learning/utilities apps, prefer Android platform APIs and the minimum number of dependencies required.

Default persistence for small scalar progress/settings data: `SharedPreferences` or an equally simple local mechanism.

Do not introduce Room/database initialization unless the application genuinely requires structured local data. If a database is required, schema versioning and migrations must be explicit and tested.

### 2.4 Startup must be dependency-tolerant

The launcher Activity must render even when optional device services are unavailable.

Examples of optional services that must fail gracefully:

- Text-to-Speech;
- speech recognition;
- microphone permission;
- network access;
- optional files or user data.

A missing optional service may disable a feature or show a status message. It must not terminate the app.

### 2.5 No startup cache/database repair loop

A newly installed app must not require the user to clear cache/data to open it.

If persistent state is incompatible after an app upgrade, handle it explicitly through compatible migrations, safe state reset, or a new application ID where appropriate. Never rely on a recurring "clear cache" message as application recovery logic.

## 3. Frozen known-good Android build baseline

Use this baseline unless a specific application requires otherwise. Change versions deliberately, not opportunistically during troubleshooting.

- GitHub runner: Ubuntu hosted runner
- Java: Temurin JDK 17
- Gradle: 8.9
- Android Gradle Plugin: 8.7.3
- compileSdk: 35
- targetSdk: 35
- build-tools: 35.0.0
- minimum SDK for the current lightweight app pattern: 26

Known-good Gradle root plugin declaration:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
}
```

Known-good app baseline:

```kotlin
plugins {
    id("com.android.application")
}

android {
    namespace = "<app namespace>"
    compileSdk = 35

    defaultConfig {
        applicationId = "<unique application id>"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

For subsequent releases of the same application, increment `versionCode`. Keep the same `applicationId` and signing identity when the new APK is intended to update the installed app.

## 4. Manifest requirements

Every installable stand-alone app must have one unambiguous launcher Activity.

Required launcher pattern:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Only declare permissions the app actually uses. Runtime permissions such as microphone access should be requested when the user invokes the feature, not used as a prerequisite for constructing the first screen.

The app must remain usable, at least in a reduced mode, if permission is denied.

## 5. Source-level pre-build checklist

Before spending compute on CI or an emulator, confirm:

- the application ID is final and unique;
- source package declarations match the intended namespace;
- launcher Activity exists at the manifest path;
- no class or resource references point to another application's namespace;
- no copied Application subclass initializes another application's database/services;
- no old Room database or seed asset is present unless intentionally required;
- no startup code calls `finish()`, throws, or terminates the process on recoverable initialization failure;
- optional Android services are wrapped in availability/error handling;
- microphone or other dangerous permissions are requested at feature use;
- persistence reads have safe defaults;
- first launch works with an empty data directory;
- every UI object used during initialization has been created before it is referenced.

Do this source review before CI. Do not use repeated builds to discover errors that are visible directly in source.

## 6. Build Gate — compile once from clean source

Use a clean source checkout and the frozen toolchain.

Required command:

```bash
gradle :app:assembleDebug --no-daemon
```

For the final user-distributed production version, use the appropriate release task and persistent release signing configuration. Debug signing is acceptable for direct development/sideload testing, but it must not be confused with a production signing strategy.

A Build Gate passes only if Gradle exits successfully and the expected APK exists.

Do not patch or modify the APK after it has been built and signed. Renaming the APK file itself is permitted; modifying APK contents is not.

## 7. Package Verification Gate

Run Android's own tools against the exact APK produced by the build.

Required checks:

```bash
apksigner verify --verbose <apk>
aapt2 dump badging <apk>
```

Verify from `aapt2` output:

- package name/application ID is correct;
- version code and version name are correct;
- minimum and target SDK are correct;
- application label is correct;
- launcher Activity is correct;
- expected permissions are present;
- no unexpected permission or hardware requirement makes installation unnecessarily restrictive.

For the current minSdk 26 pattern, APK Signature Scheme v2 verification is sufficient for development installation. A production release should use the intended stable release signing configuration.

## 8. Installation Gate

### 8.1 Fresh-install test

A new app must pass a fresh install into an empty application state:

```bash
adb uninstall <applicationId> || true
adb install <apk>
```

`adb install` must return `Success`.

### 8.2 Upgrade test

If the APK is intended to update an already installed version, also test:

```bash
adb install -r <apk>
```

An upgrade requires the same application ID and compatible signing identity. If either changes, Android will treat it as a different app or reject the update.

Do not tell the user to clear cache as a standard installation step.

## 9. Runtime Survival Gate

After installation, launch the exact declared Activity and test actual process survival.

Required sequence:

```bash
adb logcat -c
adb shell am start -W -n <applicationId>/.MainActivity
sleep 7
adb shell pidof <applicationId>
adb logcat -d -v threadtime
```

Pass conditions:

- `am start -W` reports `Status: ok`;
- the application process still exists after the wait period;
- logcat contains no fatal exception for the application;
- the launcher Activity remains the active app rather than immediately finishing.

A successful `am start` by itself is NOT a pass. Android can report a successful Activity launch and the process can still die moments later.

Use the repository script `scripts/android-emulator-smoke-test.sh` rather than re-creating this shell logic inside each workflow.

## 10. Relaunch and persistence checks

Before handoff, perform at least one second-launch check:

1. launch app;
2. force-stop app;
3. launch app again;
4. confirm the process remains alive again.

For apps that persist progress/settings:

1. change a small piece of state;
2. force-stop;
3. relaunch;
4. verify startup still succeeds with persisted state.

This catches the exact class of defect where first launch works but later launches fail because of incompatible persisted state.

## 11. Optional-service resilience tests

For apps using speech/TTS or similar platform services, test the startup path independently of those services.

The app must survive when:

- speech recognition is unavailable;
- TTS initialization fails or has no desired locale;
- microphone permission has not yet been granted;
- microphone permission is denied.

The UI should fall back to typed/manual interaction or disable only the unavailable feature.

## 12. Artifact handoff rules

The APK delivered to the user must be the exact artifact that passed package verification and runtime testing.

Before handoff record:

- application ID;
- app version;
- build commit SHA;
- workflow/run ID;
- artifact filename;
- SHA-256 hash when practical;
- which gates passed.

Do not rebuild, repackage, resign, or patch after the passing smoke test and then hand over a different binary.

## 13. Compute-efficiency protocol

Use this order every time:

### Phase A — Source review, no CI

Resolve package, manifest, namespace, startup, persistence, and optional-service problems by reading source first.

### Phase B — One Build Gate

Compile once. If compilation fails, read the compiler error and make the smallest source correction. Do not run an emulator while the build is failing.

### Phase C — One Package Verification Gate

Run `apksigner` and `aapt2` on the successful build. Fix metadata/signing issues before emulator use.

### Phase D — Runtime Gate

Only after A-C pass, boot an emulator and run the reusable smoke-test script. Emulator startup is expensive; do not use it as a source-code linter.

### Phase E — Diagnose from captured evidence

If runtime fails, always capture logcat in the same run before exiting. Never launch another emulator merely to obtain the error that the previous run should have preserved.

### Phase F — Retest only the changed layer

- Java/Kotlin compile error: rerun Build Gate first.
- manifest/package error: rerun Build + Package Verification.
- startup/runtime change: rerun all gates, including emulator.
- documentation-only change: no APK rebuild.

### Phase G — Freeze a passing baseline

Once a source/toolchain/workflow combination passes all gates, treat it as the baseline. New related apps should copy the source project structure and workflow pattern, not reverse-engineer an APK and not redesign the build process.

## 14. Prohibited failure patterns

Do not use these as normal build techniques:

- DEX string/package rewriting;
- APK binary patching to create a different app;
- copying another app's Room database and hoping cache reset will reconcile it;
- changing package identity after compilation;
- modifying APK contents after signing;
- relying only on `assembleDebug` success;
- relying only on `apksigner` success;
- relying only on `am start Status: ok`;
- handing over an APK that did not pass the runtime gate;
- repeatedly rebuilding without reading the previous compiler/logcat failure;
- running a costly emulator before static/build verification passes;
- using shell variables across workflow/action boundaries where variable scope is not guaranteed.

## 15. Standard acceptance record

Use this block in the final build record:

```text
ANDROID APK ACCEPTANCE
App: <name>
Application ID: <id>
Version: <version>
Commit: <sha>
Artifact: <filename>

[PASS] Source isolation review
[PASS] Clean Gradle build
[PASS] APK signature verification
[PASS] Package/launcher metadata verification
[PASS] Fresh install
[PASS] Cold launch
[PASS] Process alive after startup delay
[PASS] No application fatal exception in logcat
[PASS] Force-stop and second launch
[PASS] Persisted-state relaunch, if applicable
[PASS] Optional-service degradation, if applicable

Approved for user handoff: YES
```

If any required line is not a PASS, the APK is not approved for handoff.
