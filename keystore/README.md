## Android Signing Keystore

This directory contains keystores for signing Android builds.

### Debug Keystore

For local development, a debug keystore (`debug.keystore`) is used with standard Android debug credentials:
- Store Password: `android`
- Key Alias: `androiddebugkey`
- Key Password: `android`

See https://developer.android.com/studio/publish/app-signing#debug-mode

### Release Keystore (Production)
Release builds are signed by workflows using securely stored keystore files and credentials.
As soon as a tagged release is created, the release workflow signs the APK using the production keystore and uploads it to GitHub Releases.

For example:

<img src="../project-resources/static-res/github-release-build-assets-example.png" width="500" alt="GitHub Release Assets with built APK" />

### Workflows

GitHub Actions workflows are available for building releases:

- **[android-release.yml](https://github.com/hossain-khan/android-remote-notify/actions/workflows/android-release.yml)**: Builds release APK and AAB on main branch pushes and GitHub releases

### Setting Up Release Signing (for maintainers)

To set up release signing:

1. Create a release keystore file
2. Convert it to base64: `base64 -i your-release.keystore > keystore-base64.txt`
3. Add the following secrets in GitHub repository settings:
   - `KEYSTORE_BASE64`: Content of `keystore-base64.txt`
   - `KEYSTORE_PASSWORD`: Keystore password
   - `KEY_ALIAS`: Key alias name
   - `KEY_PASSWORD` (optional): Key password if different from keystore password
   - `EMAIL_API_KEY`: API key for email notifications (required for release builds)
