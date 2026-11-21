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
