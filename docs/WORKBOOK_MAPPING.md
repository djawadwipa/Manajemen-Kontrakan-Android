# Pemetaan Workbook ke Aplikasi

| Workbook | Implementasi Android |
|---|---|
| PENGATURAN | `AppSettingEntity`, layar Pengaturan |
| MASTER_UNIT | `RentalUnitEntity`, layar Unit |
| MASTER_KATEGORI | `ExpenseCategoryEntity` |
| TAGIHAN | `InvoiceEntity`, generator siklus |
| PEMBAYARAN | `PaymentEntity`, cicilan transaksional |
| PENGELUARAN | `ExpenseEntity` |
| DANA_PERBAIKAN | KPI cadangan 15% dikurangi biaya perbaikan |
| PIUTANG | sisa tagihan dan status jatuh tempo |
| REKAP_BULANAN | `monthlySummary` |
| ARUS_KAS | saldo awal + penerimaan - pengeluaran |
| LABA_RUGI | penerimaan - biaya usaha - alokasi cadangan |
| KINERJA_UNIT | dapat diturunkan dari tagihan, pembayaran, pengeluaran |
| EXPORT_LAPORAN | ekspor CSV melalui Storage Access Framework |
