package id.djawadwipa.manajemenkontrakan.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val idLocale = Locale.forLanguageTag("id-ID")
private val currencyFormat = NumberFormat.getCurrencyInstance(idLocale).apply { maximumFractionDigits = 0 }
private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", idLocale)

fun Long.toRupiah(): String = currencyFormat.format(this)
fun Long.toDateLabel(): String = LocalDate.ofEpochDay(this).format(dateFormat)
