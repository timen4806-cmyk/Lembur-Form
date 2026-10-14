package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_PRINT_ID = "channel_spkl_print"
    const val CHANNEL_REMINDER_ID = "channel_spkl_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val printChannel = NotificationChannel(
                CHANNEL_PRINT_ID,
                "Pencetakan & Dokumen SPKL",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi status pencetakan dan penyimpanan PDF lembur"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "Pengingat Cut-Off & Tugas SPKL",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat penyerahan form SPKL sebelum tanggal 17 setiap bulannya"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(printChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    fun showPrintCompletedNotification(context: Context, documentTitle: String, fileName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PRINT_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Pencetakan Selesai")
            .setContentText("Dokumen $documentTitle ($fileName) berhasil diproses ke printer / PDF.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Dokumen SPKL untuk $documentTitle telah siap dan berhasil diproses. Berkas tersimpan dalam arsip terenkripsi.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // Permission POST_NOTIFICATIONS may not be granted yet
        }
    }

    fun showCutoffReminderNotification(context: Context, month: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Peringatan Cut-Off SPKL (Tgl 17)")
            .setContentText("Batas akhir penyerahan form SPKL periode $month ke Kantor Pusat adalah tgl 17!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Perhatian: Mohon segera lengkapi tanda tangan SPKL dan rekapan Attendlist untuk periode $month sebelum tanggal 17 agar masuk dalam cut-off penggajian.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1717, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
