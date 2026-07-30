package id.djawadwipa.manajemenkontrakan.util

import id.djawadwipa.manajemenkontrakan.data.local.ExpenseEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import java.time.LocalDate

object CsvExporter {
    fun report(
        units: List<RentalUnitEntity>,
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
        expenses: List<ExpenseEntity>,
    ): String = buildString {
        appendLine("sep=,")
        appendLine("LAPORAN MANAJEMEN KONTRAKAN")
        appendLine("Dibuat,${LocalDate.now()}")
        appendLine()
        val activeUnitCount = units.count { it.status == "Aktif" }
        val emptyUnitCount = units.count { it.status == "Kosong" }
        appendLine("RINGKASAN")
        appendLine("Total Unit,${units.size}")
        appendLine("Unit Aktif,$activeUnitCount")
        appendLine("Unit Kosong,$emptyUnitCount")
        appendLine("Total Tagihan,${invoices.sumOf { it.amount }}")
        appendLine("Total Diterima,${payments.sumOf { it.amount }}")
        appendLine("Total Piutang,${invoices.sumOf { (it.amount - it.paid).coerceAtLeast(0) }}")
        appendLine("Total Pengeluaran,${expenses.sumOf { it.amount }}")
        appendLine()
        appendLine("TAGIHAN")
        appendLine("ID,Periode,Unit,Penyewa,Tagihan,Dibayar,Sisa,Status")
        invoices.forEach { i -> appendLine(listOf(i.id, i.period, i.unitId, i.tenantName, i.amount, i.paid, (i.amount-i.paid).coerceAtLeast(0), i.status).joinToString(",") { escape(it.toString()) }) }
        appendLine()
        appendLine("PEMBAYARAN")
        appendLine("ID,Tanggal,Tagihan,Unit,Penyewa,Periode,Nominal,Metode,Bukti")
        payments.forEach { p -> appendLine(listOf(p.id, LocalDate.ofEpochDay(p.paymentDate), p.invoiceId, p.unitId, p.tenantName, p.period, p.amount, p.method, p.receiptNumber).joinToString(",") { escape(it.toString()) }) }
        appendLine()
        appendLine("PENGELUARAN")
        appendLine("ID,Tanggal,Periode,Unit,Kategori,Kelompok,Uraian,Nominal,Metode,Masuk LR")
        expenses.forEach { e -> appendLine(listOf(e.id, LocalDate.ofEpochDay(e.expenseDate), e.period, e.unitName, e.category, e.groupName, e.description, e.amount, e.method, e.includeInProfitLoss).joinToString(",") { escape(it.toString()) }) }
    }

    fun unitTemplate(): String = "ID Unit,Nama Unit,Penyewa,Frekuensi,Tarif,Interval Bulan,Cadangan,Status,Jatuh Tempo,Catatan\n"

    fun parseUnits(csv: String): List<RentalUnitEntity> = csv.lineSequence()
        .filter { it.isNotBlank() }
        .drop(1)
        .mapIndexed { index, line ->
            val c = parseLine(line)
            require(c.size >= 9) { "Baris ${index + 2}: kolom tidak lengkap." }
            val code = c[0].trim()
            require(code.isNotBlank()) { "Baris ${index + 2}: ID Unit kosong." }
            RentalUnitEntity(
                id = code,
                code = code,
                name = c[1].trim(),
                tenantName = c[2].trim(),
                frequency = c[3].trim(),
                rate = c[4].trim().toLong(),
                intervalMonths = c[5].trim().toInt(),
                reservePercent = c[6].trim().replace("%", "").toDouble().let { if (it > 1) it / 100 else it },
                status = c[7].trim(),
                dueDay = c[8].trim().toInt(),
                notes = c.getOrElse(9) { "" }.trim(),
            )
        }.toList()

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { current.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { result += current.toString(); current.clear() }
                else -> current.append(ch)
            }
            i++
        }
        result += current.toString()
        return result
    }

    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
}
