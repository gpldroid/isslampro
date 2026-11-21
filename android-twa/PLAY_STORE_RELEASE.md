# IsslamPro Android — Google Play release preparation

This directory currently contains a **native Android WebView host** for the IsslamPro website. It is not a Trusted Web Activity (TWA): the app does not use Android Browser Helper, and Digital Asset Links do not verify this WebView wrapper. The app opens:

`https://gpldroid.github.io/isslampro/`

## Android release configuration

- Application ID: `com.gpldroid.isslampro`
- Version: `1.0.3 (versionCode 4)`
- Minimum Android: API 21 (Android 5.0)
- Compile SDK: 36
- Target SDK: 36
- Outputs: signed APK and Android App Bundle (AAB)

The release workflow is `.github/workflows/android-build.yml`. It builds both release formats and uploads them as a GitHub Actions artifact. It does not commit binary packages or signing material into the repository.

## Configure the existing upload keystore

The release workflow expects four **GitHub Actions repository secrets**. The keystore file and passwords must never be committed to Git or pasted into an issue, pull request, or chat.

1. Confirm that your saved keystore is the signing key you intend to keep using for this app. Keep at least two encrypted backups.
2. Convert the keystore file to one-line Base64 on your own computer:
   - Linux: `base64 -w 0 upload-key.jks`
   - macOS: `base64 -i upload-key.jks | tr -d '\n'`
   - PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-key.jks"))`
3. Open **Settings → Secrets and variables → Actions → New repository secret** in GitHub.
4. Add these exact secrets:
   - `ANDROID_KEYSTORE_BASE64`: the one-line Base64 output.
   - `ANDROID_KEYSTORE_PASSWORD`: keystore password.
   - `ANDROID_KEY_ALIAS`: alias of the signing key.
   - `ANDROID_KEY_PASSWORD`: key password.
5. Do not put the Base64 output in a normal repository variable; it is secret material.

The workflow reports the SHA-256 certificate fingerprint in its artifact. Store this fingerprint securely for your release records. When creating the Play app, enroll in Play App Signing and use this keystore as the upload key if this is a new Play listing. Do not rotate or replace it casually after the first upload.

## Run the release build

After configuring all four secrets:

1. Open **Actions → Android APK and AAB**.
2. Select **Run workflow** on branch `play-store-prep`.
3. Wait for all build steps to pass.
4. Download the `islam-pro-android-release` artifact.
5. Verify the AAB in Play Console's internal testing track before any production submission.

A successful GitHub build is not the same as Google Play approval. Test the APK on physical devices, inspect the AAB in Play Console, and complete the Play policy declarations before release.

## Google Play readiness checklist

- [ ] Create and verify the Google Play developer account.
- [ ] Confirm that the existing keystore is the intended upload key.
- [ ] Add the four GitHub Actions secrets.
- [ ] Run the signed release workflow and download the AAB.
- [ ] Test installation, navigation, audio, location permission, offline behavior, and external links on real devices.
- [ ] Prepare a public privacy policy that accurately describes actual app and website data practices.
- [ ] Complete Data safety, app access, ads, content rating, target audience, and permissions declarations accurately.
- [ ] Prepare store icon, phone screenshots, feature graphic, short description, and full description.
- [ ] Complete internal/closed testing requirements applicable to the developer account.
- [ ] Submit the production release only after test results and policy declarations are satisfactory.

## Important product limitation

This release is still a WebView-based app. A wrapper alone may not provide enough distinct utility or quality for a strong Play listing. Before production, validate that the website is owned/controlled by the publisher, all embedded content is licensed for distribution, and the app offers a reliable mobile experience beyond simply opening a website. Consider adding and testing meaningful native capabilities (for example, native media-session controls for audio playback) in a separate implementation phase.

## Signing and privacy

Never upload the keystore itself to GitHub. Never send its passwords to anyone. The workflow uses GitHub's encrypted Actions secrets and removes the decoded keystore when the runner is discarded. The APK and AAB are signed build outputs and are delivered as short-lived workflow artifacts.
