# Scripts

## archive/

Folder ini berisi script-script lama (Python, Shell, Kotlin) yang pernah digunakan
selama proses migrasi dan perbaikan awal repository. **Bukan bagian dari build utama.**

File-file ini dipertahankan sebagai referensi historis:
- `patch_*.py` / `patch_*.sh` — script patch manual untuk memperbaiki file sumber saat CI belum stabil.
- `fix_*.py` / `fix_*.sh` — script otomasi perbaikan escape, braces, dan argumen.
- `test_script.kt` / `test.kt` — snippet Kotlin eksperimen, bukan test suite resmi.
- `insert.py` — helper insert konten ke file Kotlin.

Jangan jalankan script ini pada kode saat ini. Mereka menargetkan state kode lama
yang sudah tidak relevan.
