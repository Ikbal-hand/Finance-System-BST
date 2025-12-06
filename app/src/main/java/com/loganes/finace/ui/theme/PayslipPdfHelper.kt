package com.loganes.finace.UI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.loganes.finace.model.Employee
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PayslipPdfHelper {

    private const val PAGE_WIDTH = 595f // Float
    private const val PAGE_HEIGHT = 842f // Float
    private const val MARGIN = 50f

    private val COLOR_PRIMARY = Color.parseColor("#1565C0")
    private val COLOR_ACCENT = Color.parseColor("#E3F2FD")
    private val COLOR_TEXT = Color.BLACK

    // UBAH JADI RETURN FILE
    fun generatePayslipPdf(context: Context, employee: Employee): File {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        val today = Date()

        var yPos = 60f

        // --- HEADER ---
        paint.color = COLOR_PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 120f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(MARGIN + 30f, 60f, 30f, paint)
        paint.color = COLOR_PRIMARY
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("BST", MARGIN + 30f, 68f, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 24f
        canvas.drawText("SLIP GAJI KARYAWAN", MARGIN + 80f, 55f, paint)
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("CV. BANGUN SEJAHTERA", MARGIN + 80f, 80f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 12f
        canvas.drawText("Periode:", PAGE_WIDTH - MARGIN, 50f, paint)
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(monthFormat.format(today).uppercase(), PAGE_WIDTH - MARGIN, 75f, paint)

        yPos += 100f

        // --- INFO PEGAWAI ---
        yPos += 40f
        drawInfoRow(canvas, paint, "Nama Pegawai", employee.name, MARGIN, yPos)
        drawInfoRow(canvas, paint, "Divisi / Cabang", employee.branch.name, (PAGE_WIDTH / 2f) + 20f, yPos)

        yPos += 25f
        val empId = "PEG-${employee.id.toString().take(5).uppercase()}"
        drawInfoRow(canvas, paint, "ID Pegawai", empId, MARGIN, yPos)
        drawInfoRow(canvas, paint, "Tanggal Cetak", dateFormat.format(today), (PAGE_WIDTH / 2f) + 20f, yPos)

        yPos += 40f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paint)
        yPos += 30f

        // --- RINCIAN ---
        paint.color = COLOR_ACCENT
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN, yPos - 15f, PAGE_WIDTH - MARGIN, yPos + 10f, paint)

        paint.color = COLOR_PRIMARY
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("KETERANGAN", MARGIN + 10f, yPos, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("JUMLAH (IDR)", PAGE_WIDTH - MARGIN - 10f, yPos, paint)

        yPos += 35f
        drawSalaryItem(canvas, paint, "Gaji Pokok", employee.salaryAmount, yPos, formatRp, false)
        yPos += 25f

        yPos += 10f
        paint.color = COLOR_PRIMARY
        paint.strokeWidth = 2f
        canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, paint)
        yPos += 30f

        // --- TOTAL ---
        val totalGaji = employee.salaryAmount
        paint.color = COLOR_PRIMARY
        paint.style = Paint.Style.FILL
        val rectTotal = RectF(MARGIN, yPos - 20f, PAGE_WIDTH - MARGIN, yPos + 20f)
        canvas.drawRoundRect(rectTotal, 10f, 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL DITERIMA (TAKE HOME PAY)", MARGIN + 20f, yPos + 5f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(formatRp.format(totalGaji), PAGE_WIDTH - MARGIN - 20f, yPos + 5f, paint)

        // --- FOOTER ---
        yPos += 100f
        paint.color = COLOR_TEXT
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER

        val xAdmin = PAGE_WIDTH - MARGIN - 60f
        canvas.drawText("Bandung, ${dateFormat.format(today)}", xAdmin, yPos, paint)
        canvas.drawText("Dibuat Oleh,", xAdmin, yPos + 15f, paint)

        canvas.drawLine(xAdmin - 60f, yPos + 70f, xAdmin + 60f, yPos + 70f, paint)
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Admin Keuangan", xAdmin, yPos + 85f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.color = Color.GRAY
        canvas.drawText("* Slip gaji ini sah dan diterbitkan secara otomatis oleh sistem.", MARGIN, PAGE_HEIGHT - 50f, paint)

        pdfDocument.finishPage(page)

        // --- SIMPAN FILE ---
        val cleanName = employee.name.replace(" ", "_")
        val filename = "SlipGaji_${cleanName}_${monthFormat.format(today)}.pdf"
        val file = File(context.cacheDir, filename)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file // Kembalikan File agar bisa dikirim
    }

    private fun drawInfoRow(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float) {
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(label, x, y, paint)
        paint.color = COLOR_TEXT
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(value, x, y + 15f, paint)
    }

    private fun drawSalaryItem(canvas: Canvas, paint: Paint, label: String, amount: Double, y: Float, format: NumberFormat, isDeduction: Boolean) {
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT
        canvas.drawText(label, MARGIN + 10f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        if (isDeduction) {
            paint.color = Color.RED
            canvas.drawText("- ${format.format(amount)}", PAGE_WIDTH - MARGIN - 10f, y, paint)
        } else {
            paint.color = COLOR_TEXT
            canvas.drawText(format.format(amount), PAGE_WIDTH - MARGIN - 10f, y, paint)
        }
    }
}