# Manajemen Kontrakan

Aplikasi **Android native murni** untuk pengelolaan unit kontrakan, penyewa, tagihan, cicilan, pengeluaran, piutang, laporan, impor/ekspor, serta backup lokal terenkripsi.

## Status sumber

Versi ini adalah **v0.1.0 Foundation + Core Business**, disusun berdasarkan aturan bisnis workbook `Sistem Manajemen Kontrakan Profesional 2026`. Data nama penyewa pada seed telah dianonimkan; workbook asli tidak disertakan ke repository.

Fitur yang tersedia:

- Dashboard KPI per bulan dan grafik tahunan.
- CRUD unit dan penyewa.
- Tagihan otomatis bulanan, triwulanan, semester, dan tahunan.
- Pembayaran penuh maupun cicilan.
- Pengeluaran usaha, campuran, dan pribadi per unit atau umum.
- Piutang dan status keterlambatan.
- Rekap bulanan dan ekspor laporan CSV.
- Impor unit melalui CSV dengan template.
- Backup/restore penuh `.mkbackup` dengan AES-256-GCM, PBKDF2-HMAC-SHA256, dan checksum SHA-256.
- GitHub Actions untuk pemeriksaan native-only, test, lint release, dependency review, APK, AAB, private signing, verifikasi tanda tangan, dan checksum.

## Teknologi

- Kotlin + Jetpack Compose + Material 3
- MVVM, immutable UI state, StateFlow, Room, Hilt
- Package ID permanen: `id.djawadwipa.manajemenkontrakan`
- Min SDK 26, Target/Compile SDK 36
- Gradle 8.13 dan JDK 17
- Tanpa WebView, React, JavaScript web, PWA, Firebase, akun, atau cloud
- Tidak meminta permission Android, termasuk permission internet dan penyimpanan luas

## Build lokal

1. Gunakan Android Studio/JDK 17.
2. Pasang Android SDK Platform 36.
3. Gunakan Gradle 8.13, lalu jalankan:

```bash
gradle --no-daemon test lintRelease assembleDebug
```

CI menginstal Gradle 8.13 secara terpisah melalui `gradle/actions/setup-gradle`; karena itu repository tidak bergantung pada Gradle yang terpasang di runner.

Build release lokal tanpa signing dapat dibuat dengan:

```bash
gradle --no-daemon assembleRelease bundleRelease
```

## Signed release melalui GitHub Actions

Buat private release keystore pada komputer tepercaya dan **jangan pernah commit** ke repository. Isi GitHub Actions Secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Contoh encoding Linux:

```bash
base64 -w 0 release.keystore
```

Pada macOS:

```bash
base64 < release.keystore | tr -d '\n'
```

Jalankan workflow **Signed Android Release**, lalu isi `version_name` dan `version_code`. Artefak berisi signed APK, signed AAB, `mapping.txt`, laporan lint, laporan verifikasi signature APK/AAB, dan `SHA256SUMS.txt`.

## Keamanan data

Aplikasi tidak memiliki izin internet dan tidak mengirim data ke luar perangkat. Android cloud backup dinonaktifkan. Impor/ekspor menggunakan Storage Access Framework tanpa `MANAGE_EXTERNAL_STORAGE` maupun izin penyimpanan lama.

Baca [Privacy Policy](PRIVACY_POLICY.md), [Security Policy](SECURITY.md), [Arsitektur](docs/ARCHITECTURE.md), [Pemetaan Workbook](docs/WORKBOOK_MAPPING.md), dan [Release Guide](docs/RELEASE_GUIDE.md).
