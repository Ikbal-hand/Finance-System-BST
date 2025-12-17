package com.loganes.finace.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.loganes.finace.data.model.BranchType
import com.loganes.finace.data.model.Transaction
import com.loganes.finace.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

object PdfHelper {

    // Ukuran Kertas A4 (PostScript Points)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // Palet Warna Profesional
    private val COLOR_PRIMARY = Color.parseColor("#1565C0") // Biru Tua
    private val COLOR_HEADER_BG = Color.parseColor("#E3F2FD") // Biru Muda
    private val COLOR_INCOME = Color.parseColor("#2E7D32")  // Hijau
    private val COLOR_EXPENSE = Color.parseColor("#C62828") // Merah
    private val COLOR_TEXT = Color.parseColor("#212121")    // Hitam Abu

    fun generateAndSendPdf(
        context: Context,
        transactions: List<Transaction>,
        startDate: String,
        endDate: String
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

        // --- HALAMAN 1: DASHBOARD RINGKASAN & GRAFIK ---
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = MARGIN

        // 1. HEADER UTAMA
        drawMainHeader(canvas, paint, startDate, endDate)
        yPos += 100f

        // 2. KOTAK TOTAL GLOBAL
        val globalIncome = transactions.filter { it.typeEnum == TransactionType.INCOME && !it.isPettyCash }.sumOf { it.amount }
        val globalExpense = transactions.filter { it.typeEnum == TransactionType.EXPENSE && !it.isPettyCash }.sumOf { it.amount }
        val totalAssets = FinanceCalculator.calculateTotalAssets(transactions)

        drawGlobalSummary(canvas, paint, yPos, globalIncome, globalExpense, totalAssets)
        yPos += 120f

        // 3. GRAFIK PERFORMA CABANG (Bar Chart Manual)
        drawBranchPerformanceChart(canvas, paint, yPos, transactions)

        // Selesai Halaman 1
        drawFooter(canvas, paint, 1)
        pdfDocument.finishPage(page)

        // --- HALAMAN 2 dst: RINCIAN PER CABANG ---
        // Grouping data berdasarkan cabang
        val groupedTransactions = transactions.groupBy { it.branch }
        var pageNum = 2

        groupedTransactions.forEach { (branchName, branchTrans) ->
            // Mulai Halaman Baru untuk setiap Cabang (agar rapi)
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = MARGIN

            // Header Cabang
            paint.color = COLOR_PRIMARY
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Laporan: ${branchName.replace("_", " ")}", MARGIN, yPos + 20, paint)

            // Sub-total Cabang
            val bIncome = branchTrans.filter { it.typeEnum == TransactionType.INCOME }.sumOf { it.amount }
            val bExpense = branchTrans.filter { it.typeEnum == TransactionType.EXPENSE }.sumOf { it.amount }

            paint.textSize = 12f
            paint.color = Color.GRAY
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Total Masuk: ${formatRp.format(bIncome)} | Keluar: ${formatRp.format(bExpense)}", MARGIN, yPos + 45, paint)

            yPos += 70f

            // Tabel Header
            drawTableHeader(canvas, paint, yPos)
            yPos += 25f

            // List Item Transaksi
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT

            for (item in branchTrans.sortedByDescending { it.date }) {
                // Cek Page Break
                if (yPos > PAGE_HEIGHT - 60) {
                    drawFooter(canvas, paint, pageNum)
                    pdfDocument.finishPage(page)
                    pageNum++

                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = MARGIN + 40
                    drawTableHeader(canvas, paint, yPos)
                    yPos += 25f
                }

                val isExpense = item.typeEnum == TransactionType.EXPENSE
                paint.color = if (isExpense) COLOR_EXPENSE else COLOR_TEXT

                // Kolom 1: Tanggal
                canvas.drawText(item.date, MARGIN, yPos, paint)

                // Kolom 2: Kategori
                val cat = if (item.category.length > 18) item.category.take(15) + ".." else item.category
                canvas.drawText(cat, MARGIN + 80, yPos, paint)

                // Kolom 3: Ket
                val desc = if (item.description.length > 25) item.description.take(22) + ".." else item.description
                canvas.drawText(desc, MARGIN + 200, yPos, paint)

                // Kolom 4: Nominal
                paint.textAlign = Paint.Align.RIGHT
                val sign = if (isExpense) "- " else "+ "
                canvas.drawText(sign + formatRp.format(item.amount), PAGE_WIDTH - MARGIN - 5, yPos, paint)
                paint.textAlign = Paint.Align.LEFT

                yPos += 20f
            }

            drawFooter(canvas, paint, pageNum)
            pdfDocument.finishPage(page)
            pageNum++
        }

        saveAndSharePdf(context, pdfDocument)
    }

