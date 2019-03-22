# jb-test-app-android

Project utilizes Gradle, the most popupar build system for Android ATM.

To build the project use:
```
cd $projectDir
./gradlew clean build
```
Which will generate debug .apk file:
```
ls ./app/build/outputs/apk/debug/app-debug.apk
```

.apk-packaged application can be deployed to a device using Android SDK.
To check if device is connected to PC and if adb (android debug bridge) is aware of it, use:
```
$androidSdkDir/platform-tools/adb devices
```

Install built .apk file to connected device:
```
$androidSdkDir/platform-tools/adb install 
```

Start application manually (click 'JB test app' on device launcher after installation) or via command:
```
$androidSdkDir/platform-tools/adb shell am start -n com.jetbridge.testapp.yevhen/com.jetbridge.testapp.yevhen.MembersListActivity
```
Where 'com.jetbridge.testapp.yevhen.MembersListActivity' is main application screen.

------
Video of app usage:
https://www.dropbox.com/s/flp58m9s401xlla/20190322_180500.mp4?dl=0

Pre-built .apk file:
https://www.dropbox.com/s/fo23yk8gub8zgnn/app-release.apk?dl=0
