# F-Droid Build Testing Guide

This document provides instructions for testing the F-Droid build variant to ensure it works correctly without proprietary dependencies.

## Prerequisites

- Android SDK installed
- Java 17 or later
- Android device or emulator

## Building F-Droid Variant

### Debug Build
```bash
# Clean previous builds
./gradlew clean

# Build F-Droid debug variant
./gradlew assembleFdroidDebug

# APK location
ls -la app/build/outputs/apk/fdroid/debug/
```

### Release Build
```bash
# Build F-Droid release variant (unsigned)
./gradlew assembleFdroidRelease

# APK location
ls -la app/build/outputs/apk/fdroid/release/
```

## Testing Checklist

### ✅ Basic Functionality
- [ ] App launches without crashes
- [ ] Main screen displays battery and storage information
- [ ] Settings screen accessible
- [ ] Alert configuration screens work

### ✅ F-Droid Specific Features
- [ ] App shows "Remote Notify (F-Droid)" in app name
- [ ] Application ID is `dev.hossain.remotenotify.fdroid`
- [ ] No Google Play Services references
- [ ] No Firebase functionality
- [ ] Email configuration shows appropriate F-Droid message

### ✅ Core Monitoring Features
- [ ] Battery threshold configuration works
- [ ] Storage threshold configuration works
- [ ] Alert history displays correctly
- [ ] Background monitoring can be enabled/disabled

### ✅ Notification Channels (Working)
- [ ] Telegram configuration and testing
- [ ] REST webhook configuration and testing
- [ ] Slack webhook configuration and testing

### ❌ Disabled Features (Expected in F-Droid)
- [ ] Email notifications show as unavailable
- [ ] Google Play In-App Review does not trigger
- [ ] No Firebase analytics calls
- [ ] No crashlytics reporting

## APK Analysis

### Check Dependencies
```bash
# Extract APK and check for Google dependencies
unzip -l app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep -i google

# Should return empty or minimal results
```

### Verify App Information
```bash
# Check package info
aapt dump badging app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep package

# Expected output:
# package: name='dev.hossain.remotenotify.fdroid' versionCode='19' versionName='1.15-fdroid'
```

### Check Permissions
```bash
# List permissions
aapt dump permissions app/build/outputs/apk/fdroid/release/app-fdroid-release.apk

# Should not include Google Play Services permissions
```

## Manual Testing Scenarios

### Scenario 1: Fresh Install
1. Install F-Droid APK on clean device/emulator
2. Open app and verify it starts without errors
3. Check that app name shows "Remote Notify (F-Droid)"
4. Navigate through all main screens

### Scenario 2: Alert Configuration
1. Go to Alert Configuration
2. Try to configure email notifications
3. Verify appropriate "not available in F-Droid" message shows
4. Configure Telegram notifications successfully
5. Configure webhook notifications successfully

### Scenario 3: Background Monitoring
1. Enable background monitoring
2. Set low battery threshold (e.g., 90% for testing)
3. Configure Telegram bot for notifications
4. Verify WorkManager schedules periodic work
5. Check that alerts are generated appropriately

### Scenario 4: Settings and About
1. Open Settings screen
2. Verify all settings work correctly
3. Open About screen
4. Check version information shows F-Droid variant
5. Verify no Google Play review prompts appear

## Common Issues and Solutions

### Build Issues

**Issue**: `google-services.json` not found
**Solution**: F-Droid builds don't need this file. Ensure you're building the fdroid variant.

**Issue**: Firebase/Google Play dependencies missing
**Solution**: These are intentionally excluded in fdroid flavor. This is expected behavior.

### Runtime Issues

**Issue**: Email configuration crashes
**Solution**: Check that BuildConfig.SUPPORTS_EMAIL_NOTIFICATIONS is false in fdroid builds.

**Issue**: App tries to use Google services
**Solution**: Verify flavor-specific source files are being used correctly.

## Validation Commands

```bash
# Verify no Google Play Services in APK
zipinfo -1 app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep -i "google\|firebase\|gms" || echo "✅ No Google services found"

# Check APK size (F-Droid should be smaller than Google Play version)
ls -lah app/build/outputs/apk/fdroid/release/app-fdroid-release.apk

# Verify correct package name
aapt dump badging app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep "package.*fdroid"
```

## F-Droid Compliance Check

- ✅ No proprietary dependencies
- ✅ No external API requirements for core functionality  
- ✅ Open source license (MIT)
- ✅ No tracking or analytics
- ✅ Reproducible builds with Gradle
- ✅ No hardcoded secrets or keys

## Reporting Issues

If you find any issues with the F-Droid build:

1. Check this testing guide first
2. Verify you're testing the correct flavor (fdroid, not gplay)
3. Report issues to: https://github.com/hossain-khan/android-remote-notify/issues
4. Include:
   - Device/emulator information
   - Build variant (fdroid debug/release)
   - Steps to reproduce
   - Expected vs actual behavior