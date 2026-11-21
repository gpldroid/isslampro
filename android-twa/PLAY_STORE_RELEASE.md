# IsslamPro — Google Play release checklist

## 1. Protect and configure the existing signing key

You said you already have a signing key. Keep the original file and at least two encrypted backups. Do not commit the keystore or share its passwords in chat, issues, pull requests, or repository files.

Convert the keystore file to a single-line Base64 string on your own computer:

- Linux: `base64 -w 0 upload-key.jks`
- macOS: `base64 -i upload-key.jks | tr -d '\n'`
- PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-key.jks"))`

In GitHub, open **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The one-line Base64 output |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Alias of the signing key |
| `ANDROID_KEY_PASSWORD` | Password for that key |

These must be Actions **secrets**, not ordinary variables. The workflow decodes the key only on the temporary GitHub runner.

If this is the first Google Play listing for this package, enroll in **Play App Signing** and use this saved key as the upload key. Preserve it and do not rotate it casually after the first upload.

## 2. Run the builds

1. Open **Actions → Android validation and release**.
2. Run the workflow on branch `play-store-prep`.
3. For an unsigned compile check, leave `signed_release` off.
4. After all four secrets are configured, run it again with `signed_release` on.
5. Download the artifact named `islam-pro-android-release`.
6. Keep `app-release.aab`, `app-release.apk`, `upload-key-sha256.txt`, and `SHA256SUMS.txt together in secure release records.

An unsigned validation artifact is not suitable for Play upload. Only the signed AAB is intended for Play Console.

## 3. Create the Google Play developer account

Use the Google account that should legally own the app listing. Choose Personal if you publish as an individual; choose Organization only if a registered organization is the publisher and can provide the required verification details.

Official registration: https://play.google.com/console/signup

Google currently documents a one-time US$25 registration fee for a full-distribution account. Identity and contact verification are part of signup. Do not send identity documents or payment details through this chat.

For new personal developer accounts, Google requires a closed test with at least 12 opted-in testers continuously for 14 days before applying for production access. Check the current Play Console dashboard for the requirements attached to your account.

## 4. Before production submission

- [ ] Complete developer account and identity verification.
- [ ] Confirm the package ID `com.gpldroid.isslampro` and saved upload key.
- [ ] Run the unsigned validation workflow successfully.
- [ ] Add the four GitHub Actions secrets and build the signed AAB.
- [ ] Test install, back navigation, external links, location permission, audio playback, rotation, network loss, and offline cache on physical Android devices.
- [ ] Verify that the publisher owns or is authorized to distribute all website content and audio.
- [ ] Publish a privacy policy that accurately describes actual app and website data practices.
- [ ] Complete Play Console Data safety, ads, app access, content rating, target audience, and permission declarations truthfully.
- [ ] Prepare store icon, feature graphic, screenshots, short description, and full description.
- [ ] Complete the testing track required by the account.
- [ ] Submit to production only after the above checks pass.

## 5. Product quality gate

This project is currently a WebView-based website host, not a native Quran/audio application and not a TWA. A WebView wrapper alone may not meet Google Play's expectations for minimum functionality or user experience. Before production, test the actual app and consider adding meaningful native capabilities such as Android media-session/lock-screen controls for Quran audio. Do not claim offline audio or background playback until they have been implemented and tested.

A successful GitHub build does not guarantee Play approval.