    // --- FUNGSI GAMBAR GRAFIK BATANG ---
    private fun drawBranchPerformanceChart(c: Canvas, p: Paint, startY: Float, allTrans: List<Transaction>) {
        val titleY = startY
        p.color = COLOR_TEXT
        p.textSize = 14f
        p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("Grafik Performa Cabang (Pemasukan vs Pengeluaran)", MARGIN, titleY, p)

        val chartY = titleY + 30f
        val chartHeight = 150f
        val chartWidth = PAGE_WIDTH - (MARGIN * 2)
        val branches = BranchType.values()

        // Cari nilai tertinggi untuk skala grafik
        var maxValue = 0.0
        val branchData = branches.map { branch ->
            val bTrans = allTrans.filter { it.branch == branch.name }
            val inc = bTrans.filter { it.typeEnum == TransactionType.INCOME }.sumOf { it.amount }
            val exp = bTrans.filter { it.typeEnum == TransactionType.EXPENSE }.sumOf { it.amount }
            maxValue = max(maxValue, max(inc, exp))
            Triple(branch.name, inc, exp)
        }

        if (maxValue == 0.0) maxValue = 1.0 // Hindari divide by zero

        // Gambar Garis Sumbu
        p.color = Color.LTGRAY
        p.strokeWidth = 2f
        c.drawLine(MARGIN, chartY + chartHeight, MARGIN + chartWidth, chartY + chartHeight, p) // X Axis
        c.drawLine(MARGIN, chartY, MARGIN, chartY + chartHeight, p) // Y Axis

        // Gambar Batang
        val barWidth = (chartWidth / branches.size) / 3
        var currentX = MARGIN + 30f

        branchData.forEach { (name, inc, exp) ->
            // Hitung tinggi batang (skala)
            val incHeight = (inc / maxValue * chartHeight).toFloat()
            val expHeight = (exp / maxValue * chartHeight).toFloat()

            // Batang Pemasukan (Hijau)
            p.color = COLOR_INCOME
            c.drawRect(currentX, chartY + chartHeight - incHeight, currentX + barWidth, chartY + chartHeight, p)

            // Batang Pengeluaran (Merah)
            p.color = COLOR_EXPENSE
            c.drawRect(currentX + barWidth, chartY + chartHeight - expHeight, currentX + (barWidth * 2), chartY + chartHeight, p)

            // Label Cabang (Singkat)
            p.color = COLOR_TEXT
            p.textSize = 9f
            p.textAlign = Paint.Align.CENTER
            val shortName = if(name == "PUSAT") "PUSAT" else name.take(3) + ".."
            c.drawText(shortName, currentX + barWidth, chartY + chartHeight + 15, p)

            currentX += (barWidth * 3) + 10f // Geser ke kanan
        }

        // Legenda Grafik
        p.textAlign = Paint.Align.LEFT
        val legendY = chartY + chartHeight + 40f
        p.color = COLOR_INCOME
        c.drawRect(MARGIN, legendY, MARGIN + 10, legendY + 10, p)
        p.color = Color.GRAY
        c.drawText("Pemasukan", MARGIN + 15, legendY + 8, p)

        p.color = COLOR_EXPENSE
        c.drawRect(MARGIN + 100, legendY, MARGIN + 110, legendY + 10, p)
        p.color = Color.GRAY
        c.drawText("Pengeluaran", MARGIN + 115, legendY + 8, p)
    }

