# Checkpoint 5: Selesaikan Unified Render Queue

## One-liner

Satukan semua jalur render (Visualizer, Slideshow) ke dalam WorkManager queue yang sudah ada, sehingga semua export terlihat di `ExportQueueCard` dan berjalan sekuensial.

## Konteks

Saat ini:
- `VideoExportWorker` hanya mendukung 3 jobType: `EDITOR`, `LOOP`, `MASTERING`.
- `VisualizerViewModel` dan `SlideshowViewModel` memanggil processor langsung di ViewModel scope — tidak lewat WorkManager, tidak tampil di queue card.
- `ExportQueueCard` UI sudah punya badge untuk `BATCH_COLOR_GRADING`, `SPEED_RETIME`, `TRANSITION_RENDER` (ditulis sebagai badge static), tapi Visualizer/Slideshow belum ada.
- Kedua processor sudah menyimpan `RenderJobEntity` ke Room (history), tapi tidak terkoordinasi dengan WorkManager queue.

Target: semua render lewat `ExportQueueViewModel.enqueueProjectExport()` → `VideoExportWorker` → processor → Room history. Progress tampil di `ExportQueueCard`. ViewModel screen bisa observe status job spesifiknya untuk inline feedback.

## Acceptance Criteria

1. ✅ `VideoExportWorker.doWork()` menangani jobType `"VISUALIZER"` dan `"SLIDESHOW"`.
2. ✅ `BatchExportRequest` punya field `visualizerConfigJson` dan `slideshowConfigJson` untuk menyimpan request complex sebagai JSON string di WorkManager Data.
3. ✅ `SlideshowViewModel.renderSlideshow()` mengenqueue lewat `ExportQueueViewModel` bukan panggil processor langsung.
4. ✅ `VisualizerViewModel.exportVisualizer()` mengenqueue lewat `ExportQueueViewModel` bukan panggil processor langsung.
5. ✅ `ExportQueueCard` menampilkan badge `VISUALIZER` dan `SLIDESHOW` untuk job type baru.
6. ✅ Progress inline tetap terlihat di screen masing-masing melalui observasi WorkManager state untuk job ID tertentu.
7. ✅ Cancel dari screen masing-masing memanggil `exportQueueViewModel.cancelExportJob(workId)`.
8. ✅ Unit test: serialization round-trip request → JSON → request untuk kedua tipe.
9. ✅ Gate: `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug` semua hijau.

## Phased Delivery

### Phase A: Worker Extension + Serialization
- Tambah `KEY_VISUALIZER_CONFIG` dan `KEY_SLIDESHOW_CONFIG` di `VideoExportWorker`.
- Buat `RenderRequestSerializer` object dengan `toJson()`/`fromJson()` untuk kedua request type (pakai `org.json.JSONObject`/`JSONArray` bawaan Android, tanpa library baru).
- Extend `VideoExportWorker.doWork()` branch untuk `"VISUALIZER"` → deserialize config → `VisualizerProcessor.renderVisualizer()` dan `"SLIDESHOW"` → `SlideshowProcessor.renderSlideshow()`.
- Extend `BatchExportRequest` dan `buildDataFromRequest()`.

### Phase B: ViewModel Rewiring
- Inject `ExportQueueViewModel` ke `SlideshowViewModel` dan `VisualizerViewModel`.
- Ganti panggilan langsung processor dengan `enqueueProjectExport(request)`.
- Simpan `UUID` job terakhir di ViewModel state.
- Observe WorkManager LiveData/Flow untuk job tersebut → map ke `jobProgress` yang sudah ada di UI state.
- Cancel: delegate ke `exportQueueViewModel.cancelExportJob(lastJobId)`.

### Phase C: UI Badge + Test
- Tambah badge `VISUALIZER` dan `SLIDESHOW` di `ExportQueueCard.QueueItemRow`.
- Tulis `RenderRequestSerializerTest` (round-trip untuk kedua tipe).
- Jalankan 3 gate gradle.

## Edge Cases
- Visualizer config punya `Color` (Int ARGB) dan sealed class `VisualizerBackground` — serialisasi harus handle semua variant.
- `SlideshowRenderRequest.imageUris` bisa kosong (sudah divalidasi di ViewModel); serialisasi harus tetap aman.
- Job yang sudah berjalan di WorkManager saat app dibunuh: WorkManager recover otomatis, progress resume.

## Out of Scope
- Batch render multiple visualizer/slideshow sekaligus (batch sudah ada di queue, 1 job = 1 render).
- Migrasi history lama dari `render_jobs` ke format baru.
- Perubahan di `flutter_app/`.
- `BATCH_COLOR_GRADING`, `SPEED_RETIME`, `TRANSITION_RENDER` — belum ada processor nyata, badge UI dibiarkan.

## File Impact

New:
- `app/src/main/java/com/example/core/work/RenderRequestSerializer.kt`
- `app/src/test/java/com/example/core/work/RenderRequestSerializerTest.kt`

Modified:
- `app/src/main/java/com/example/core/work/VideoExportWorker.kt`
- `app/src/main/java/com/example/core/work/ExportQueueViewModel.kt`
- `app/src/main/java/com/example/core/ui/ExportQueueCard.kt`
- `app/src/main/java/com/example/feature/slideshow/SlideshowViewModel.kt`
- `app/src/main/java/com/example/feature/visualizer/VisualizerViewModel.kt`
- `app/src/main/java/com/example/MainActivity.kt`

## Verification

```bash
./gradlew.bat :app:compileDebugKotlin --console=plain
./gradlew.bat :app:testDebugUnitTest --console=plain
./gradlew.bat :app:assembleDebug --console=plain
```

## Hasil Verifikasi (2026-08-09)

- `testDebugUnitTest --tests "com.example.core.work.RenderRequestSerializerTest"` → BUILD SUCCESSFUL, exit 0.
- `testDebugUnitTest` (full suite) → BUILD SUCCESSFUL in 1m 48s, exit 0.
- `compileDebugKotlin` → UP-TO-DATE (bagian dari langkah di atas), tidak ada error compile baru.
- `assembleDebug` → BUILD SUCCESSFUL, UP-TO-DATE, exit 0.
- Root cause blocker sebelumnya: JVM unit test memuat stub Android `org.json` (`RuntimeException("Stub!")`) alih-alih implementasi nyata. Diselesaikan dengan menambahkan `testImplementation("org.json:json:20240303")` di `app/build.gradle.kts`.

Status: **SELESAI.**
