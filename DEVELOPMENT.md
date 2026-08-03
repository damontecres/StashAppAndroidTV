# Developer's guide

See also the [Contributing](CONTRIBUTING.md) guide for general information on contributing to the project.

##  Overview

This project is an Android TV client for Stash. It is written in Kotlin and uses the Apollo GraphQL client to interact with the server.

### Tech stack

* [JetPack Compose](https://developer.android.com/jetpack/compose) for the UI
* [Koin](https://insert-koin.io/) for dependency injection
* [Navigation 3](https://developer.android.com/guide/navigation/navigation-3) for displaying app pages
* [Room](https://developer.android.com/training/data-storage/room) & [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for local data storage
* [Media3/ExoPlayer](https://developer.android.com/media/media3/exoplayer) for media playback
* [MPV/libmpv](https://github.com/mpv-player/mpv) for media playback
* [Coil](https://coil-kt.github.io/coil/) for image loading
* [OkHttp](https://square.github.io/okhttp/) for HTTP requests

There is a single Activity, `MainActivity`. The app generally uses a MVVM architecture.

### Modules

The app is divided into several modules:
- `buildSrc` - pre-build tasks and dependencies
- `apollo-compiler` - custom Apollo compiler to manipulate the generated code
- `app` - the main app module
- `stash-server` - a git submodule for the server to get assets and graphql schemas

### Internationalization

Android's native internationalization is used for the app, but not all of the strings used in code are correctly stored in a resource file allowing for translation.

When possible, it is preferable to use a string from the server because the server has much better translation support. These strings are converted during the build process from `stash-server/ui/v2.5/src/locales` JSON files into `stash_strings.xml` Android resource files via a gradle task defined in `buildSrc`. The resulting string resources are prefixed with `stashapp_`.

## Major components

### GraphQL

The app uses [Apollo Kotlin](https://www.apollographql.com/docs/kotlin) to automatically generate all GraphQL schemas, queries, and mutations. The schema is inherited from the server and queries/mutations are written in `.graphql` files in the `app/src/main/graphql` directory. The `apollo-compiler` module is used to manipulate the generated code to mark it with interfaces and enable serialization.

All interactions with the server are done via GraphQL using one of the `StashEngine` subclasses: `QueryEngine`, `MutationEngine`, or `SubscriptionEngine`. Each query/mutation/subscription roughly translates to a function in one of those classes.

If lower level network access is needed, the `StashApi` has an `ApolloClient` for graphql. For general HTTP requests an `OkHttpClient` (with or without authentication) can be injected.

Tips:
- Install the Apollo plugin in Android Studio
- Don't over-fetch data
- Use or create "slim" versions of types when possible
- Avoid fetching counts or getting lists unless necessary for the UI

### Paging

Paging is handled by wrapping a `StashPagingSource` in a `ComposePager` or `PagingObjectAdapter` (for paging items in the UI) or `StashSparseFilterFetcher` (for fetching arbitrary indexes not necessarily for the UI).

`StashPagingSource` in turn wraps a `DataSupplier` which provides graphql query objects used to fetch data from the server.

### Filtering

A filter is encapsulated in a `FilterArgs` object. This object contains a "find" filter (server side sort and page info) and an "object" filter (the requested filter options). This class is `Serializable` and can be passed between fragments.

`DataSupplierFactory` gets the right `DataSupplier` for a given filter.

### UI

The UI is built using Jetpack Compose. The main fragment is `NavDrawerFragment` which contains the `ApplicationContent` composable. This composable is the main entry point for the app and contains the navigation drawer and the main content area.

In order to support both TV and phone/tablet UIs, there are a few compatibility composables for common UI elements such as Buttons, Cards, etc.

The UI is contained in the `com.github.damontecres.stashapp.ui` package.

### Playback

The app uses [media3's `ExoPlayer`](https://github.com/androidx/media) for playback. The composables for playback are in the `com.github.damontecres.stashapp.ui.components.playback` package with most logic in `PlaybackPageContent` and `PlaybackViewModel`.

`CodecSupport` determines which codecs are supported by the device. `StreamUtils` uses that information to determine if the stream from the server can be directly played or if it needs to be transcoded.

#### Extensions

There are several native components for extra playback compatibility. This includes Media3 ffmpeg/av1 decoders and `libmpv`. These extensions are not required to build the app, but without them some functionality will not work.

If you want to include these in a local build, see the [instructions here](https://github.com/damontecres/wholphin-extensions?tab=readme-ov-file#usage) for configuring the repository.

You can also build the extensions locally from https://github.com/damontecres/wholphin-extensions and include them in `app/libs`. The gradle build dependency resolution prefers these local files over fetching from the remote maven registry.

Finally, if no `wholphin-mpv` implementation is found, `:app:mpv-stub` will be used. This allows the app to compile, but any runtime usage of MPV will throw an exception.

### Image loading

The app uses [Coil](https://coil-kt.github.io/coil/) for image loading for composables.

It also uses [`Glide`](https://github.com/bumptech/glide) for image loading in non-Compose code. However, developers should use the `StashGlide` class which handles setting some defaults related to caching. For example: `Glide.with(context).load(url)` should be `StashGlide.with(context, url)`

### Read only mode

All mutation should be performed using the `MutationEngine` which helps ensure that the read only mode is respected by blocking any mutations.

But for better UI/UX, buttons and other controls that would trigger a mutation should be removed or disabled if read only mode is enabled. This can be checked with `com.github.damontecres.stashapp.util.ConstantsKt#readOnlyModeEnabled`.
