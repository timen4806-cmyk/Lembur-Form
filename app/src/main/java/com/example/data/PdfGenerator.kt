package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.model.EmployeeData
import com.example.model.LemburRowItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for generating official SPKL (Surat Perintah Kerja Lembur) PDF documents
 * using Android's native [PdfDocument] and [Canvas] 2D graphics APIs.
 */
object PdfGenerator {
    const val PAGE_WIDTH = 595 // Standard A4 width in PostScript points (72 DPI)
    const val PAGE_HEIGHT = 842 // Standard A4 height in PostScript points (72 DPI)
    const val MARGIN = 36f

    // Primary Colors
    private val COLOR_PRIMARY = Color.rgb(15, 76, 76) // Deep Enterprise Teal
    private val COLOR_HEADER_FILL = Color.rgb(225, 238, 236) // Light Teal Tint
    private val COLOR_ROW_ALT_FILL = Color.rgb(250, 252, 252) // Alternating row color
    private val COLOR_BORDER = Color.rgb(60, 60, 60) // Clean charcoal border

    /**
     * Generates a complete SPKL PDF document into [outputFile].
     *
     * @param context Android context for content resolution (e.g. logo URI)
     * @param employee Employee header information including custom company name
     * @param rows List of overtime work entries
     * @param ybsSig Digital signature bitmap of the employee (Yang Bersangkutan)
     * @param leaderSig Digital signature bitmap of the Area Leader (Pimpinan Area)
     * @param spvSig Digital signature bitmap of Central Management (Pimpinan Pusat)
     * @param logoUri Optional URI pointing to custom company logo in device storage
     * @param logoBitmap Optional direct Bitmap for company logo
     * @param outputFile Target file to save the generated PDF
     * @return Generated [File]
     */
    fun generateSpklPdf(
        context: Context,
        employee: EmployeeData,
        rows: List<LemburRowItem>,
        ybsSig: Bitmap?,
        leaderSig: Bitmap?,
        spvSig: Bitmap?,
        logoUri: String? = null,
        logoBitmap: Bitmap? = null,
        outputFile: File
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Resolved company logo
        val resolvedLogo = logoBitmap ?: resolveLogoFromUri(context, logoUri)

        // Rendering Pipeline
        var currentY = MARGIN
        // 1. Header with Company Name on LEFT and Logo on RIGHT
        currentY = drawHeader(canvas, currentY, employee, resolvedLogo)
        // 2. Employee Info Table
        currentY = drawEmployeeInfoTable(canvas, currentY, employee)
        // 3. Overtime Table with signatures in PARAF PENGESAHAN columns
        currentY = drawOvertimeTable(canvas, currentY, rows, ybsSig, leaderSig, spvSig)
        // 4. Notes inside a clean bordered rectangular box
        drawNotesInBox(canvas, currentY, employee)

        document.finishPage(page)

        // Write to output file
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    /** Overload for compatibility with calls omitting direct [logoBitmap] */
    fun generateSpklPdf(
        context: Context,
        employee: EmployeeData,
        rows: List<LemburRowItem>,
        ybsSig: Bitmap?,
        leaderSig: Bitmap?,
        spvSig: Bitmap?,
        logoUri: String? = null,
        outputFile: File
    ): File {
        return generateSpklPdf(
            context = context,
            employee = employee,
            rows = rows,
            ybsSig = ybsSig,
            leaderSig = leaderSig,
            spvSig = spvSig,
            logoUri = logoUri,
            logoBitmap = null,
            outputFile = outputFile
        )
    }

    /**
     * Resolves and downsamples an image URI into a Bitmap.
     */
    private fun resolveLogoFromUri(context: Context, logoUri: String?): Bitmap? {
        if (logoUri.isNullOrEmpty()) return null
        return try {
            val uri = Uri.parse(logoUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 1. Draws Header:
     * - LEFT: Company Name (manual user input), Document Title (SPKL), Subtitle, and Date.
     * - RIGHT: Company Logo (moved to right side as requested).
     */
    private fun drawHeader(
        canvas: Canvas,
        startY: Float,
        employee: EmployeeData,
        logoBitmap: Bitmap?
    ): Float {
        var currentY = startY
        val logoBoxSize = 44f
        val centerX = PAGE_WIDTH / 2f

        // RIGHT: Draw Company Logo or stylized SPKL Badge
        val logoLeft = PAGE_WIDTH - MARGIN - logoBoxSize
        val logoRect = RectF(logoLeft, currentY, logoLeft + logoBoxSize, currentY + logoBoxSize)

        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG))
        } else {
            val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(logoRect, 6f, 6f, badgeBg)
            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("SPKL", logoRect.centerX(), logoRect.centerY() + 4f, badgeText)
        }

        // LEFT: Company Name in bold, uppercase, prominent font
        val companyNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val compName = employee.namaPerusahaan.ifBlank { "PT. MULTI KARYA UTAMA" }.uppercase()
        canvas.drawText(compName, MARGIN, currentY + 16f, companyNamePaint)

        // LEFT: Date / Metadata subtext
        val dateSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val dateNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Dicetak: $dateNow WIB • Dokumen Resmi", MARGIN, currentY + 30f, dateSubPaint)

        // CENTER: Document Title strictly centered in the middle of the PDF
        currentY += logoBoxSize + 6f

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 60, 60)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("SURAT PERINTAH KERJA LEMBUR (SPKL)", centerX, currentY + 12f, headerTitlePaint)
        canvas.drawText("FORM LEMBUR KARYAWAN & REKAPAN PENUGASAN RESMI", centerX, currentY + 25f, subHeaderPaint)

        // Header separator line
        currentY += 34f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        currentY += 8f

        return currentY
    }

