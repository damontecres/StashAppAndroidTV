# Stash App for Android TV

This is a basic Android TV app for browsing and playing videos from a [Stash](https://github.com/stashapp/stash) server.

Not all features of Stash are supported, but the basics for browsing, searching, and playing scenes should work.

The app is not intended to perform administrative functions such as scanning, scraping, or editing details.

## Setup

Make sure your Stash server is running and that you can access it over the network (not `localhost`).

### Installation

1. Enable side-loading "unknown" apps
    - https://androidtvnews.com/unknown-sources-chromecast-google-tv/
    - https://www.xda-developers.com/how-to-sideload-apps-android-tv/
    - https://developer.android.com/distribute/marketing-tools/alternative-distribution#unknown-sources
    - https://www.aftvnews.com/how-to-enable-apps-from-unknown-sources-on-an-amazon-fire-tv-or-fire-tv-stick/
1. Install the APK on your Android TV device with one of these options:
    - Install a browser program such as [Downloader](https://www.aftvnews.com/downloader/), use it to get the latest apk with short code `745800` or URL: https://aftv.news/745800
    - Download the latest APK release from the [releases page](https://github.com/damontecres/StashAppAndroidTV/releases/latest) or https://aftv.news/745800
        - Put the APK on an SD Card/USB stick/network share and use a file manager app from the Google Play Store / Amazon AppStore (e.g. `FX File Explorer`). Android's preinstalled file manager probably will not work!
        - Use `Send files to TV` from the Google Play Store on your phone & TV
        - (Expert) Use [ADB](https://developer.android.com/studio/command-line/adb) to install the APK from your computer ([guide](https://fossbytes.com/side-load-apps-android-tv/#h-how-to-sideload-apps-on-your-android-tv-using-adb))

The app does not auto-update, so you will need to repeat this process for each new release.

### Configuration

1. Open the app
1. Open settings (the gear icon at the top-right)
1. Enter the URL of your Stash server (e.g. `http://192.168.1.122:9999`)
    - Don't use `localhost`; use the IP address or domain of your Stash server
    - If you have configured HTTPS, make sure to use `https://` instead of `http://`
1. If you have enabled authentication on your Stash server, enter the API Key
    1. Use your phone to browse to your Stash server and copy the API Key from the Settings->Security page (e.g. http://192.168.1.122:9999/settings?tab=security)
    1. Use your phone's [virtual remote control](https://support.google.com/chromecast/answer/11221499) to paste the API Key into the TV app
    - Do not enter an API Key if authentication is not enabled on your Stash server!
1. Select `Test Connection` to verify that the app can connect to your Stash server

## Contributions

Issues and pull requests are always welcome! UI/UX improvements are especially desired!

Please check before submitting that your issue or pull request is not a duplicate.

If you plan to submit a pull request, please read the [contributing guide](CONTRIBUTING.md) before submitting!
