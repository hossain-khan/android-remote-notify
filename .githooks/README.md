# Git Hooks for Android Remote Notify

This directory contains git hooks to ensure code quality and consistency in the Android Remote Notify project.

## Available Hooks

### Pre-commit Hook

The `pre-commit` hook automatically runs `./gradlew formatKotlin` before each commit to ensure consistent code formatting across the project.

#### What it does:

1. **Checks for staged files**: Only runs if there are files staged for commit
2. **Filters Kotlin files**: Only processes `.kt` and `.kts` files that are staged
3. **Runs formatKotlin**: Executes the Gradle `formatKotlin` task to format the code
4. **Handles formatting results**:
   - If formatting **succeeds** and files are modified: automatically adds the formatted files to the commit
   - If formatting **fails**: stops the commit and displays error messages
   - If no changes are needed: proceeds with the commit normally

#### Benefits:

- ✅ **Consistent formatting**: Ensures all committed Kotlin code follows the project's formatting standards
- ✅ **Automatic fixing**: Automatically formats and includes properly formatted files in commits
- ✅ **Early feedback**: Catches formatting issues before code reaches the repository
- ✅ **Zero maintenance**: Runs automatically without manual intervention

## Setup Instructions

To enable the git hooks in your local repository, follow these steps:

### Option 1: Configure Git to Use .githooks Directory (Recommended)

```bash
# Navigate to the project root
cd /path/to/android-remote-notify

# Configure git to use the .githooks directory
git config core.hooksPath .githooks
```

### Option 2: Manual Hook Installation

```bash
# Navigate to the project root
cd /path/to/android-remote-notify

# Copy the hook to your .git/hooks directory
cp .githooks/pre-commit .git/hooks/pre-commit

# Ensure the hook is executable
chmod +x .git/hooks/pre-commit
```

### Verification

To verify the hook is properly installed:

```bash
# Check git configuration (Option 1)
git config core.hooksPath

# Or check if hook exists (Option 2)
ls -la .git/hooks/pre-commit
```

## Testing the Hook

You can test the pre-commit hook by:

1. **Making a formatting change** to a Kotlin file (e.g., add extra spaces)
2. **Staging the file**: `git add filename.kt`
3. **Attempting to commit**: `git commit -m "test commit"`
4. **Observing the hook behavior**: The hook should format the file and include it in the commit

## Bypassing the Hook (When Needed)

In rare cases where you need to bypass the pre-commit hook:

```bash
# Skip all pre-commit hooks
git commit --no-verify -m "commit message"
```

**⚠️ Note**: Use `--no-verify` sparingly and only when absolutely necessary, as it bypasses important code quality checks.

## Troubleshooting

### Hook not running
- Verify the hook is executable: `chmod +x .githooks/pre-commit`
- Check git configuration: `git config core.hooksPath`
- Ensure you're in the project root directory

### formatKotlin task fails
- Make sure the project builds successfully: `./gradlew build`
- Ensure `local.properties` exists with required `EMAIL_API_KEY`: 
  ```
  EMAIL_API_KEY=your_api_key_here
  ```
- Check that you have the required dependencies and configurations
- Verify the `kotlinter` plugin is properly configured in `app/build.gradle.kts`
- Ensure Android SDK is properly installed and configured

### Permission issues
- Ensure the hook script has execute permissions: `chmod +x .githooks/pre-commit`
- Check that you can run gradle tasks: `./gradlew tasks`

## Related Documentation

- [Kotlinter Plugin Documentation](https://github.com/jeremymailen/kotlinter-gradle)
- [Git Hooks Documentation](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
- [Project Contributing Guidelines](../README.md)