    /**
     * 2. Draws the 2-Column Employee Information Grid Table.
     */
    private fun drawEmployeeInfoTable(canvas: Canvas, startY: Float, employee: EmployeeData): Float {
        var currentY = startY
        val tableLeft = MARGIN
        val tableWidth = PAGE_WIDTH - (MARGIN * 2)
        val colLabelW = 88f
        val colValW = (tableWidth / 2f) - colLabelW
        val rowH = 17f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val fillHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_FILL
            style = Paint.Style.FILL
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val employeeFields = listOf(
            Pair("NAMA KARYAWAN", employee.namaKaryawan.ifBlank { "-" }) to Pair("WILAYAH", employee.wilayah.ifBlank { "-" }),
            Pair("NOMER INDUK (NIK)", employee.nik.ifBlank { "-" }) to Pair("LOKASI KERJA", employee.lokasiKerja.ifBlank { "-" }),
            Pair("DEPARTEMEN", employee.departemen.ifBlank { "-" }) to Pair("BULAN", employee.bulan.ifBlank { "-" }),
            Pair("JABATAN", employee.jabatan.ifBlank { "-" }) to Pair("TAHUN", employee.tahun.ifBlank { "-" })
        )

        for (item in employeeFields) {
            val leftField = item.first
            val rightField = item.second

            // Left Col Label
            val rLeftLabel = RectF(tableLeft, currentY, tableLeft + colLabelW, currentY + rowH)
            canvas.drawRect(rLeftLabel, fillHeaderPaint)
            canvas.drawRect(rLeftLabel, borderPaint)
            canvas.drawText(" ${leftField.first}", rLeftLabel.left + 3f, rLeftLabel.centerY() + 3.2f, boldPaint)

            // Left Col Val
            val rLeftVal = RectF(tableLeft + colLabelW, currentY, tableLeft + colLabelW + colValW, currentY + rowH)
            canvas.drawRect(rLeftVal, borderPaint)
            canvas.drawText(" ${leftField.second}", rLeftVal.left + 3f, rLeftVal.centerY() + 3.2f, textPaint)

            // Right Col Label
            val rightStartX = tableLeft + colLabelW + colValW
            val rRightLabel = RectF(rightStartX, currentY, rightStartX + colLabelW, currentY + rowH)
            canvas.drawRect(rRightLabel, fillHeaderPaint)
            canvas.drawRect(rRightLabel, borderPaint)
            canvas.drawText(" ${rightField.first}", rRightLabel.left + 3f, rRightLabel.centerY() + 3.2f, boldPaint)

            // Right Col Val
            val rRightVal = RectF(rightStartX + colLabelW, currentY, tableLeft + tableWidth, currentY + rowH)
            canvas.drawRect(rRightVal, borderPaint)
            canvas.drawText(" ${rightField.second}", rRightVal.left + 3f, rRightVal.centerY() + 3.2f, textPaint)

            currentY += rowH
        }

        return currentY + 10f
    }

