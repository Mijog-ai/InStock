# WMS Grid Locator — Android Tablet App (Kotlin / Jetpack Compose)

## Overview

Logistics floor operations app designed for landscape tablets (1280x800). Supports login, zone selection, interactive shelf grid, book-in, consume, and book-out operations.

## Setup

1. Open `tablet_android/` in Android Studio (Hedgehog 2023.1+ or newer)
2. Sync Gradle
3. Run on an Android tablet or emulator (API 26+, landscape orientation)

The app auto-seeds sample data on first launch (same users as the web app).

## Default Credentials

| Username   | Password | Role      |
|------------|----------|-----------|
| admin      | admin123 | Admin     |
| logistics1 | log123   | Logistics |
| logistics2 | log123   | Logistics |
| consumer1  | con123   | Consumer  |

## Architecture

```
app/src/main/
├── assets/
│   └── grid_config.json          # Grid structure (same as web app)
├── java/com/wms/gridlocator/
│   ├── data/
│   │   ├── Entities.kt           # Room entities: User, Booking, Movement
│   │   ├── WmsDao.kt             # Room DAO
│   │   ├── WmsDatabase.kt        # Room database singleton
│   │   ├── Repository.kt         # Repository abstraction layer
│   │   ├── GridConfig.kt         # Config model + loader
│   │   └── SeedDatabase.kt       # Sample data seeder
│   ├── viewmodel/
│   │   └── WmsViewModel.kt       # MVVM ViewModel
│   ├── ui/
│   │   ├── theme/Theme.kt        # Material 3 theme (matching design system)
│   │   └── screens/
│   │       ├── LoginScreen.kt
│   │       ├── ZoneSelectorScreen.kt
│   │       ├── ShelfGridScreen.kt
│   │       ├── BookInDialog.kt
│   │       └── CellDetailsDialog.kt
│   ├── MainActivity.kt           # Entry point + navigation
│   └── WmsApplication.kt
```

## Configuration

The grid is loaded from `assets/grid_config.json` — same format as the web app. Modify shelves, sections, rows per shelf, and max_slots_per_cell there.

## Replacing Local DB with Remote SQL Server

All DB access goes through `Repository.kt` which wraps `WmsDao`. To switch to a remote API:

1. Create a new `RemoteRepository` implementing the same function signatures
2. Replace the `Repository` injection in `WmsViewModel`
3. The ViewModel and all UI code remain unchanged

The MVVM architecture keeps the data layer fully decoupled from UI.
