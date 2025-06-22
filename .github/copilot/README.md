# GitHub Copilot Configuration

This directory contains configuration files for GitHub Copilot to work properly with this Android project.

## Files

### `firewall.yml`
Configures the firewall to allow GitHub Copilot access to necessary domains for Android development:

- **Google repositories**: `dl.google.com`, `maven.google.com` - Required for Android Gradle Plugin and dependencies
- **Google services**: `*.googleapis.com`, `*.gstatic.com` - Required for various Google services
- **Firebase**: `*.firebase.google.com`, `*.firebaseapp.com` - Required for Firebase services used by the app
- **Maven repositories**: `repo1.maven.org`, `central.maven.org` - Required for third-party dependencies
- **Gradle services**: `plugins.gradle.org`, `services.gradle.org` - Required for Gradle plugin resolution

### Background

This configuration was added to fix issue #319 where GitHub Copilot agent was blocked by firewall rules from accessing `dl.google.com`, preventing it from downloading Android SDK components and dependencies.

## Alternative Setup

If the firewall configuration doesn't work, there's also a setup workflow at `.github/workflows/copilot-setup.yml` that can be run to pre-download dependencies before the firewall is enabled.

## References

- [Customizing the development environment for Copilot coding agent](https://docs.github.com/en/copilot/customizing-copilot/customizing-the-development-environment-for-copilot-coding-agent)
- [Customizing or disabling the firewall for Copilot coding agent](https://docs.github.com/en/copilot/customizing-copilot/customizing-or-disabling-the-firewall-for-copilot-coding-agent)