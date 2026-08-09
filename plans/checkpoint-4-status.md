# Checkpoint 4 — Status (SELESAI)

Status: **SELESAI — 4 dari 4 gate hijau**

Implementasi lengkap meliputi:

## Data Layer (Room v6)
- `ProjectSnapshotEntity.kt`: tabel `project_snapshots`, FK ke `projects` (`ON UPDATE NO ACTION ON DELETE CASCADE`), index `projectId`.
- `ProjectSnapshotDao.kt`: query list, get, insert, delete, count, dan `trimToLimit` (ORDER BY createdAt DESC, id DESC).
- `Migrations.kt`: `MIGRATION_5_6` sesuai KSP JSON (termasuk `ON UPDATE NO ACTION`).
- `AppDatabase.kt`: versi 6.

## Repository & Tests
- `ProjectSnapshotRepository.kt`: cap 10 snapshot (`MAX_SNAPSHOTS_PER_PROJECT = 10`), nullable DAO.
- `AppDatabaseMigrationTest.kt`: 2 test memvalidasi struktur tabel v5 ke v6.
- `ProjectSnapshotRepositoryTest.kt`: 8 test covering create, trim, restore, validasi FK CASCADE, dsb. (GREEN).

## UI & Presentation
- `ProjectManagerViewModel.kt`: penambahan state, `createSnapshot`, `restoreSnapshot`, `deleteSnapshot`, nullable param untuk backward-compatibility test.
- `ProjectManagerScreen.kt`: tombol icon baru untuk tiap project card memanggil riwayat snapshot.
- `SnapshotHistoryDialog.kt`: dialog compose lengkap dalam Bahasa Indonesia dengan fungsi hapus dan restore.

## Eksekusi Verifikasi
- `compileDebugKotlin`: ✅ hijau
- `ProjectSnapshotRepositoryTest`: ✅ hijau (exit 0)
- `testDebugUnitTest` (full suite): ✅ hijau (1m 36s)
- `assembleDebug`: ✅ hijau (`BUILD SUCCESSFUL in 2m 7s`).

## Pekerjaan tertunda

Commit file Checkpoint 4 (selektif, kecualikan `run-checkpoint1.bat`), lalu
`git push origin release/1.1.0-stabilization` (branch saja, tanpa tag).