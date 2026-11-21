# IsslamPro Android

Android wrapper for the IsslamPro website:

`https://gpldroid.github.io/isslampro/`

The code under this directory is a **native Android WebView host**, not a Trusted Web Activity (TWA). It currently provides the Android application shell and website integration; native media-session controls and richer offline behavior remain future work.

## Build configuration

- Application ID: `com.gpldroid.isslampro`
- Version: `1.0.3 (versionCode 4)`
- Minimum Android: API 21 (Android 5.0)
- Compile SDK / Target SDK: 36
- Java: 17
- Gradle: 8.13
- Android Gradle Plugin: 8.13.2

## Build and release

See [PLAY_STORE_RELEASE.md](PLAY_STORE_RELEASE.md) for keystore setup, GitHub Actions secrets, release build steps, and the Google Play readiness checklist.

The workflow first runs an unsigned validation build on pushes. A signed APK and AAB are produced only when manually dispatching the workflow with `signed_release=true` and after all four signing secrets are configured.

The workflow does not commit APKs, AABs, keystores, or passwords into the repository. Signed packages are delivered as GitHub Actions artifacts.


## Native Android integrations (under active validation)

The Android host now includes an origin-restricted WebView message bridge (only the main frame at `https://gpldroid.github.io` is accepted), a media-session notification bridge, and an AlarmManager-based prayer scheduler. Prayer schedules are stored locally and restored after reboot or app update. Exact alarms are used only when Android grants exact-alarm access; otherwise the scheduler falls back to inexact idle-allowed alarms.

Quran audio downloads continue to use the website's Cache Storage offline library. This is persistent WebView-origin storage rather than a separately managed native file library, and the browser may still evict it under storage pressure. Validate long downloads, offline playback, and cache retention on target devices.

The adhan service streams the selected adhan URL when its scheduled alarm fires. It currently requires network access for that audio; pre-downloading adhan audio for fully offline prayer playback is not yet implemented.

These integrations are implementation-stage code, not a production certification. Confirm the Android build, test notification permissions and exact-alarm access on Android 13–16, and test lock-screen media controls, device reboot, time-zone changes, and adhan playback on physical devices before Play submission.
