package com.loganes.finace.UI

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.loganes.finace.model.BranchType
import com.loganes.finace.model.Transaction
import com.loganes.finace.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // Warna
    private val COLOR_PRIMARY = Color.parseColor("#1565C0")
    private val COLOR_ACCENT = Color.parseColor("#E3F2FD")
    private val COLOR_INCOME = Color.parseColor("#2E7D32")
    private val COLOR_EXPENSE = Color.parseColor("#C62828")
    private val COLOR_STRIPE = Color.parseColor("#F5F5F5")

    fun generateAndSendPdf(
        context: Context,
        transactions: List<Transaction>,
        startDate: String,
        endDate: String,
        chartBitmap: Bitmap? = null
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val printDate = dateFormat.format(Date())

        // --- LOGIKA UTAMA: TOTAL PERUSAHAAN (SEMUA MASUK KE PUSAT) ---
        // Ini adalah uang real yang dimiliki pemilik (Pusat) dari semua cabang
        val totalOmzetPerusahaan = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalBebanPerusahaan = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val saldoKasPusatReal = totalOmzetPerusahaan - totalBebanPerusahaan

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = 50f

        // --- 1. HEADER UTAMA ---
        drawHeader(canvas, paint, startDate, endDate)
        yPos += 80f

        // --- 2. DASHBOARD KAS PUSAT (KEKAYAAN REAL) ---
        // Judul Dashboard diganti agar user paham ini adalah akumulasi semua cabang
        paint.textSize = 12f
        paint.color = Color.DKGRAY
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("RINGKASAN KAS PUSAT (KONSOLIDASI)", MARGIN, yPos, paint)
        yPos += 20f

        drawSummaryBox(canvas, paint, "Total Omzet (Semua Cabang)", formatRp.format(totalOmzetPerusahaan), COLOR_INCOME, MARGIN, yPos)
        drawSummaryBox(canvas, paint, "Total Beban Operasional", formatRp.format(totalBebanPerusahaan), COLOR_EXPENSE, MARGIN + 180, yPos)
        // Saldo Akhir Real
        drawSummaryBox(canvas, paint, "Sisa Kas Netto", formatRp.format(saldoKasPusatReal), COLOR_PRIMARY, MARGIN + 360, yPos)

        yPos += 70f

        // --- 3. GRAFIK ---
        if (chartBitmap != null) {
            val scaledHeight = 200
            val scaledWidth = PAGE_WIDTH - (MARGIN * 2).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(chartBitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, MARGIN, yPos, paint)
            yPos += 220f
        }

        // --- 4. RINCIAN PER CABANG ---
        BranchType.values().forEach { branch ->
            val branchData = transactions.filter { it.branch == branch }

            if (branchData.isNotEmpty()) {

                // Pagination Check
                if (yPos > PAGE_HEIGHT - 120) {
                    drawFooter(canvas, paint, pageNumber, printDate)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 50f
                }

                // JUDUL CABANG
                // Jika cabang == PUSAT, kita ganti namanya agar tidak bingung
                val displayBranchName = if (branch.name.equals("PUSAT", ignoreCase = true)) "OPERASIONAL KANTOR PUSAT (HQ)" else "CABANG: ${branch.name.uppercase()}"

                paint.color = COLOR_PRIMARY
                paint.textSize = 14f
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(displayBranchName, MARGIN, yPos, paint)
                yPos += 10f
                paint.strokeWidth = 2f
                canvas.drawLine(MARGIN, yPos + 5, PAGE_WIDTH - MARGIN, yPos + 5, paint)
                yPos += 25f

                // Header Tabel
                drawTableHeader(canvas, paint, yPos)
                yPos += 25f

                // Loop Data
                var index = 0
                branchData.forEach { item ->
                    if (yPos > PAGE_HEIGHT - 60) {
                        drawFooter(canvas, paint, pageNumber, printDate)
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = 50f
                        drawTableHeader(canvas, paint, yPos)
                        yPos += 25f
                    }

                    // Zebra Striping
                    if (index % 2 == 0) {
                        paint.color = COLOR_STRIPE
                        paint.style = Paint.Style.FILL
                        canvas.drawRect(MARGIN, yPos - 15, PAGE_WIDTH - MARGIN, yPos + 10, paint)
                    }

                    paint.color = Color.BLACK
                    paint.textSize = 10f
                    paint.textAlign = Paint.Align.LEFT

                    canvas.drawText(item.date, MARGIN + 5, yPos, paint)
                    val kat = if (item.category.length > 18) item.category.take(15) + "..." else item.category
                    canvas.drawText(kat, MARGIN + 80, yPos, paint)
                    val desc = if (item.description.length > 25) item.description.take(22) + "..." else item.description
                    canvas.drawText(desc, MARGIN + 200, yPos, paint)

                    paint.textAlign = Paint.Align.RIGHT
                    if (item.type == TransactionType.INCOME) {
                        paint.color = COLOR_INCOME
                        canvas.drawText("+ ${formatRp.format(item.amount)}", PAGE_WIDTH - MARGIN - 5, yPos, paint)
                    } else {
                        paint.color = COLOR_EXPENSE
                        canvas.drawText("- ${formatRp.format(item.amount)}", PAGE_WIDTH - MARGIN - 5, yPos, paint)
                    }

                    yPos += 20f
                    index++
                }

                // --- LOGIKA BARU TOTAL CABANG ---
                val totalIncomeBranch = branchData.filter{it.type == TransactionType.INCOME}.sumOf{it.amount}
                val totalExpenseBranch = branchData.filter{it.type == TransactionType.EXPENSE}.sumOf{it.amount}

                paint.color = Color.DKGRAY
                canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paint)
                yPos += 20f

                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textSize = 11f

                // Tampilkan Kontribusi Pemasukan (Bukan Netto)
                paint.textAlign = Paint.Align.RIGHT
                paint.color = COLOR_INCOME
                // Label diganti "Setor ke Pusat" agar jelas uangnya lari ke pusat
                canvas.drawText("Total Pemasukan (Masuk ke Kas Pusat): ${formatRp.format(totalIncomeBranch)}", PAGE_WIDTH - MARGIN, yPos, paint)
                yPos += 15f

                paint.color = COLOR_EXPENSE
                // Label diganti "Penggunaan Anggaran"
                canvas.drawText("Penggunaan Kas Operasional: ${formatRp.format(totalExpenseBranch)}", PAGE_WIDTH - MARGIN, yPos, paint)

                yPos += 40f
            }
        }

        drawFooter(canvas, paint, pageNumber, printDate)
        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, "Laporan_Keuangan_Pusat.pdf") // Ganti nama file

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            sharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }

    // --- HELPER TETAP SAMA ---
    private fun drawHeader(canvas: Canvas, paint: Paint, start: String, end: String) {
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 90f, paint)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("LAPORAN KONSOLIDASI", MARGIN, 45f, paint) // Ganti Judul
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("CV BST - KEUANGAN PUSAT", MARGIN, 70f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 12f
        canvas.drawText("Periode:", PAGE_WIDTH - MARGIN, 45f, paint)
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("$start - $end", PAGE_WIDTH - MARGIN, 65f, paint)
    }

    private fun drawSummaryBox(c: Canvas, p: Paint, title: String, value: String, color: Int, x: Float, y: Float) {
        val width = 160f
        val height = 55f // Sedikit diperbesar
        p.color = color
        p.style = Paint.Style.FILL
        val rect = RectF(x, y, x + width, y + height)
        c.drawRoundRect(rect, 8f, 8f, p)
        p.color = Color.WHITE
        p.textSize = 9f
        p.typeface = Typeface.DEFAULT
        p.textAlign = Paint.Align.LEFT
        c.drawText(title, x + 10, y + 20, p)
        p.textSize = 13f
        p.typeface = Typeface.DEFAULT_BOLD
        c.drawText(value, x + 10, y + 42, p)
    }

    private fun drawTableHeader(c: Canvas, p: Paint, y: Float) {
        p.color = COLOR_ACCENT
        p.style = Paint.Style.FILL
        c.drawRect(MARGIN, y - 15, PAGE_WIDTH - MARGIN, y + 10, p)
        p.color = COLOR_PRIMARY
        p.textSize = 10f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.LEFT
        c.drawText("TANGGAL", MARGIN + 5, y, p)
        c.drawText("KATEGORI", MARGIN + 80, y, p)
        c.drawText("KETERANGAN", MARGIN + 200, y, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("NOMINAL (Rp)", PAGE_WIDTH - MARGIN - 5, y, p)
    }

    private fun drawFooter(c: Canvas, p: Paint, pageNum: Int, date: String) {
        val yFooter = PAGE_HEIGHT - 30f
        p.color = Color.LTGRAY
        p.strokeWidth = 1f
        c.drawLine(MARGIN, yFooter - 15, PAGE_WIDTH - MARGIN, yFooter - 15, p)
        p.color = Color.GRAY
        p.textSize = 10f
        p.typeface = Typeface.DEFAULT
        p.textAlign = Paint.Align.LEFT
        c.drawText("Dicetak: $date", MARGIN, yFooter, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("Halaman $pageNum", PAGE_WIDTH - MARGIN, yFooter, p)
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Konsolidasi CV BST")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Kirim Laporan via..."))
    }
}