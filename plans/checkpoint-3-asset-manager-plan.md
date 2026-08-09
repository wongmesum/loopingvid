# Checkpoint 3: Media Asset Manager

## Ground truth (verified by reading code)

- Room DB: `core/database/AppDatabase.kt` — **version 4**, 6 entities, `addMigrations(MIGRATION_3_4)`,
  `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)`. Adding an entity **requires** a real
  `MIGRATION_4_5`, otherwise Room throws at runtime (no blanket destructive fallback for v4).
- Migration precedent: `core/database/Migrations.kt` holds `MIGRATION_3_4` (plain `execSQL`).
- Migration test precedent: `app/src/test/java/com/example/core/database/AppDatabaseMigrationTest.kt` drives the
  migration against a real SQLite file via `FrameworkSQLiteOpenHelperFactory` + Robolectric, **not** Room's
  `MigrationTestHelper` (exported schema JSON is a KSP output, not committed). Reuse this exact pattern.
- `core/database/LoopingVidRepository.kt` takes 3 required DAOs + 3 nullable ones. Constructing it in a unit test
  means faking 3 unrelated DAOs — bad for a focused test.
- `core/ui/SelectedMediaFile.kt` — `fromUri(context, uri)` resolves name/size/mime/isVideo;
  `takePersistablePermission(context)` **swallows** `SecurityException`, so its success cannot be trusted.
- Picker call sites are **not** centralised. `rememberMediaPickerHelper` (`core/ui/MediaPickerHelper.kt`) exists but
  feature screens call `rememberLauncherForActivityResult` directly:
  `LoopScreen.kt:102`, `EditorScreen.kt:171/177/183`, `MasteringScreen.kt:120`,
  `SlideshowScreen.kt:95`, `VisualizerStudioScreen.kt:51`, `LiveStreamingPage.kt:170`.
- Navigation: `NavDestination.kt` is a sealed class with `bottomNavItems` (4 fixed tabs) + `subPageRoutes`.
  New screens land in `subPageRoutes` and are reached from the overflow menu in `MainScreen.kt` / Studio launcher.
- ViewModels are hand-constructed in `MainActivity.onCreate` — no DI. New ViewModel follows that.

## Scope decision

One checkpoint, one vertical slice: **persist real asset usage, then surface it.**

In scope:
1. Room `assets` table (recent, favorite, pinned, usage count) + non-destructive `MIGRATION_4_5`.
2. `AssetRepository` — thin wrapper over `AssetDao` only, so the ViewModel is testable without faking
   unrelated DAOs. `LoopingVidRepository` stays untouched.
3. `AssetManagerViewModel` + `AssetLibraryScreen` (Recent / Favorit tabs, toggle favorite, pin, remove).
4. Recording real picks from the three primary media pickers: Loop, Editor, Mastering.
5. Tests: migration guard, DAO-level behaviour, ViewModel behaviour with a fake DAO.

Out of scope (stated, not silently dropped):
- Thumbnail generation/caching. Metadata cache satisfies the Phase 3 success criteria; thumbnails are a UX phase.
- Slideshow / Visualizer / Live picker recording (multi-URI and stream sources — different shape, later checkpoint).
- "Open in Editor/Loop" hand-off from the library. The library manages assets; feeding a selection back into a
  feature ViewModel needs nav-result plumbing, which is its own checkpoint.
- Cloud sync, search/FTS, bulk import.

## Data model

`core/database/AssetEntity.kt`:

```kotlin
@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["lastAccessedAt"]),
        Index(value = ["isFavorite"])
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val mediaType: String,          // "VIDEO" | "AUDIO"
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val usageCount: Int = 1,
    val lastAccessedAt: Long,
    val createdAt: Long,
    val permissionPersisted: Boolean = false,
    val isMissing: Boolean = false
)
```

`uriString` unique — one row per media file, re-picking increments `usageCount` instead of duplicating.

`permissionPersisted` is written from a **verified** read of `contentResolver.persistedUriPermissions`, not from the
return of `takePersistablePermission` (which hides failures). A `GetContent` URI usually cannot be persisted; that
row is honestly marked `false` and flagged in the UI as session-only rather than pretending it will reopen.

## Files, in dependency order

1. `core/database/AssetEntity.kt` — entity above.
2. `core/database/AssetDao.kt` — `observeRecent(limit)`, `observeFavorites()`, `getByUri(uriString)`,
   `insert(asset)`, `update(asset)`, `deleteById(id)`, `touch(uriString, timestamp)`.
