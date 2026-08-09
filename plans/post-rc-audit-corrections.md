# Post-RC Audit Corrections

## Status Jujur
- **Asset Manager:** selesai lokal (Room asset cache, UI pustaka, picker recording), belum device test.
- **Project Snapshot:** selesai lokal (Room v6, repository, UI history), belum device test.
- **Unified Render Engine:** sebagian.
- **Audio Analysis:** sumber data nyata sudah terpasang (mastering + trimmer), belum device test.
- **Visualizer:** belum device test.
- **Slideshow:** belum device test.
- **ARM64:** belum didukung.
- **Coverage:** rendah.
- **Release signing:** belum selesai.
- **Release Candidate:** belum layak publish.

## Checkpoint Selesai
- Checkpoint 1: Hapus data simulasi dari production path. [SELESAI]
- Checkpoint 2: Integrasikan Unified Audio Analysis. [SELESAI]
  - `core/media/AudioAnalysisMapper.kt` baru: pemilik tunggal `AudioAnalysisData` + `AudioAnalysisResult.toAudioAnalysisData()`.
  - `core/media/WaveformAnalyzer.kt` dihapus beserta `generateSimulatedWaveform()`.
  - `MasteringViewModel` dan `AudioMasteringViewModel` membaca `AudioAnalysisRepository.getOrAnalyze()`, dengan guard hasil basi saat URI berganti.
  - `Media3SegmentTrimmer` menerima repository lewat parameter (di-thread dari `MainActivity` → `MainScreen` → `AppNavHost` → `EditorScreen`); beat-snap akhirnya punya data nyata.
  - Beat grid diturunkan dari BPM terukur (`60000/bpm`), bukan onset palsu; ditolak bila BPM non-finite, di luar 20-300, atau confidence < 0.5.
  - Canvas waveform dan beat marker diberi guard divide-by-zero; kegagalan analisis dicatat lewat `Timber.e`.
  - Test: `AudioAnalysisMapperTest` (9 test) plus 2 guard baru di `SimulationGuardTest`.
  - Verifikasi: `:app:compileDebugKotlin` exit 0, `:app:testDebugUnitTest` exit 0, `:app:assembleDebug` exit 0 (`app-debug.apk` terbentuk).

## Checkpoints Tertunda
- Checkpoint 3: Bangun Media Asset Manager. [SELESAI]
  - `core/database/AssetEntity.kt` + `AssetDao.kt`: Room entity & DAO, unique URI, usage count, favorites/pin.
  - `core/database/Migrations.kt`: `MIGRATION_4_5` — CREATE TABLE + 3 indexes, verified against Room schema v5 JSON.
  - `core/database/AppDatabase.kt`: bumped v5, registered entity + DAO + migration.
  - `core/database/AssetRepository.kt`: upsert by URI, toggle favorite/pin, mark missing, delete.
  - `feature/assets/AssetManagerViewModel.kt`: collects recent/favorite flows, exposes UI actions.
  - `feature/assets/AssetLibraryScreen.kt`: tabs Terbaru/Favorit, session badge, action icons.
  - `feature/assets/AssetRecorder.kt`: picker-site helper — resolves metadata, verifies persisted permission.
  - `core/ui/SelectedMediaFile.kt`: added `hasPersistedReadPermission(context)`.
  - Navigation: `NavDestination.AssetLibrary`, overflow menu item, `AppNavHost` route.
  - Picker integration: Loop, Editor (video/audio/overlay), Mastering all record picks.
  - Tests: 2 migration tests, 7 repository tests, 3 ViewModel tests — all green.
  - Verification: compileDebugKotlin ✓, testDebugUnitTest ✓, assembleDebug ✓ (APK 53MB).
- Checkpoint 4: Project Snapshot dan Recovery. [SELESAI]
  - `ProjectSnapshotEntity.kt`, `ProjectSnapshotDao.kt`, `ProjectSnapshotRepository.kt` mengatur penyimpanan & max 10 snapshot.
  - `MIGRATION_5_6` selesai (v6), FK CASCADE tervalidasi dengan export KSP (hash d3a44187ef0bf12b7b1bf565abac800c).
  - `ProjectManagerViewModel.kt` diextend dengan state snapshot (backward-compatible repo nullable).
  - `ProjectManagerScreen.kt` dan `SnapshotHistoryDialog.kt` melayani UI interaksi: buat, restore, hapus snapshot.
  - Test: 2 migration tests, 8 repository tests — semua hijau.
  - Verifikasi: `compileDebugKotlin` ✅, `ProjectSnapshotRepositoryTest` ✅ (exit 0), `testDebugUnitTest` ✅ (1m 36s), `assembleDebug` ✅ (2m 7s).
- Checkpoint 5: Selesaikan Unified Render Queue. [SELESAI]
  - `VideoExportWorker.kt` mengenali `VISUALIZER` dan `SLIDESHOW`, membaca config spesifiknya, dan koleksi progress dipindah ke processor yang benar.
  - `ExportQueueViewModel.kt` disesuaikan untuk JSON config di dalam WorkManager `Data` dan status `BLOCKED` diubah mapping-nya ke `QUEUED` (bukan `CANCELLED`).
  - `RenderRequestSerializer.kt` menggunakan `org.json` murni untuk round-trip objek yang punya variant `VisualizerBackground`.
  - `SlideshowViewModel.kt` dan `VisualizerViewModel.kt` dialihkan ke model enqueue, progress dibaca dari state antrean.
  - Test dependency `org.json:json:20240303` dipasang agar `RenderRequestSerializerTest` tidak bentrok dengan stub JVM framework Android.
  - UI `ExportQueueCard.kt` menampilkan lencana badge job tipe baru.
  - Verifikasi: `testDebugUnitTest` (RenderRequestSerializerTest) ✅ (exit 0), full `testDebugUnitTest` ✅ (1m 48s), `compileDebugKotlin` ✅, `assembleDebug` ✅ (8s).
- Checkpoint 6: Verifikasi ARM64.
- Checkpoint 7: Device Test dan Release Gate.
