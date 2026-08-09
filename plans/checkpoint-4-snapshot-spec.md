# Spec: Project Snapshot dan Recovery

## 1. One-liner
Membangun sistem versioning snapshot proyek yang memungkinkan pengguna menyimpan, melihat riwayat (diff), dan memulihkan status proyek pada titik waktu tertentu.

## 2. User Story
As a content creator, I want to save snapshots of my video project at different stages, so that I can experiment with edits and safely restore previous versions if I make a mistake.

## 3. Acceptance Criteria
- [ ] Database memiliki tabel `project_snapshots` yang berelasi dengan `projects`.
- [ ] User dapat membuat snapshot baru dari state editor saat ini.
- [ ] `ProjectManagerScreen` memiliki UI (bottom sheet atau dialog) untuk melihat daftar snapshot dari sebuah proyek.
- [ ] Daftar snapshot menampilkan timestamp dan ukuran/perubahan ringkas.
- [ ] User dapat menekan "Restore" pada sebuah snapshot yang akan menimpa draft aktif.
- [ ] Proses pemulihan menimpa `configJson` proyek ke state yang disimpan di snapshot.

## 4. Phased Delivery
- **Phase A (Data Layer)**: Buat `ProjectSnapshotEntity`, `ProjectSnapshotDao`, tambah relasi ke `AppDatabase`, dan tulis migration test. **Done-condition**: DAO tests green.
- **Phase B (Repository & Domain)**: Integrasikan logic `ProjectRepository` untuk save dan load snapshot JSON, terjemahkan JSON ke/dari `EditorUiState` atau `LoopUiState`. **Done-condition**: Repository tests green.
- **Phase C (UI Integration)**: Tambah tombol "Save Snapshot" di Editor/Mastering/Loop dan menu "History/Snapshots" di `ProjectManagerScreen`. **Done-condition**: UI bisa menampilkan dan merestore state di memori.

## 5. Edge Cases / Error States
- User restore snapshot ke media source yang filenya sudah dihapus dari device -> berikan error peringatan, tetap load parameter lain.
- Proyek dihapus -> cascade delete semua snapshot-nya (lewat ForeignKey ON DELETE CASCADE).
- Ruang penyimpanan limit -> batasi max 10 snapshot per proyek.

## 6. Out of Scope
- Branching tree kompleks (git-like graph). Hanya linear history.
- Auto-snapshot (tetap andalkan auto-save draft 1 slot yang ada, snapshot multi-versi dilakukan explicit oleh user atau sebelum render saja).
- Export/import snapshot sebagai file zip terpisah.

## 7. Dependencies
- Room database terpasang (AppDatabase v5).
- UI Jetpack Compose.

## 8. File Impact Estimate
- **Baru**: `ProjectSnapshotEntity.kt`, `ProjectSnapshotDao.kt`, `ProjectSnapshotRepository.kt`, `SnapshotHistoryDialog.kt`.
- **Modifikasi**: `AppDatabase.kt` (v6), `Migrations.kt` (MIGRATION_5_6), `ProjectManagerViewModel.kt`, `ProjectManagerScreen.kt`.
