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

After the initial install above, the app will automatically check for updates which can then be installed in settings.

The first time you attempt an update, the Android TV OS should guide you through enabling the required additional permissions for the app to install updates.

#### Develop build

This build is the current work-in-progress. It has the latest features, but may be unstable or have bugs!

You can install the latest development debug build from the [develop pre-release](https://github.com/damontecres/StashAppAndroidTV/releases/tag/develop) using the same methods as above. The short code is `505547` or URL: https://aftv.news/505547

You can also use the in-app updater for development builds by changing the URL in `Settings->Advanced->Update URL` to https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/tags/develop (replace `latest` with `tags/develop`).

### Configuration

The first time you open the app, follow the prompts to configure the app to connect to your Stash server.

1. Enter the full URL of your Stash server (e.g. `http://192.168.1.122:9999`)
    - Don't use `localhost`; use the IP address or domain of your Stash server
    - Don't forget to include the port which 9999 by default
    - If you have configured HTTPS/SSL/TLS, make sure to use `https://` instead of `http://`
1. If you have enabled authentication on your Stash server, you will be prompted to enter the API Key
    1. Use your phone to browse to your Stash server and copy the API Key from the Settings->Security page (e.g. http://192.168.1.122:9999/settings?tab=security)
    1. Use your phone's [virtual remote control](https://support.google.com/chromecast/answer/11221499) to paste the API Key into the TV app
1. If you have trouble submitting the URL or API Key using the virtual remote control, [see some tips here](https://github.com/damontecres/StashAppAndroidTV/wiki/Tips-&-Tricks#i-cant-submit-the-server-url-when-using-a-remote-phone-app)

#### Multiple servers

You can configure multiple servers in the app. To add, remove, or switch servers, use the `Manage Servers` option in the settings or click the Stash icon on the main page.

### Compatibility

#### Server

The app strives to be compatible with the latest released version of Stash.

Currently, the minimum supported/tested server version is Stash `0.27.0`.

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

### Scene list with sorting & filters

![0_4_2_scenes](https://github.com/user-attachments/assets/438fe917-1ea0-4f65-9c96-ddd2ace0504a)

### Performer page
![0_4_0_performer](https://github.com/user-attachments/assets/16ae2514-6b00-425a-82a2-9db0f4de51d6)
![0_4_0_performer_scenes_sort](https://github.com/user-attachments/assets/c5b4e89b-b4de-4499-88a1-828eeda1b550)

### Playlist
![0_4_0_playlist](https://github.com/user-attachments/assets/f06b0ce3-82fe-4cbd-a8b5-c2213d8c33f7)