3. `core/database/Migrations.kt` — append `MIGRATION_4_5`: `CREATE TABLE IF NOT EXISTS assets` + 3 `CREATE INDEX`.
   Column list and index names must match what Room generates for the entity, or Room's schema validation fails on
   open. Verify by opening the app once after the change (or by the identity-hash failure surfacing in the test).
4. `core/database/AppDatabase.kt` — add `AssetEntity::class`, bump `version = 5`,
   `addMigrations(MIGRATION_3_4, MIGRATION_4_5)`, `abstract fun assetDao(): AssetDao`.
5. `core/database/AssetRepository.kt` — wraps `AssetDao`:
   - `val recentAssets: Flow<List<AssetEntity>>`
   - `val favoriteAssets: Flow<List<AssetEntity>>`
   - `suspend fun recordAccess(file: SelectedMediaFile, permissionPersisted: Boolean)` — upsert by `uriString`:
     existing row → `usageCount + 1`, refreshed `lastAccessedAt`, refreshed metadata; new row → insert.
   - `suspend fun toggleFavorite(id: Long)`, `togglePinned(id: Long)`, `deleteAsset(id: Long)`.
6. `feature/assets/AssetManagerViewModel.kt` — `AssetManagerUiState(recentAssets, favoriteAssets, selectedTab)`,
   collects both flows, exposes the toggle/delete/record events. Holds no `Context`; the caller resolves
   permission state and passes a `Boolean`.
7. `feature/assets/AssetLibraryScreen.kt` — two tabs (Terbaru / Favorit), `LazyColumn` of asset rows with
   name, formatted size, media-type icon, usage count, favorite + pin + remove actions, session-only badge when
   `permissionPersisted == false`, and an empty state. Indonesian labels, `testTag` on every action.
8. `ui/navigation/NavDestination.kt` — `object AssetLibrary : NavDestination("asset_library", "Pustaka Media", …)`,
   added to `subPageRoutes`.
9. `ui/navigation/AppNavHost.kt` — route composable, `assetManagerViewModel: AssetManagerViewModel? = null`.
10. `ui/navigation/MainScreen.kt` — thread the ViewModel through, add an overflow menu item "Pustaka Media".
11. `MainActivity.kt` — `AssetRepository(db.assetDao())` + `AssetManagerViewModel(assetRepository)`, passed to
    `MainScreen`.
12. Picker integration — `LoopScreen.kt:102`, `EditorScreen.kt:171/177/183`, `MasteringScreen.kt:120`.
    Each launcher callback, after its existing `viewModel.on…Selected(...)` call, also does:
    `SelectedMediaFile.fromUri` → `takePersistablePermission` → verify against `persistedUriPermissions` →
    `assetManagerViewModel?.recordAccess(file, persisted)`. Nullable param, default null, so existing call sites
    and tests keep compiling.

## Tests

- `AppDatabaseMigrationTest` — extend with `migration 4 to 5 creates an empty assets table`, and
  `assets table accepts a row with the columns the entity declares`. Seed v4, run `MIGRATION_3_4` then
  `MIGRATION_4_5`, assert pre-existing `render_jobs` / `projects` rows survive.
- New `AssetRepositoryTest` with an in-memory fake `AssetDao`:
  - first `recordAccess` inserts with `usageCount == 1`
  - second `recordAccess` on the same URI increments to 2 and does **not** insert a second row
  - `recordAccess` refreshes `fileName` when the file was renamed
  - `toggleFavorite` flips and persists
- New `AssetManagerViewModelTest`: recent/favorite flows reach `uiState`; toggle delegates to the repository.
- `SimulationGuardTest` — no new guard needed; the asset list is fed only by real picks. If an empty-state
  placeholder list is ever added, a guard must come with it.

## Verification (all three must be green before commit)

1. `./gradlew.bat :app:compileDebugKotlin --console=plain`
2. `./gradlew.bat :app:testDebugUnitTest --console=plain`
3. `./gradlew.bat :app:assembleDebug --console=plain`

Then: update `plans/post-rc-audit-corrections.md` (Checkpoint 3 → `[SELESAI]` with sub-bullets),
commit `feat(assets): add media asset manager with recent and favorites`,
`git push origin release/1.1.0-stabilization` (branch only — no tags, no merge to `main`).

## Known risk

Room schema validation is exact. If `MIGRATION_4_5`'s SQL differs from the entity by so much as a column order in
the index or a missing `NOT NULL`, Room fails on database open with an identity-hash mismatch — and that failure
appears at **runtime**, not compile time. The migration test is what catches it before a device does.
