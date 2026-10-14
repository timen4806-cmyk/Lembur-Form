package com.example.data

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfPrintDocumentAdapter(
    private val pdfFile: File,
    private val documentName: String = "SPKL_Form"
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("$documentName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
        }

        try {
            val input = FileInputStream(pdfFile)
            val output = FileOutputStream(destination?.fileDescriptor)

            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } > 0) {
                if (cancellationSignal?.isCanceled == true) {
                    input.close()
                    output.close()
                    callback?.onWriteCancelled()
                    return
                }
                output.write(buf, 0, bytesRead)
            }

            input.close()
            output.close()
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }

    companion object {
        fun printPdf(context: Context, pdfFile: File, jobName: String = "Cetak SPKL Lembur") {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val adapter = PdfPrintDocumentAdapter(pdfFile, jobName)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("spkl_res", "SPKL Print", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager?.print(jobName, adapter, printAttributes)
        }
    }
}
