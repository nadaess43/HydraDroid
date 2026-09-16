<div align="center">

# HydraDroid

**Hydra catalogue, torrents and game library — natively on Android.**

*An unofficial Android port of [Hydra Launcher](https://github.com/hydralauncher/hydra), adapted for touch screens.*

[![Version](https://img.shields.io/badge/version-0.0.1--alpha-orange)](https://drive.google.com/drive/folders/1YicRTRJrszcRgVgd9hhXr94SQx0qOsiJ?usp=sharing)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[Download](#-download) • [Screenshots](#-screenshots) • [Features](#-features) • [Building](#-building-from-source) • [FAQ](#-faq)

</div>

---

## 📥 Download

Get the latest APK from Google Drive:

**👉 [Latest build (Google Drive)](https://drive.google.com/drive/folders/1YicRTRJrszcRgVgd9hhXr94SQx0qOsiJ?usp=sharing)**

> Requires **Android 8.0 (API 26) or higher**. No root needed.
> If Android blocks the install, allow *“Install unknown apps”* for your browser/file manager.

---

## 📸 Screenshots

| Home | Catalogue | Game details |
|---|---|---|
| ![Home](docs/screenshots/home.png) | ![Catalogue](docs/screenshots/catalogue.png) | ![Game details](docs/screenshots/details.png) |

| Library | Downloads | Settings |
|---|---|---|
| ![Library](docs/screenshots/library.png) | ![Downloads](docs/screenshots/downloads.png) | ![Settings](docs/screenshots/settings.png) |

<!-- Screenshots are placeholders — real captures will be added here. -->

---

## ✨ Features

### 📚 Catalogue
- Full Hydra catalogue with search (debounced, history, live suggestions)
- Sections: **Trending now**, **Top games of the week**, **Games with achievements**
- Genre filters, download-source filters, 4 sort orders
- Infinite feed on Home + paginated catalogue with result counts
- 🎲 **Surprise me** — random game picker (Steam250 lists, 7-day cache, instant offline picks)

### 🎮 Game details
- Hero banner, clean description (no HTML garbage), screenshot gallery with fullscreen viewer
- Download options (repacks) with source badges, sizes and dates
- Reviews with translations, HowLongToBeat, ProtonDB badges
- One-tap **Download**, **Queue**, **Add to library**

### 📁 Library
- Local library backed by Room: add from catalogue / game page / folder scan
- Favorites, collections, PC / Retro categories, 4 sort orders, search
- Per-game download button, custom local files support

### ⬇️ Downloads — real torrent client
- **Torrents**: libtorrent 2.0 via jlibtorrent (DHT, magnets, `.torrent` files, seeding, per-file selection, speed/connection limits, custom + built-in trackers)
- **HTTP**: resumable downloads with `Range` support, SAF folder picker
- **Debrid**: Real-Debrid, TorBox, Premiumize — direct unrestrict + remote magnet downloads (no seeding needed)
- **Hosters**: Gofile, PixelDrain, Mediafire, FuckingFast, Rootz, Datanodes, VikingFile, Archive.org — page links auto-resolved to direct files, just like the PC launcher
- Per-link **service picker** (Torrent / hoster / Direct / via Debrid), pause/resume, errors with retry, archive extraction (zip/7z/rar/tar), foreground service with auto-resume after restart

### 👤 Profile, notifications & settings
- Profile with friends & online status, notifications feed
- Settings: English / Russian UI (real-time switch), Debrid tokens, download folder, speed & seeding limits, global trackers, download-source management
- Encrypted token storage, crash-safe reinstall (no more splash-screen deaths)

---

## 🛠 Tech stack

| Layer | Technology |
|---|---|
| UI | Kotlin, Jetpack Compose (Material 3), Navigation Compose, Coil |
| Data | Room, DataStore Preferences, EncryptedSharedPreferences |
| Network | Retrofit + Moshi + OkHttp |
| Torrents | jlibtorrent (libtorrent 2.0, all ABIs) |
| Archives | commons-compress, junrar |
| Backend | Hydra public API (`hydra-api-us-east-1.losbroxas.org`) |

**Project layout:**

```
app/src/main/java/com/hydradroid/
├── MainActivity.kt            # BottomBar tabs, header + search, nav graph
├── HydraDroidApp.kt           # Room, session restore, image cache
├── ui/
│   ├── screens/               # Home, Catalogue, GameDetails, Library,
│   │                          # Downloads, Profile, Settings, dialogs
│   ├── components/            # GameCard, HeroBanner, buttons, badges
│   ├── theme/                 # Hydra colors / type (ported from original SCSS)
│   └── i18n/                  # EN/RU string tables, language engine
├── data/
│   ├── remote/                # Hydra API (Retrofit service + client)
│   ├── local/                 # Room DB, DataStore prefs, token store
│   ├── torrent/               # libtorrent engine wrapper
│   ├── download/              # Resumable HTTP downloader (File + SAF)
│   ├── hosters/               # Gofile/PixelDrain/Mediafire/… resolvers
│   ├── debrid/                # Real-Debrid / TorBox / Premiumize
│   ├── archive/               # zip/7z/rar/tar extractor
│   └── random/                # Surprise-me picker (Steam250)
└── service/
    └── DownloadService.kt     # Foreground download service
```

---

## 🔨 Building from source

**Requirements:** Android Studio Ladybug or newer, JDK 17.

```bash
# 1. Open the HydraDroid/ folder in Android Studio
# 2. Make sure local.properties points to your SDK:
#    sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk

# Debug APK
.\gradlew.bat :app:assembleDebug --offline

# Release APK (R8 + signed with debug key, for sideload testing)
.\gradlew.bat :app:assembleRelease --offline
```

Output: `app/build/outputs/apk/<debug|release>/app-<debug|release>.apk`

---

## ❓ FAQ

**Does it launch games?**
No — it's a catalogue + downloader + library for your phone. Launching PC games is out of scope.

**Do I need a Hydra account?**
No. Browsing, torrents, HTTP and hoster downloads work without login. Account sign-in is not wired yet (placeholder screen for now).

**Where do download sources come from?**
Settings → Download sources. The list starts empty; add sources by URL. Filtering, fingerprints and repack availability follow the enabled sources.

**The app reinstalled and crashed on splash before — fixed?**
Yes. Encrypted-prefs restore failures, DB downgrades and backup restores are all handled; the app always starts.

---

## ⚖️ License & credits

- Original project: [Hydra Launcher](https://github.com/hydralauncher/hydra) by Los Broxas (MIT).
- This port is an **unofficial community project**, not affiliated with the Hydra team.
- Game metadata, covers and descriptions belong to their respective owners (Steam, publishers).
- Torrents are a protocol: only download content you have the right to possess in your country.

---

<div align="center">

Made for phones. PRs and issues welcome.

</div>
