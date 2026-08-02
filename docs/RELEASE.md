# LoopingVid — Release & APK Build Guide

Panduan lengkap untuk membangun APK Android yang ditandatangani (signed) dan menerbitkan release secara otomatis melalui GitHub Actions — **tanpa Android Studio**.

---

## Arsitektur CI/CD

```
Push tag v1.0.0
     │
     ▼
┌─────────────────────────────────────────────────┐
│  GitHub Actions: .github/workflows/release.yml  │
├─────────────────────────────────────────────────┤
│  1. Validate tag format (vMAJOR.MINOR.PATCH)    │
│  2. Check release doesn't already exist         │
│  3. Setup Java 17 + Flutter stable              │
│  4. flutter pub get                             │
│  5. flutter analyze                             │
│  6. flutter test                                │
│  7. Decode keystore from ANDROID_KEYSTORE_BASE64│
│  8. Create key.properties from secrets          │
│  9. flutter build apk --release (signed)        │
│ 10. Rename → loopingvid-1.0.0.apk              │
│ 11. Clean up keystore & key.properties          │
│ 12. Upload artifact (7 days retention)          │
│ 13. Create GitHub Release + attach APK          │
└─────────────────────────────────────────────────┘
     │
     ▼
GitHub Release: v1.0.0 + loopingvid-1.0.0.apk
```

---

## 1. Membuat Android Keystore

Keystore digunakan untuk menandatangani APK secara digital. Tanpa ini, APK tidak bisa diinstall di device pengguna.

### Menggunakan keytool (sudah ada di JDK)

```bash
keytool -genkeypair \
  -v \
  -keystore loopingvid-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias loopingvid \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=LoopingVid, OU=Mobile, O=AIStudio, L=Jakarta, ST=DKI, C=ID"
```

**Catatan:**
- Ganti `YOUR_STORE_PASSWORD` dan `YOUR_KEY_PASSWORD` dengan password kuat
- File `loopingvid-release.jks` yang dihasilkan **JANGAN** di-commit ke repository
- Simpan backup keystore di tempat aman (jika hilang, tidak bisa update app di Play Store)

---

## 2. Mengubah Keystore Menjadi Base64

### Di Windows (PowerShell)

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("loopingvid-release.jks")) | Set-Clipboard
```

Hasilnya otomatis ter-copy ke clipboard.

### Di Windows (Command Prompt)

```cmd
certutil -encode loopingvid-release.jks encoded.txt
type encoded.txt
```

Lalu copy isi `encoded.txt` (hapus baris `-----BEGIN CERTIFICATE-----` dan `-----END CERTIFICATE-----`).

### Di macOS / Linux

```bash
base64 -i loopingvid-release.jks | pbcopy    # macOS (auto-copy)
base64 loopingvid-release.jks                 # Linux (print to terminal)
```

---

## 3. Memasukkan GitHub Secrets

1. Buka repository di GitHub
2. Klik **Settings** → **Secrets and variables** → **Actions**
3. Klik **New repository secret** untuk masing-masing:

| Secret Name | Value | Contoh |
|-------------|-------|--------|
| `ANDROID_KEYSTORE_BASE64` | Hasil encode Base64 dari file `.jks` | `MIIEvgIBADANBgkq...` (sangat panjang) |
| `ANDROID_KEY_ALIAS` | Alias key di keystore | `loopingvid` |
| `ANDROID_KEYSTORE_PASSWORD` | Password store | `MyStr0ngP4ss!` |
| `ANDROID_KEY_PASSWORD` | Password key | `MyK3yP4ss!` |

> **PENTING:** Jangan pernah mencetak atau membagikan nilai-nilai ini.

---

## 4. Membuat Tag Versi

Tag versi memicu workflow release secara otomatis.

### Format yang valid
```
v1.0.0
v1.2.3
v2.0.0
```

### Cara membuat tag

```bash
# Pastikan semua perubahan sudah di-commit dan push
git add .
git commit -m "release: v1.0.0"
git push origin main

# Buat tag
git tag v1.0.0
git push origin v1.0.0
```

### Atau melalui GitHub UI
1. Buka repository → **Releases** → **Draft a new release**
2. Di **Choose a tag**, ketik `v1.0.0` → **Create new tag: v1.0.0 on publish**
3. Klik **Publish release**

---

## 5. Menjalankan Workflow Manual

Jika ingin build APK tanpa membuat tag/release:

1. Buka repository → tab **Actions**
2. Pilih workflow **"Build & Release APK"**
3. Klik **"Run workflow"**
4. Masukkan versi (contoh: `v1.0.0`)
5. Klik **"Run workflow"**

> Workflow manual hanya menghasilkan artifact, TIDAK membuat GitHub Release.

---

## 6. Mengunduh APK dari GitHub Releases

### Dari GitHub Releases (recommended)
1. Buka repository → tab **Releases**
2. Klik release terbaru (contoh: `v1.0.0`)
3. Di bagian **Assets**, klik `loopingvid-1.0.0.apk`
4. Transfer ke device Android dan install

### Dari GitHub Actions Artifact
1. Buka repository → tab **Actions**
2. Klik workflow run yang berhasil (✅)
3. Scroll ke **Artifacts** → klik **loopingvid-release**
4. Extract ZIP → install APK

> **Catatan:** Artifact hanya disimpan selama 7 hari. GitHub Releases permanen.

---

## Troubleshooting

### "Tag does not match format vMAJOR.MINOR.PATCH"
- Pastikan format tag: `v1.0.0` (dengan huruf `v` kecil)
- Tidak boleh: `V1.0.0`, `1.0.0`, `v1.0`, `v1.0.0-beta`

### "Release already exists"
- Tag yang sama tidak bisa di-release dua kali
- Hapus release lama di GitHub UI jika ingin rebuild, atau gunakan versi baru

### Build gagal di "Decode keystore"
- Pastikan `ANDROID_KEYSTORE_BASE64` berisi Base64 yang valid
- Test decode manual: `echo "YOUR_BASE64" | base64 --decode > test.jks`

### APK tidak bisa diinstall
- Enable "Install from Unknown Sources" di Settings → Security
- Pastikan device mendukung arm64-v8a (semua HP 2018+)

---

## File yang Relevan

| File | Fungsi |
|------|--------|
| `.github/workflows/release.yml` | Workflow CI/CD untuk build + release |
| `.github/workflows/build-apk.yml` | Workflow CI untuk build biasa (tanpa release) |
| `flutter_app/android/app/build.gradle` | Konfigurasi signing & build Android |
| `flutter_app/android/app/proguard-rules.pro` | ProGuard rules untuk R8 shrinking |
| `flutter_app/pubspec.yaml` | Versi app & dependencies |

---

## Keamanan

- File keystore **TIDAK** pernah disimpan di repository
- Keystore hanya ada di memory CI selama build, lalu dihapus
- Password tidak pernah dicetak ke log (`secrets` masked oleh GitHub)
- `key.properties` di-generate on-the-fly dan dihapus setelah build
- `.gitignore` sudah meng-exclude `*.jks`, `*.keystore`, `key.properties`
