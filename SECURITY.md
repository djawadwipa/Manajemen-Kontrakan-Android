# Security Policy

## Supported versions

Versi terbaru pada branch `main` mendapat pembaruan keamanan.

## Pelaporan

Laporkan kerentanan secara privat kepada pemilik repository. Jangan memasukkan data penyewa, file database, keystore, password, atau secret ke issue publik.

## Kontrol keamanan

- Tidak ada secret di source code.
- Private signing key hanya melalui GitHub Actions Secrets.
- Release `debuggable=false`, R8 dan resource shrinking aktif.
- Cleartext network ditolak dan tidak ada permission INTERNET.
- Manual backup memakai AES-256-GCM dan PBKDF2-HMAC-SHA256 (210.000 iterasi).
- Integritas ciphertext diverifikasi dengan SHA-256 sebelum dekripsi.
- Cloud backup Android dinonaktifkan.
- Storage Access Framework digunakan tanpa `MANAGE_EXTERNAL_STORAGE`.
