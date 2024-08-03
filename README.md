# Stash App for Android TV

This is an Android TV app for browsing images and playing scenes from a [Stash](https://github.com/stashapp/stash) server. Many devices running Android TV are supported, including Amazon Fire TV devices. See [device compatibility](https://github.com/damontecres/StashAppAndroidTV/wiki/Device-Compatibility) for more information.

Not all features of Stash are supported, but the basics for browsing, searching, and playing scenes work well.

The app is not intended to perform administrative functions such as scraping or editing details. Some curation tasks are available though such as adding tags or performers to scenes.

### Main page
![main Large](https://github.com/damontecres/StashAppAndroidTV/assets/154766448/79f374cd-ae97-4a8f-bbbe-07a8a25b7148)

## Setup

Make sure your Stash server is running and that you can access it over the network (not `localhost`).

Check the [Tips & Tricks](https://github.com/damontecres/StashAppAndroidTV/wiki/Tips-&-Tricks) page for some common issues and solutions!

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
1. [Configure the app](#configuration)
1. Optionally, install the [StashAppAndroidTV Companion](https://github.com/damontecres/StashAppAndroidTV-Companion) plugin on your server to enable additional features
    - Search for `StashAppAndroidTV Companion` on your server's Settings->Plugins page
    - Alternatively, trigger an install from the Android TV app in `Settings->Advanced->Install companion plugin`

### Upgrading the app

Starting with `v0.2.2`, the app will automatically check for updates and, as an experimental feature, the update can be installed in settings.

#### Develop build

This build has the latest features, but may be unstable or have bugs!

You can install the latest development debug build from the [develop pre-release](https://github.com/damontecres/StashAppAndroidTV/releases/tag/develop) using the same methods as above. The short code is `505547` or URL: https://aftv.news/505547

Starting with `v0.2.2`, you update in-app to the newest development build by changing the URL in `Settings->Advanced->Update URL` to https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/tags/develop (replace `latest` with `tags/develop`).

### Configuration

The first time you open the app, follow the prompts to configure the app to connect to your Stash server.

1. Enter the full URL of your Stash server (e.g. `http://192.168.1.122:9999`)
    - Don't use `localhost`; use the IP address or domain of your Stash server
    - If you have configured HTTPS/SSL/TLS, make sure to use `https://` instead of `http://`
1. If you have enabled authentication on your Stash server, enter the API Key
    1. Use your phone to browse to your Stash server and copy the API Key from the Settings->Security page (e.g. http://192.168.1.122:9999/settings?tab=security)
    1. Use your phone's [virtual remote control](https://support.google.com/chromecast/answer/11221499) to paste the API Key into the TV app
1. If you have trouble submitting the URL or API Key using the virtual remote control, [see some tips here](https://github.com/damontecres/StashAppAndroidTV/wiki/Tips-&-Tricks#i-cant-submit-the-server-url-when-using-a-remote-phone-app)

#### Multiple servers

You can configure multiple servers in the app. To add, remove, or switch servers, use the `Manage Servers` option in the settings.

### Compatibility

#### Server

The app strives to be compatible with the latest released version of Stash.

Currently, the minimum supported/tested server version is Stash `0.25.0`.

#### Devices

The app supports many devices running Android TV OS, such as the NVIDIA Shield, Amazon Fire TV devices (Fire OS 6 or greater), or Chromecast with Google TV.

See [Device Compatibility](https://github.com/damontecres/StashAppAndroidTV/wiki/Device-Compatibility) for more information.

## Companion plugin

The [StashAppAndroidTV Companion](https://github.com/damontecres/StashAppAndroidTV-Companion) plugin enables additional features in the app. It is not required to use the app, but it is recommended especially if you run into issues.

Currently, the plugin only supports receiving crash reports and logs from the app. But, this can help diagnose issues and improve the app.

## Contributions

Issues and pull requests are always welcome! UI/UX improvements are especially desired!

Please check before submitting that your issue or pull request is not a duplicate.

If you plan to submit a pull request, please read the [contributing guide](CONTRIBUTING.md) before submitting!

## Additional screenshots

### Scene list, filters, & sorting
![0_2_10_scenes](https://github.com/damontecres/StashAppAndroidTV/assets/154766448/092e0a87-e097-44c2-a605-e1ba93443e89)

### Performer page
![0_2_10_performer](https://github.com/damontecres/StashAppAndroidTV/assets/154766448/83eefe0d-800c-4a0c-bcc5-4176372d296d)
