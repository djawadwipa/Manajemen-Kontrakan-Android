package id.djawadwipa.manajemenkontrakan.data.repository

import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseCategoryEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToLong

object SeedData {
    val settings = AppSettingEntity()

    val units = listOf(
        unit("KTR-001", "Penyewa 01", "Kontrakan 01", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-002", "Penyewa 02", "Kontrakan 02", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-003", "Penyewa 03", "Kontrakan 03", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-004", "Penyewa 04", "Kontrakan 04", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-005", "Penyewa 05", "Kontrakan 05", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-006", "Penyewa 06", "Kontrakan 06", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-007", "Penyewa 07", "Kontrakan 07", "Bulanan", 1_200_000, 1, "Aktif"),
        unit("KTR-008", "Penyewa 08", "Kontrakan 08", "Bulanan", 1_200_000, 1, "Aktif"),
        unit("KTR-009", "Penyewa 09", "Kontrakan 09", "Bulanan", 1_200_000, 1, "Aktif"),
        unit("KTR-010", "Penyewa 10", "Kontrakan 10", "Bulanan", 1_200_000, 1, "Aktif"),
        unit("KTR-011", "Penyewa 11", "Kontrakan 11", "Bulanan", 800_000, 1, "Aktif"),
        unit("KTR-012", "Penyewa 12", "Kontrakan 12", "Bulanan", 800_000, 1, "Aktif"),
        unit("KTR-013", "Penyewa 13", "Kontrakan 13", "Bulanan", 900_000, 1, "Aktif"),
        unit("KTR-014", "Belum ada penghuni", "Kontrakan X", "Bulanan", 0, 1, "Kosong", "Tarif belum ditetapkan"),
        unit("KTR-015", "Penyewa Tahunan", "Unit Tahunan", "Tahunan", 14_000_000, 12, "Aktif", "Dibayar setahun sekali"),
        unit("KTR-016", "Penyewa Triwulanan", "Unit Triwulanan", "Triwulanan", 3_200_000, 3, "Aktif", "Dibayar tiap 3 bulan"),
    )

    val categories = listOf(
        category("Perbaikan & Renovasi", "Usaha", "Ya"),
        category("Utilitas", "Campuran", "Perlu alokasi"),
        category("Jasa Pemeliharaan", "Usaha", "Ya"),
        category("Administrasi & Pajak", "Campuran", "Perlu review"),
        category("Gaji & Tenaga Kerja", "Campuran", "Perlu alokasi"),
        category("Peralatan Rumah", "Campuran", "Perlu review"),
        category("Kebutuhan Rumah Tangga", "Pribadi", "Tidak"),
        category("Kesehatan & Perawatan", "Pribadi", "Tidak"),
        category("Transfer & Dukungan Keluarga", "Pribadi", "Tidak"),
        category("Sosial & Keagamaan", "Pribadi", "Tidak"),
        category("Transportasi", "Campuran", "Perlu review"),
        category("Konsumsi", "Pribadi", "Tidak"),
        category("Lain-lain", "Perlu Review", "Perlu review"),
    )

    fun invoices(year: Int, units: List<RentalUnitEntity>): List<InvoiceEntity> = buildList {
        units.filter { it.status == "Aktif" && it.rate > 0 }.forEach { unit ->
            for (month in 1..12) {
                if ((month - 1) % unit.intervalMonths != 0) continue
                val ym = YearMonth.of(year, month)
                val dueDay = unit.dueDay.coerceIn(1, ym.lengthOfMonth())
                val invoiceDate = LocalDate.of(year, month, 1)
                val dueDate = LocalDate.of(year, month, dueDay)
                val period = String.format(Locale.ROOT, "%04d-%02d", year, month)
                add(
                    InvoiceEntity(
                        id = "INV-$year${month.toString().padStart(2, '0')}-${unit.code}",
                        unitId = unit.id,
                        tenantName = unit.tenantName,
                        period = period,
                        invoiceDate = invoiceDate.toEpochDay(),
                        dueDate = dueDate.toEpochDay(),
                        amount = unit.rate,
                        reserveTarget = (unit.rate * unit.reservePercent).roundToLong(),
                    ),
                )
            }
        }
    }

    private fun unit(code: String, tenant: String, name: String, frequency: String, rate: Long, interval: Int, status: String, notes: String = "") =
        RentalUnitEntity(code, code, name, tenant, frequency, rate, interval, 0.15, status, 10, notes)

    private fun category(name: String, group: String, rule: String) =
        ExpenseCategoryEntity(name.lowercase().replace(Regex("[^a-z0-9]+"), "-"), name, group, rule)
}
