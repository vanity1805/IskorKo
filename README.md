## Technology Stack (languages, frameworks, tools used)
- Kotlin + Jetpack Compose (UI)
- Android SDK / Gradle
- Firebase Auth + Cloud Firestore
- Android Studio

## Installation / Setup Instructions
1) Open the project in Android Studio and let Gradle sync.
2) Install required Android SDK components when prompted.
3) Configure Firebase:
   - Create a Firebase project and add an Android app.
   - Download `google-services.json` and place it in `app/`.
   - Enable Firebase Auth and Cloud Firestore in the Firebase console.
4) Device setup:
   - Android 10+ device or emulator.
   - If using a phone, enable Developer Options and USB Debugging.
5) Run:
   - Click Run in Android Studio, or use the commands below.

### Common Commands
From the project root:
```bash
# List connected devices/emulators
adb devices

# Start an emulator (example)
emulator -list-avds
emulator -avd <AVD_NAME>

# Build debug APK
gradlew assembleDebug

# Build release APK
gradlew assembleRelease

# Clean build outputs
gradlew clean

# Install debug APK to a connected device
gradlew installDebug

# Install a release APK (if configured)
gradlew installRelease

# Build an Android App Bundle
gradlew bundleRelease

# Run unit tests
gradlew test

# Run instrumented tests (requires device/emulator)
gradlew connectedAndroidTest

# Lint checks
gradlew lint

# Show Gradle tasks
gradlew tasks

# View device logs
adb logcat
```

## User Guide (basic usage instructions)
- Choose a role (Professor or Student) on the profile selection screen.
- Professors:
  - Create a class and share the class code.
  - Create exams and scan answer sheets.
  - Review results and export grades.
- Students:
  - Join a class using the provided class code.
  - View grades and class notifications.
