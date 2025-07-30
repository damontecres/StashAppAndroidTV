# Stash App for Android TV

This is an Android TV app for browsing images and playing scenes from a [Stash](https://github.com/stashapp/stash) server. Many devices running Android TV are supported, including Amazon Fire TV devices. See [device compatibility](https://github.com/damontecres/StashAppAndroidTV/wiki/Device-Compatibility) for more information.

The app supports browsing, searching, and filtering just about everything including scenes, images, performers, tags, etc. The app also supports video hardware decoding when available so your server can transcode less.

The app does not perform administrative functions such as scraping, but some curation tasks are available like adding tags or performers to scenes.

### Phones & tablets

The app is primarily designed for Android TV, but it can also run on Android phones and tablets. However, much of the UI is not yet fully optimized for touch input and may not as work well, especially on smaller screens.

###
![0_6_6_main](https://github.com/user-attachments/assets/6549ce26-bd8a-4b86-90c7-3447f7de3eef)

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

The first time you attempt an update, the OS should guide you through enabling the required additional permissions for the app to install updates.

### Configuration

The first time you open the app, follow the prompts to configure the app to connect to your Stash server.

1. Enter the full URL of your Stash server (e.g. `http://192.168.1.122:9999`)
    - Don't use `localhost`; use the IP address or domain of your Stash server
    - Don't forget to include the port which 9999 by default
    - If you have configured HTTPS/SSL/TLS, make sure to use `https://` instead of `http://`
2. If you have enabled authentication on your Stash server, you must enter its API Key
    1. Use your phone to browse to your Stash server and copy the API Key from the Settings->Security page (e.g. http://192.168.1.122:9999/settings?tab=security)
    2. Use your phone's [virtual remote control](https://support.google.com/chromecast/answer/11221499) or the `Amazon Fire TV` app to paste the API Key into the TV app
    3. You can also use your username and password instead. After entering your username and password, the app will automatically retrieve (or generate) the API Key from the server.
3. If you have trouble submitting the URL or API Key using the virtual remote control, [see some tips here](https://github.com/damontecres/StashAppAndroidTV/wiki/Tips-&-Tricks#i-cant-submit-the-server-url-when-using-a-remote-phone-app)

#### Multiple servers

You can configure multiple servers in the app. To add, remove, or switch servers, use the `Manage Servers` option in the settings or click the Stash icon on the main page.

### Compatibility

#### Server

The app strives to be compatible with the latest released version of Stash.

Currently, the minimum supported server version is Stash `0.28.0`.

#### Devices

The app supports many devices running Android TV OS, such as the NVIDIA Shield, Amazon Fire TV devices (Fire OS 6 or greater), or Chromecast with Google TV.

It also supports Android phones and tablets, but the UI is not fully optimized for touch input.

See [Device Compatibility](https://github.com/damontecres/StashAppAndroidTV/wiki/Device-Compatibility) for more information.

## Companion plugin

The [StashAppAndroidTV Companion](https://github.com/damontecres/StashAppAndroidTV-Companion) plugin enables additional features in the app. It is not required to use the app, but it is recommended especially if you run into issues.

Currently, the plugin only supports receiving crash reports and logs from the app. But, this can help diagnose issues and improve the app.

## Contributions

Issues and pull requests are always welcome! UI/UX improvements are especially desired!

Please check before submitting that your issue or pull request is not a duplicate.

If you plan to submit a pull request, please read the [contributing guide](CONTRIBUTING.md) before submitting!

## Additional screenshots

### Scene list
![0_6_6_scene_grid](https://github.com/user-attachments/assets/10f3d32e-ce04-4456-a992-8bfa13291f14)

### Edit scene details
![0_6_6_scene_edit](https://github.com/user-attachments/assets/bbf7d0e4-2a93-4dcd-a5ee-59f4100e5ae0)

### Filtering scenes
![0_6_6_scene_filter_create](https://github.com/user-attachments/assets/9dc4da09-0398-4eab-be5f-620b04f14b03)

### Performer details
![0_6_6_performer](https://github.com/user-attachments/assets/76ffbf8f-9329-4f8a-a11c-881441af75f8)

### Playlist
![0_6_6_playlist](https://github.com/user-attachments/assets/b93677d8-c865-4b39-b948-db9c7fa1ab62)
