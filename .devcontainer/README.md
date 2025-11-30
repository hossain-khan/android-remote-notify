# Dev Container Configuration

This directory contains the development container configuration for Android Remote Notify.

## Features

- **Base Image**: Java 21 (Bookworm)
- **Android SDK**: Manually installed via post-create script
- **Android Command Line Tools**: Version 11076708
- **Platform Tools**: Latest version
- **VS Code Extensions**: Kotlin, Gradle, Java, IntelliJ keybindings, and GitHub Copilot support

## Environment Variables

- `ANDROID_HOME`: `/usr/local/lib/android/sdk`
- `ANDROID_SDK_ROOT`: `/usr/local/lib/android/sdk`

## Post-Create Setup

The `post-create.sh` script automatically:
1. Downloads and installs Android SDK Command Line Tools if not present
2. Accepts Android SDK licenses
3. Installs Android SDK Platform 35 and Build Tools 35.0.0
4. Updates SDK components
5. Sets Gradle wrapper permissions
6. Pre-downloads Gradle dependencies

## Usage

### First Time Setup

1. Open the project in VS Code
2. When prompted, click "Reopen in Container"
3. Wait for the container to build and post-create script to complete

### Configuration

Before building, create a `local.properties` file in the project root with your email API key:

```properties
EMAIL_API_KEY=your_api_key_here
```

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Generating Code Coverage Report

```bash
./gradlew koverHtmlReportDebug
```

The coverage report will be available at:
- Module reports: `app/build/reports/kover/html/index.html`

### Formatting Code

```bash
./gradlew formatKotlin
```

### Linting Code

```bash
./gradlew lintKotlin
```

## Connecting Android Devices

The container is configured with `--privileged` mode to allow ADB access to connected devices. Your local `.android` directory is mounted to preserve ADB keys and settings.

## Troubleshooting

### SDK License Issues

If you encounter SDK license errors, run:
```bash
yes | sdkmanager --licenses
```

### Gradle Issues

If Gradle fails to sync, try:
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Android SDK Not Found

Verify the environment variables:
```bash
echo $ANDROID_HOME
echo $ANDROID_SDK_ROOT
```

Both should point to `/usr/local/lib/android/sdk`.

### EMAIL_API_KEY Error

If you see an error about `EMAIL_API_KEY`, ensure you've created the `local.properties` file with a valid API key as described in the Configuration section above.
