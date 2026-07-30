# Panduan Release

1. Buat private keystore pada workstation tepercaya.
2. Simpan backup keystore dan kredensial di lokasi offline yang aman.
3. Tambahkan empat GitHub Actions Secrets sesuai `README.md`.
4. Pastikan branch utama lulus workflow **Android CI**.
5. Jalankan workflow **Signed Android Release** secara manual.
6. Masukkan `version_name` semantik, misalnya `1.0.0`, dan `version_code` integer yang selalu meningkat.
7. Workflow memvalidasi keystore/alias, menjalankan test dan `lintRelease`, membangun serta menandatangani APK/AAB, lalu memverifikasi signature keduanya.
8. Unduh APK, AAB, signature reports, mapping, lint report, dan `SHA256SUMS.txt`.
9. Cocokkan checksum sebelum distribusi.
10. Unggah AAB ke Play Internal/Closed Testing atau APK ke situs HTTPS.

Keystore tidak disimpan sebagai artefak dan dihapus dari runner pada langkah `always()`.
