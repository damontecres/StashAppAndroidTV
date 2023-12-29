# Stash App for Android TV

A basic Android TV app for browsing and playing videos from a [Stash](https://github.com/stashapp/stash) server.

## Setup

### Installation

1. Download the latest APK release from the [releases page](https://github.com/damontecres/StashAppAndroidTV/releases/latest)
2. Install the APK on your Android TV device

    a. Put the APK on an SD Card/USB stick/network share and use a file manager app from the Google Play Store / Amazon AppStore (e.g. FX File Explorer). Android's preinstalled file manager does not work!

    b. You can use [ADB](https://developer.android.com/studio/command-line/adb) to install the APK from your computer ([guide](https://fossbytes.com/side-load-apps-android-tv/#h-how-to-sideload-apps-on-your-android-tv-using-adb))

The app does not auto-update, so you will need to repeat this process for each new release.

### Configuration

1. Open the app
2. Go to Settings (the wrench or gear icon at the top)
3. Enter the URL of your Stash server (e.g. `http://192.168.1.122:9999`)
4. If you have enabled authentication on your Stash server, you need to enter the API Key

    a. Use your phone to browse to your Stash server and copy the API Key from the "Settings" page

    b. Use your phone's [virtual remote control](https://support.google.com/chromecast/answer/11221499) to paste the API Key into the app

## Contributions

Issues and pull requests are always welcome! UI/UX improvements are especially desired!

Please check before submitting that your issue or pull request is not a duplicate.

If you plan to submit a pull request, please read the [contributing guide](CONTRIBUTING.md) before submitting!