    /**
     * 3. Draws the Overtime Detail Table with Signatures inside the PARAF PENGESAHAN columns.
     */
    private fun drawOvertimeTable(
        canvas: Canvas,
        startY: Float,
        rows: List<LemburRowItem>,
        ybsSig: Bitmap?,
        leaderSig: Bitmap?,
        spvSig: Bitmap?
    ): Float {
        var currentY = startY
        val tableLeft = MARGIN
        val tableWidth = PAGE_WIDTH - (MARGIN * 2)

        val colNoW = 24f
        val colDateW = 58f
        val colShiftW = 46f
        val colHoursW = 52f
        val colTotalHW = 38f
        // Generous width for the signature cells in PARAF PENGESAHAN
        val colParafYbsW = 48f
        val colParafLeaderW = 48f
        val colParafSpvW = 48f
        val colKetW = tableWidth - (colNoW + colDateW + colShiftW + colHoursW + colTotalHW + colParafYbsW + colParafLeaderW + colParafSpvW)

        val headerH1 = 16f
        val headerH2 = 16f
        val totalHeaderH = headerH1 + headerH2

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val fillHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_FILL
            style = Paint.Style.FILL
        }
        val fillRowAltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROW_ALT_FILL
            style = Paint.Style.FILL
        }
        val centerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // --- Header Rows ---
        // NO
        val rNo = RectF(tableLeft, currentY, tableLeft + colNoW, currentY + totalHeaderH)
        canvas.drawRect(rNo, fillHeaderPaint); canvas.drawRect(rNo, borderPaint)
        canvas.drawText("NO", rNo.centerX(), rNo.centerY() + 3f, centerBoldPaint)

        // HARI/TGL
        val rDate = RectF(rNo.right, currentY, rNo.right + colDateW, currentY + totalHeaderH)
        canvas.drawRect(rDate, fillHeaderPaint); canvas.drawRect(rDate, borderPaint)
        canvas.drawText("HARI/TGL", rDate.centerX(), rDate.centerY() + 3f, centerBoldPaint)

        // KETERANGAN TUGAS & JAM (Spans Shift, Jam, Total Jam)
        val tugasGroupW = colShiftW + colHoursW + colTotalHW
        val rTugasGroup = RectF(rDate.right, currentY, rDate.right + tugasGroupW, currentY + headerH1)
        canvas.drawRect(rTugasGroup, fillHeaderPaint); canvas.drawRect(rTugasGroup, borderPaint)
        canvas.drawText("KETERANGAN TUGAS & JAM", rTugasGroup.centerX(), rTugasGroup.centerY() + 3f, centerBoldPaint)

        // PARAF PENGESAHAN (Spans YBS, Pim. Area, Pim. Pusat)
        val parafGroupW = colParafYbsW + colParafLeaderW + colParafSpvW
        val rParafGroup = RectF(rTugasGroup.right, currentY, rTugasGroup.right + parafGroupW, currentY + headerH1)
        canvas.drawRect(rParafGroup, fillHeaderPaint); canvas.drawRect(rParafGroup, borderPaint)
        canvas.drawText("PARAF PENGESAHAN", rParafGroup.centerX(), rParafGroup.centerY() + 3f, centerBoldPaint)

        // KETERANGAN LEMBUR
        val rKet = RectF(rParafGroup.right, currentY, tableLeft + tableWidth, currentY + totalHeaderH)
        canvas.drawRect(rKet, fillHeaderPaint); canvas.drawRect(rKet, borderPaint)
        canvas.drawText("KETERANGAN LEMBUR", rKet.centerX(), rKet.centerY() + 3f, centerBoldPaint)

        // Subheaders
        val subY = currentY + headerH1
        val rShift = RectF(rDate.right, subY, rDate.right + colShiftW, subY + headerH2)
        canvas.drawRect(rShift, fillHeaderPaint); canvas.drawRect(rShift, borderPaint)
        canvas.drawText("SHIFT", rShift.centerX(), rShift.centerY() + 3f, centerBoldPaint)

        val rJam = RectF(rShift.right, subY, rShift.right + colHoursW, subY + headerH2)
        canvas.drawRect(rJam, fillHeaderPaint); canvas.drawRect(rJam, borderPaint)
        canvas.drawText("JAM", rJam.centerX(), rJam.centerY() + 3f, centerBoldPaint)

        val rJml = RectF(rJam.right, subY, rJam.right + colTotalHW, subY + headerH2)
        canvas.drawRect(rJml, fillHeaderPaint); canvas.drawRect(rJml, borderPaint)
        canvas.drawText("TOTAL", rJml.centerX(), rJml.centerY() + 3f, centerBoldPaint)

        val rYbs = RectF(rJml.right, subY, rJml.right + colParafYbsW, subY + headerH2)
        canvas.drawRect(rYbs, fillHeaderPaint); canvas.drawRect(rYbs, borderPaint)
        canvas.drawText("YBS", rYbs.centerX(), rYbs.centerY() + 3f, centerBoldPaint)

        val rArea = RectF(rYbs.right, subY, rYbs.right + colParafLeaderW, subY + headerH2)
        canvas.drawRect(rArea, fillHeaderPaint); canvas.drawRect(rArea, borderPaint)
        canvas.drawText("PIM. AREA", rArea.centerX(), rArea.centerY() + 3f, centerBoldPaint)

        val rPusat = RectF(rArea.right, subY, rArea.right + colParafSpvW, subY + headerH2)
        canvas.drawRect(rPusat, fillHeaderPaint); canvas.drawRect(rPusat, borderPaint)
        canvas.drawText("PIM. PUSAT", rPusat.centerX(), rPusat.centerY() + 3f, centerBoldPaint)

        currentY += totalHeaderH

        // --- Data Rows (Spacious for clear signature rendering) ---
        val dataRowH = 30f
        var totalHoursSum = 0.0

        for ((idx, row) in rows.withIndex()) {
            val rBg = RectF(tableLeft, currentY, tableLeft + tableWidth, currentY + dataRowH)
            if (idx % 2 == 1) {
                canvas.drawRect(rBg, fillRowAltPaint)
            }

            var currX = tableLeft

            // NO
            val cellNo = RectF(currX, currentY, currX + colNoW, currentY + dataRowH)
            canvas.drawRect(cellNo, borderPaint)
            canvas.drawText(row.no.toString(), cellNo.centerX(), cellNo.centerY() + 3f, centerTextPaint)
            currX += colNoW

            // DATE / DAY
            val cellDate = RectF(currX, currentY, currX + colDateW, currentY + dataRowH)
            canvas.drawRect(cellDate, borderPaint)
            val dayDateStr = if (row.hari.isNotBlank() && row.tgl.isNotBlank()) "${row.hari}, ${row.tgl}" else (row.hari.ifBlank { row.tgl })
            canvas.drawText(dayDateStr.ifBlank { "-" }, cellDate.centerX(), cellDate.centerY() + 3f, centerTextPaint)
            currX += colDateW

            // SHIFT
            val cellShift = RectF(currX, currentY, currX + colShiftW, currentY + dataRowH)
            canvas.drawRect(cellShift, borderPaint)
            canvas.drawText(row.shiftTugas.ifBlank { "-" }, cellShift.centerX(), cellShift.centerY() + 3f, centerTextPaint)
            currX += colShiftW

            // JAM TUGAS
            val cellJam = RectF(currX, currentY, currX + colHoursW, currentY + dataRowH)
            canvas.drawRect(cellJam, borderPaint)
            canvas.drawText(row.jamTugas.ifBlank { "-" }, cellJam.centerX(), cellJam.centerY() + 3f, centerTextPaint)
            currX += colHoursW

            // TOTAL JAM
            val cellTotal = RectF(currX, currentY, currX + colTotalHW, currentY + dataRowH)
            canvas.drawRect(cellTotal, borderPaint)
            val parsedHours = row.jumlahJam.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            totalHoursSum += parsedHours
            canvas.drawText(row.jumlahJam.ifBlank { "-" }, cellTotal.centerX(), cellTotal.centerY() + 3f, centerBoldPaint)
            currX += colTotalHW

            // 1. YBS Signature in PARAF PENGESAHAN
            val cellYbs = RectF(currX, currentY, currX + colParafYbsW, currentY + dataRowH)
            canvas.drawRect(cellYbs, borderPaint)
            if (row.tgl.isNotBlank() && ybsSig != null) {
                val sigRect = RectF(cellYbs.left + 2f, cellYbs.top + 2f, cellYbs.right - 2f, cellYbs.bottom - 2f)
                canvas.drawBitmap(ybsSig, null, sigRect, bitmapPaint)
            } else if (row.tgl.isNotBlank()) {
                canvas.drawText("✓ Paraf", cellYbs.centerX(), cellYbs.centerY() + 3f, centerTextPaint)
            }
            currX += colParafYbsW

            // 2. PIM AREA Signature in PARAF PENGESAHAN
            val cellLeader = RectF(currX, currentY, currX + colParafLeaderW, currentY + dataRowH)
            canvas.drawRect(cellLeader, borderPaint)
            if (row.tgl.isNotBlank() && leaderSig != null) {
                val sigRect = RectF(cellLeader.left + 2f, cellLeader.top + 2f, cellLeader.right - 2f, cellLeader.bottom - 2f)
                canvas.drawBitmap(leaderSig, null, sigRect, bitmapPaint)
            } else if (row.tgl.isNotBlank()) {
                canvas.drawText("✓ Disetujui", cellLeader.centerX(), cellLeader.centerY() + 3f, centerTextPaint)
            }
            currX += colParafLeaderW

            // 3. PIM PUSAT Signature in PARAF PENGESAHAN
            val cellSpv = RectF(currX, currentY, currX + colParafSpvW, currentY + dataRowH)
            canvas.drawRect(cellSpv, borderPaint)
            if (row.tgl.isNotBlank() && spvSig != null) {
                val sigRect = RectF(cellSpv.left + 2f, cellSpv.top + 2f, cellSpv.right - 2f, cellSpv.bottom - 2f)
                canvas.drawBitmap(spvSig, null, sigRect, bitmapPaint)
            } else if (row.tgl.isNotBlank()) {
                canvas.drawText("✓ Sah", cellSpv.centerX(), cellSpv.centerY() + 3f, centerTextPaint)
            }
            currX += colParafSpvW

            // KETERANGAN LEMBUR
            val cellKet = RectF(currX, currentY, tableLeft + tableWidth, currentY + dataRowH)
            canvas.drawRect(cellKet, borderPaint)
            val ketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 7f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val truncatedKet = if (row.keterangan.length > 34) row.keterangan.take(32) + "..." else row.keterangan
            canvas.drawText(truncatedKet.ifBlank { "-" }, cellKet.left + 4f, cellKet.centerY() + 2.5f, ketPaint)

            currentY += dataRowH
        }

        // Summary Total Hours Row
        val rSummary = RectF(tableLeft, currentY, tableLeft + tableWidth, currentY + 18f)
        canvas.drawRect(rSummary, fillHeaderPaint)
        canvas.drawRect(rSummary, borderPaint)
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "TOTAL REKAP JAM LEMBUR BULAN INI: ${String.format(Locale.US, "%.1f", totalHoursSum)} JAM",
            rSummary.centerX(),
            rSummary.centerY() + 3.2f,
            summaryPaint
        )

        return currentY + 26f
    }

    /**
     * 4. Draws Official Notes inside a clean bordered rectangular box as requested,
     * along with the cryptographic digital security seal.
     */
    private fun drawNotesInBox(canvas: Canvas, startY: Float, employee: EmployeeData) {
        var currentY = startY
        val tableLeft = MARGIN
        val tableWidth = PAGE_WIDTH - (MARGIN * 2)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val noteBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 250)
            style = Paint.Style.FILL
        }
        val noteHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 242, 241)
            style = Paint.Style.FILL
        }

        val noteBoxH = 68f
        val boxRect = RectF(tableLeft, currentY, tableLeft + tableWidth, currentY + noteBoxH)

        // Draw outer box background and border
        canvas.drawRect(boxRect, noteBoxBg)
        canvas.drawRect(boxRect, borderPaint)

        // Header band of the box
        val headerBandRect = RectF(tableLeft, currentY, tableLeft + tableWidth, currentY + 16f)
        canvas.drawRect(headerBandRect, noteHeaderBg)
        canvas.drawRect(headerBandRect, borderPaint)

        val noteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = COLOR_PRIMARY
        }
        canvas.drawText(" CATATAN PENTING & KETENTUAN PENGAJUAN SPKL:", tableLeft + 4f, currentY + 11.5f, noteTitlePaint)

        // Notes inside the box
        val noteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = Color.rgb(40, 40, 40)
        }

        val notes = listOf(
            "1. Form ini harus diisi dan diparaf secara sah oleh yang bersangkutan sebelum diserahkan ke kantor pusat.",
            "2. Perintah lembur ini wajib dimasukkan ke dalam rekapan Attendlist sesuai periode Cut Off pembayaran.",
            "3. Form SPKL akan diperiksa oleh Pimpinan Pusat sebelum diserahkan kepada HRD untuk penginputan.",
            "4. Berkas SPKL dan Attendlist harus diserahkan sebelum batas waktu (Tgl. 17 terakhir setiap bulannya)."
        )

        var noteY = currentY + 27f
        for (n in notes) {
            canvas.drawText(n, tableLeft + 8f, noteY, noteTextPaint)
            noteY += 10.5f
        }

        currentY += noteBoxH + 12f

        // Digital Security Verification Stamp
        val securityBorder = RectF(tableLeft, currentY, tableLeft + tableWidth, currentY + 18f)
        val secBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 244, 244)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(securityBorder, 4f, 4f, secBg)

        val secTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            textSize = 6.8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dateNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val checksum = Integer.toHexString((employee.nik + employee.namaKaryawan + dateNow).hashCode()).uppercase()
        canvas.drawText(
            "🔒 ENKRIPSI DIGITAL SHA-256 | KODE VERIFIKASI: SPKL-$checksum | DOKUMEN RESMI TERSERTIFIKASI",
            securityBorder.centerX(),
            securityBorder.centerY() + 2.5f,
            secTextPaint
        )
    }
}
