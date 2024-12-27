# Developer's guide

See also the [Contributing](CONTRIBUTING.md) guide for general information on contributing to the project.

##  Overview

This project is an Android TV client for Stash. It is written in Kotlin and uses the Apollo GraphQL client to interact with the server.

The app uses Android's [Leanback](https://developer.android.com/training/tv/playback/leanback/) library to provide a TV-friendly interface. There are custom implementations for paging and navigation though.

The app uses a single Activity (`RootActivity`). Each page is a `Fragment` that can contain other fragments.

### Modules

The app is divided into several modules:
- `buildSrc` - pre-build tasks and dependencies
- `apollo-compiler` - custom Apollo compiler to manipulate the generated code
- `app` - the main app module
- `stash-server` - a git submodule for the server to get assets and graphql schemas

### Internationalization

Android's native internationalization is used for the app, but not all of the strings used in code are correctly stored in a resource file allowing for translation.

When possible, it is preferable to use a string from the server because the server has much better translation support. These strings are converted during the build process from `stash-server/ui/v2.5/src/locales` JSON files into `stash_strings.xml` Android resource files via a gradle task defined in `buildSrc`. The resulting string resources are prefixed with `stashapp_`.

### Major components

#### GraphQL

The app uses [Apollo Kotlin](https://www.apollographql.com/docs/kotlin) to automatically generate all GraphQL schemas, queries, and mutations. The schema is inherited from the server and queries/mutations are written in `.graphql` files in the `app/src/main/graphql` directory. The `apollo-compiler` module is used to manipulate the generated code to mark it with interfaces and enable serialization.

All interactions with the server are done via GraphQL using one of the `StashEngine` subclasses: `QueryEngine`, `MutationEngine`, or `SubscriptionEngine`. Each query/mutation/subscription roughly translates to a function in one of those classes.

If lower level network access is needed, `StashClient` can be used to create an `ApolloClient` for graphql or an `OkHttpClient` for other requests.

Tips:
- Install the Apollo plugin in Android Studio
- Don't over-fetch data
- Use or create "slim" versions of types when possible
- Avoid fetching counts or getting lists unless necessary for the UI

#### Navigation

Navigating the app is handled by the `NavigationManager` class. This class takes a `Destination` object and creates/displays the appropriate fragment. Destinations can have arguments that are passed to the fragment (retrieved via `requireArguments().getDestination<DestinationClassName>()`).

#### Paging

Paging is handled by wrapping a `StashPagingSource` in a `PagingObjectAdapter` (for paging items in the UI) or `StashSparseFilterFetcher` (for fetching arbitrary indexes not necessarily for the UI).

`StashPagingSource` in turn wraps a `DataSupplier` which provides graphql query objects used to fetch data from the server.

#### Filtering

A filter is encapsulated in a `FilterArgs` object. This object contains a "find" filter (server side sort and page info) and an "object" filter (the requested filter options). This class is `Serializable` and can be passed between fragments.

`DataSupplierFactory` gets the right `DataSupplier` for a given filter.

#### UI

The UI is built using Leanback's `Presenters` to create cards or "full width" displays.

`StashPresenter` is main abstract class for card presenters and implementations are responsible for binding data to views. The `StashPresenter.defaultClassPresenterSelector()` function returns a `Presenter` that can used for most types of data displayed as Cards.

#### Playback

The app uses [media3's `ExoPlayer`](https://github.com/androidx/media) for playback. The `PlaybackFragment` abstract class handles most of the playback logic.

`CodecSupport` determines which codecs are supported by the device. `StreamUtils` uses that information to determine if the stream from the server can be directly played or if it needs to be transcoded.
