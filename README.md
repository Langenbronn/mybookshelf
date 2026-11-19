# Gnome Library

A smartphone app for **Android** and **iPhone**, written once in **Kotlin** using
[Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/).

It has a bottom menu with 5 pages: Library, Scan, Places, Favorites and Settings.

**Scan** reads a book's barcode (ISBN) with the camera, or lets you type the ISBN, and shows
the book's details (cover, authors, publisher, pages, description) from Google Books, with
Open Library as a fallback.
- Android: uses Google's code scanner (needs Google Play services; on the emulator, use a
  "Google Play" system image, or type the ISBN by hand).
- iOS: uses the camera directly (`scanner/BarcodeScanner.ios.kt`); permission text is in `Info.plist`.

Tap **Add to my library** under a found book to save it. The **Library** page lists the saved
books with all their details. They are stored on the phone (`data/Library.kt`), so they stay
after closing the app.

## Open the project (Android Studio)

1. Android Studio > **File > Open** > select this `my-bookshelf` folder.
2. Wait for the **Gradle sync** (the first time it downloads Gradle and the libraries, which takes a few minutes).
3. Recommended: install the **Kotlin Multiplatform** plugin (*Settings > Plugins*).
4. Pick the `composeApp` run configuration, choose an emulator or a phone, then click ▶ **Run**.

> **One-time step:** the `gradlew` scripts are not in the repo yet. Create them from Android Studio:
> open the **Gradle** panel (right side) > `GnomeLibrary` > *Tasks* > *build setup* > double-click **wrapper**.
> You need them for the command line and for iOS builds.

## Project structure

```
my-bookshelf/
├── gradle/libs.versions.toml        ← ALL library versions (upgrade here)
├── composeApp/
│   ├── build.gradle.kts             ← app build config (dependencies, Android settings)
│   └── src/
│       ├── commonMain/kotlin/...    ← ⭐ SHARED code (Android + iOS): work here
│       │   ├── App.kt               ← root: top bar + bottom menu + current page
│       │   ├── navigation/Screen.kt ← the list of menu pages
│       │   └── ui/
│       │       ├── screens/         ← one file per page
│       │       └── theme/Theme.kt   ← app colors
│       ├── androidMain/             ← Android only (MainActivity, manifest, icon)
│       └── iosMain/                 ← iOS only (MainViewController)
└── iosApp/                          ← Xcode project (thin Swift wrapper)
```

## How to add a new page

1. **`navigation/Screen.kt`**: add an entry, e.g. `Profile("Profile", Icons.Filled.Person),`
2. **`ui/screens/ProfileScreen.kt`**: create the page:
   ```kotlin
   @Composable
   fun ProfileScreen() {
       Text("My profile")
   }
   ```
3. **`App.kt`**: add the case in the `when`: `Screen.Profile -> ProfileScreen()`

The page then appears in the menu automatically. (Kotlin reports an error if you forget step 3.)

## iPhone (iOS)

Building for iOS **requires a Mac with Xcode**. Apple doesn't allow building iOS apps on Windows.
On a Mac: open `iosApp/iosApp.xcodeproj` in Xcode and click Run
(or use Android Studio on the Mac with the Kotlin Multiplatform plugin).
For a real iPhone, set your Apple `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

On Windows you develop and test everything on Android. The same code runs on iOS.

## Useful links

- Compose basics: https://developer.android.com/develop/ui/compose/tutorial
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html