    // --- HELPER LAINNYA ---

    private fun drawMainHeader(c: Canvas, p: Paint, start: String, end: String) {
        // Background Header
        p.color = COLOR_PRIMARY
        p.style = Paint.Style.FILL
        c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 120f, p)

        // Teks Judul
        p.color = Color.WHITE
        p.textSize = 26f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        c.drawText("LAPORAN KEUANGAN EKSEKUTIF", MARGIN, 50f, p)

        p.textSize = 14f
        p.typeface = Typeface.DEFAULT
        c.drawText("Periode Laporan: $start s/d $end", MARGIN, 80f, p)
        c.drawText("Generated by Loganes App", PAGE_WIDTH - MARGIN - 180, 80f, p)
    }

    private fun drawGlobalSummary(c: Canvas, p: Paint, y: Float, income: Double, expense: Double, asset: Double) {
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

        // Kotak 1: Total Aset
        drawCard(c, p, MARGIN, y, 160f, "Total Aset Bersih", formatRp.format(asset), COLOR_PRIMARY)

        // Kotak 2: Total Omzet
        drawCard(c, p, MARGIN + 180, y, 160f, "Total Pemasukan", formatRp.format(income), COLOR_INCOME)

        // Kotak 3: Total Biaya
        drawCard(c, p, MARGIN + 360, y, 160f, "Total Pengeluaran", formatRp.format(expense), COLOR_EXPENSE)
    }

    private fun drawCard(c: Canvas, p: Paint, x: Float, y: Float, w: Float, title: String, value: String, color: Int) {
        // Background Card
        p.color = Color.parseColor("#F5F5F5")
        p.style = Paint.Style.FILL
        c.drawRect(x, y, x + w, y + 80f, p)

        // Border Atas (Aksen Warna)
        p.color = color
        c.drawRect(x, y, x + w, y + 5f, p)

        // Teks
        p.textSize = 10f
        p.color = Color.GRAY
        p.typeface = Typeface.DEFAULT
        c.drawText(title, x + 10, y + 30, p)

        p.textSize = 14f // Ukuran font nilai agak dikecilkan biar muat
        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT_BOLD
        // Logic simple buat ngepasin teks
        val displayValue = if (value.length > 15) "Rp..." else value
        c.drawText(displayValue, x + 10, y + 55, p)
    }

    private fun drawTableHeader(c: Canvas, p: Paint, y: Float) {
        p.color = COLOR_HEADER_BG
        p.style = Paint.Style.FILL
        c.drawRect(MARGIN, y - 15, PAGE_WIDTH - MARGIN, y + 5, p)

        p.color = COLOR_PRIMARY
        p.textSize = 10f
        p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("TANGGAL", MARGIN + 5, y, p)
        c.drawText("KATEGORI", MARGIN + 80, y, p)
        c.drawText("KETERANGAN", MARGIN + 200, y, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("NOMINAL", PAGE_WIDTH - MARGIN - 5, y, p)
        p.textAlign = Paint.Align.LEFT
    }

    private fun drawFooter(c: Canvas, p: Paint, pageNum: Int) {
        val yFooter = PAGE_HEIGHT - 30f
        p.color = Color.LTGRAY
        p.strokeWidth = 1f
        c.drawLine(MARGIN, yFooter - 15, PAGE_WIDTH - MARGIN, yFooter - 15, p)
        p.color = Color.GRAY
        p.textSize = 10f
        c.drawText("Loganes Finance - Confidential", MARGIN, yFooter, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("Hal $pageNum", PAGE_WIDTH - MARGIN, yFooter, p)
        p.textAlign = Paint.Align.LEFT
    }

    private fun saveAndSharePdf(context: Context, document: PdfDocument) {
        try {
            val file = File(context.cacheDir, "Laporan_Keuangan_Loganes.pdf")
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Kirim Laporan"))
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
        }
    }
}