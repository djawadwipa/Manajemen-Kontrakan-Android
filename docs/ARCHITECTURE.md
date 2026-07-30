# Arsitektur

Aplikasi menggunakan satu modul Android untuk menjaga build v0.1.0 sederhana dan stabil, dengan pemisahan package berdasarkan tanggung jawab:

- `data/local`: Room entities, DAO, database.
- `data/repository`: single source of truth, transaksi atomik pembayaran, seed workbook.
- `di`: Hilt bindings.
- `ui`: Compose navigation, state, ViewModel, layar dan komponen.
- `util`: kriptografi backup, CSV, formatter.

UI hanya membaca `MainUiState` immutable. Semua operasi tulis berjalan pada `viewModelScope` dan repository. Pembayaran menggunakan transaksi Room agar baris pembayaran dan saldo tagihan konsisten.

## Aturan bisnis workbook

- Cadangan perbaikan default 15% dari penerimaan.
- Jatuh tempo default tanggal 10.
- Bulanan: tagihan setiap bulan.
- Triwulanan: Januari, April, Juli, Oktober.
- Tahunan: Januari.
- Potensi Januari seed 2026: Rp29.900.000.
- Potensi Februari seed 2026: Rp12.700.000.
