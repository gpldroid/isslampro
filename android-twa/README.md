# IsslamPro Android

Android application shell for the IsslamPro Quran and prayer app:

`https://gpldroid.github.io/isslampro/`

This project uses a native Android WebView host (not a Trusted Web Activity). The goal of this branch is to improve Quran audio playback, offline listening, prayer scheduling, and adhan playback. No app-store submission is part of this work.

## Build configuration

- Application ID: `com.gpldroid.isslampro`
- Version: `1.0.4 (versionCode 5)`
- Minimum Android: API 21 (Android 5.0)
- Compile SDK / Target SDK: 36
- Java: 17
- Gradle: 8.13
- Android Gradle Plugin: 8.13.2

## Current implementation

- The WebView bridge is restricted to the trusted `https://gpldroid.github.io` origin and main-frame messages.
- Quran audio uses the website player and a native Android media-session notification. Notification controls send play/pause/next/previous commands to the page.
- Saved Quran audio is stored in the WebView Cache Storage library. The service worker now serves cached byte-range requests as streams, avoiding creation of a full in-memory Blob for long recordings.
- Cache Storage persistence is requested when supported. Actual quota, eviction behavior, and offline playback still need testing on physical devices and with the audio providers used by the app.
- Prayer alarms are stored locally and replaced cleanly when a new schedule arrives. Alarms are restored after reboot, app replacement, device date/time changes, and exact-alarm permission changes.
- Exact alarms are used only when Android grants exact-alarm access; otherwise the app falls back to inexact idle-allowed alarms.
- The adhan service starts as a media-playback foreground service and streams the selected adhan URL when its alarm fires. Offline adhan pre-download is not implemented; internet is required for a streamed adhan.

## Validation status and known limits

The GitHub Actions workflow builds an unsigned debug APK/AAB for validation and checks JavaScript syntax and Android manifest XML. A successful CI build proves compilation, not real-device behavior.

The media notification still controls the WebView's HTML audio player rather than owning a separate native ExoPlayer pipeline. Confirm that WebView audio continues reliably when the screen is locked, the app is backgrounded, or Android reclaims the Activity. If the Activity/WebView is destroyed, notification controls may no longer reach the page.

Before calling this release-ready, test on physical Android devices:
- Android 13–16 notification permission and notification-channel behavior
- Quran audio while screen-locked, including play/pause/next/previous
- Long recitations, seeking, interrupted networks, and offline playback after force-closing/reopening the app
- Cache quota and retention after device storage pressure
- Prayer times, test adhan, exact-alarm permission denied/granted, reboot, time/date changes, and app update
- Adhan playback with screen off and with network unavailable

Do not merge PR #8 into `main` until the latest CI run succeeds and the device test checklist is completed.
