# Android TWA build

This directory wraps the existing Islam Pro PWA in a Trusted Web Activity. The website itself remains in the repository root and is not copied or rewritten.

- Application ID: `com.gpldroid.isslampro`
- Website: `https://gpldroid.github.io/isslampro/`
- Android target API: 36 (required for new Google Play submissions from August 31, 2026)
- Outputs: `download/app-release.apk` and `download/app-release.aab`

## Required GitHub Actions secrets

Add these repository secrets under Settings → Secrets and variables → Actions:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded upload keystore (single-line Base64).
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: key alias.
- `ANDROID_KEY_PASSWORD`: key password.

Never commit the keystore or passwords.

## Important: Digital Asset Links

A fully verified TWA requires the signing certificate fingerprint in:

`https://gpldroid.github.io/.well-known/assetlinks.json`

The current website is a GitHub Pages project site at `/isslampro/`. A `.well-known/assetlinks.json` file placed inside this repository would be served below `/isslampro/`, which is not the required origin-root URL. The workflow therefore writes a copy to `download/assetlinks.json` after signing, so it can be published at the correct origin root if that host is configured.

Until the origin-root Digital Asset Links file is published, Android may open the website in a Custom Tab fallback rather than a verified fullscreen TWA.

## Build

The workflow uses Java 17, Gradle 8.13, Android Gradle Plugin 8.13.2, and Android Browser Helper 2.7.4. Run it from GitHub Actions after adding the four secrets.
