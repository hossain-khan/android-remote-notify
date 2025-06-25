# How to Submit Android Remote Notify to F-Droid

This document provides step-by-step instructions for submitting the Android Remote Notify app to the F-Droid app store.

## Prerequisites

Before submitting to F-Droid, ensure you have:

1. **Repository Access**: Administrative access to the `hossain-khan/android-remote-notify` GitHub repository
2. **F-Droid Account**: An account on the [F-Droid Forum](https://forum.f-droid.org/) or GitLab access
3. **Build Environment**: Android SDK and tools to test F-Droid builds locally

## F-Droid Build Preparation

### 1. Project Structure

The project has been configured with two build flavors:

- **`fdroid`**: FOSS-compliant build without Google services
- **`gplay`**: Full-featured build with Google Play Services for other app stores

### 2. F-Droid Specific Changes

#### Build Flavors Configuration
- F-Droid flavor excludes:
  - Firebase Crashlytics and Analytics
  - Google Play In-App Review API
  - External email API requirements
  - Google Services dependencies

#### Source Code Changes
- Created flavor-specific implementations in `app/src/fdroid/` and `app/src/gplay/`
- F-Droid versions of classes:
  - `CrashlyticsTree.kt`: Uses Android Log instead of Firebase
  - `InAppReviewManager.kt`: No-op implementation for F-Droid
- BuildConfig flags to control feature availability

### 3. Testing F-Droid Build

To test the F-Droid build variant locally:

```bash
# Clean and build F-Droid debug variant
./gradlew clean
./gradlew assembleFdroidDebug

# Or build F-Droid release variant
./gradlew assembleFdroidRelease

# Install on device/emulator
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

Verify that:
- App launches without crashes
- No Google Play Services are referenced
- Email configuration shows appropriate messages about F-Droid limitations
- All core functionality (battery/storage monitoring, Telegram, webhooks) works

## F-Droid Submission Process

### Option 1: Request for Packaging (Recommended)

1. **Create Forum Post**
   - Visit [F-Droid Forum - Requests for Packaging](https://forum.f-droid.org/c/requests-for-packaging/12)
   - Create a new topic with title: `[RFP] Remote Notify - Android device monitoring with notifications`

2. **Provide Required Information**
   ```
   **App Name**: Remote Notify
   **Description**: Monitor battery and storage levels on remote Android devices with customizable notifications
   **Website**: https://github.com/hossain-khan/android-remote-notify
   **Source Code**: https://github.com/hossain-khan/android-remote-notify
   **Issue Tracker**: https://github.com/hossain-khan/android-remote-notify/issues
   **License**: MIT
   **Donation**: [PLACEHOLDER - Add if you want to include donation info]
   
   **Categories**: System, Utilities
   **Anti-Features**: None (F-Droid build is fully FOSS)
   ```

3. **Technical Details**
   ```
   **Build Instructions**:
   - Use the `fdroid` flavor: `./gradlew assembleFdroidRelease`
   - No special requirements or external dependencies
   - Builds with standard Android SDK
   
   **F-Droid Compatibility**:
   - ✅ No proprietary dependencies in fdroid flavor
   - ✅ No hardcoded secrets or API keys required
   - ✅ MIT License (compatible with F-Droid)
   - ✅ Builds reproducibly with Gradle
   ```

### Option 2: Direct Metadata Contribution

1. **Fork F-Droid Data Repository**
   ```bash
   git clone https://gitlab.com/fdroid/fdroiddata.git
   cd fdroiddata
   ```

2. **Create Metadata File**
   Create `metadata/dev.hossain.remotenotify.fdroid.yml`:
   
   ```yaml
   # [PLACEHOLDER - This section needs to be filled with actual F-Droid metadata]
   # The maintainer will need to create the complete metadata file following F-Droid guidelines
   
   Categories:
     - System
     - Utilities
   License: MIT
   AuthorName: Hossain Khan
   AuthorEmail: [PLACEHOLDER - Add your email]
   WebSite: https://github.com/hossain-khan/android-remote-notify
   SourceCode: https://github.com/hossain-khan/android-remote-notify
   IssueTracker: https://github.com/hossain-khan/android-remote-notify/issues
   
   Summary: Monitor remote Android device battery and storage levels
   Description: |
     Remote Notify monitors battery and storage levels on Android devices and sends
     notifications when thresholds are exceeded. Supports multiple notification
     channels including Telegram, REST webhooks, and Slack.
     
     Features:
     * Battery level monitoring with customizable thresholds
     * Storage space monitoring with configurable alerts
     * Multiple notification channels (Telegram, Webhooks, Slack)
     * Periodic background monitoring using WorkManager
     * Alert history and logging with filtering capabilities
     * Material3 UI with dark/light theme support
     
     The F-Droid version excludes Google services and email notifications
     that require external API keys.
   
   RepoType: git
   Repo: https://github.com/hossain-khan/android-remote-notify.git
   
   Builds:
     - versionName: '1.15'
       versionCode: 19
       commit: [PLACEHOLDER - Add actual git tag/commit]
       subdir: app
       sudo:
         - apt-get update
         - apt-get install -y openjdk-17-jdk-headless
         - update-alternatives --auto java
       gradle:
         - fdroid
   ```

3. **Submit Merge Request**
   - Create a merge request to the F-Droid data repository
   - Follow F-Droid's contribution guidelines

### Option 3: Self-Hosted F-Droid Repository

If you want to distribute through your own F-Droid repository:

1. **Set up F-Droid Server**
   - Follow [F-Droid Server Setup Guide](https://f-droid.org/docs/Running_the_Server/)

2. **Configure Repository**
   - Create repository configuration
   - Add app metadata
   - Build and sign APKs

## Required Actions by Maintainer

### Before Submission

1. **Set up Versioning**
   ```bash
   # Create and push git tags for releases
   git tag -a v1.15 -m "Release version 1.15 with F-Droid support"
   git push origin v1.15
   ```

2. **Update Documentation**
   - [ ] Add email address for F-Droid metadata (replace `[PLACEHOLDER - Add your email]`)
   - [ ] Add donation information if desired (replace `[PLACEHOLDER - Add if you want to include donation info]`)
   - [ ] Verify and update app description for F-Droid audience

### 3. Test F-Droid Build Thoroughly
   ```bash
   # Test clean build
   ./gradlew clean assembleFdroidRelease
   
   # Verify APK content
   aapt dump badging app/build/outputs/apk/fdroid/release/app-fdroid-release.apk
   
   # Check for any Google dependencies
   unzip -l app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep -i google
   ```
   
   **Note**: If you encounter Android Gradle Plugin resolution issues in CI environments, try using a local development machine or different Android SDK setup. The build configuration is correct, but some CI environments may have limitations with newer AGP versions.

4. **Create Release Notes**
   - Update [release notes](project-resources/google-play/release-notes.md) to mention F-Droid availability
   - Document F-Droid specific limitations (no email notifications)

### After F-Droid Acceptance

1. **Update README**
   - Add F-Droid badge and download link
   - Document differences between F-Droid and Google Play versions

2. **Maintain F-Droid Compatibility**
   - Keep `fdroid` flavor up to date with new releases
   - Test F-Droid builds with each release
   - Update F-Droid metadata when needed

## F-Droid Specific Limitations

### Features Not Available in F-Droid Build

1. **Email Notifications**
   - Requires external Mailgun API key
   - F-Droid policy prohibits requiring external commercial services
   - Users can still use Telegram, webhooks, and Slack for notifications

2. **Google Play In-App Reviews**
   - Not available on F-Droid
   - F-Droid users can provide feedback through GitHub issues

3. **Firebase Analytics and Crashlytics**
   - Replaced with basic Android logging
   - No tracking or data collection in F-Droid builds

### Working Features in F-Droid

- ✅ Battery and storage monitoring
- ✅ Telegram notifications
- ✅ REST webhook notifications  
- ✅ Slack webhook notifications
- ✅ Alert configuration and history
- ✅ Background monitoring with WorkManager
- ✅ Material3 UI with theming

## Troubleshooting

### Common Build Issues

1. **Missing Google Services**
   ```
   Error: Could not find google-services.json
   ```
   **Solution**: This error should not occur with fdroid flavor. If it does, ensure you're building the correct flavor:
   ```bash
   ./gradlew assembleFdroidRelease
   ```

2. **API Key Validation Errors**
   ```
   Error: EMAIL_API_KEY is not set
   ```
   **Solution**: The fdroid flavor should not require API keys. Check that BuildConfig is properly configured.

3. **Dependency Resolution Issues**
   - Ensure all Google/Firebase dependencies are marked as `gplayImplementation`
   - Clean and rebuild: `./gradlew clean build`

### Verifying F-Droid Compliance

```bash
# Check for proprietary code patterns
grep -r "google.*play" app/src/fdroid/ || echo "✅ No Google Play references in F-Droid source"
grep -r "firebase" app/src/fdroid/ || echo "✅ No Firebase references in F-Droid source"

# Verify APK content
./gradlew assembleFdroidRelease
unzip -l app/build/outputs/apk/fdroid/release/app-fdroid-release.apk | grep -v "\.dex\|\.so\|META-INF" | grep -i google
```

## Support and Resources

- **F-Droid Documentation**: https://f-droid.org/docs/
- **F-Droid Inclusion Policy**: https://f-droid.org/docs/Inclusion_Policy/
- **F-Droid Forum**: https://forum.f-droid.org/
- **Project Issues**: https://github.com/hossain-khan/android-remote-notify/issues

## Timeline Expectations

- **Initial Submission**: Can be done immediately after completing placeholders
- **F-Droid Review**: Typically 1-4 weeks depending on complexity
- **First Publication**: 2-6 weeks from initial submission
- **Update Processing**: 1-3 days for subsequent releases

---

**Note**: This guide contains placeholders marked with `[PLACEHOLDER]` that need to be filled in by the repository maintainer before submission.