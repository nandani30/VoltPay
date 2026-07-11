package com.voltpay.app.ui.scan

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.voltpay.app.R
import java.io.File
import java.io.FileOutputStream

class MyQrActivity : AppCompatActivity() {

    private var qrBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_qr)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
        
        findViewById<View>(R.id.btnShare).setOnClickListener {
            qrBitmap?.let { shareQrCode(it) }
        }

        findViewById<View>(R.id.btnDownload).setOnClickListener {
            qrBitmap?.let { saveQrToGallery(it) }
        }

        var myName = "User"
        var myUpiId = "user@upi"
        
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            myName = sharedPreferences.getString("user_name", "User") ?: "User"
            myUpiId = sharedPreferences.getString("user_upi_id", "user@upi") ?: "user@upi"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        findViewById<TextView>(R.id.tvName)?.text = myName
        findViewById<TextView>(R.id.tvUpiId)?.text = myUpiId

        val qrContent = "upi://pay?pa=$myUpiId&pn=$myName"

        qrBitmap = generateQrCode(qrContent)
        qrBitmap?.let {
            findViewById<ImageView>(R.id.ivQrCode)?.setImageBitmap(it)
        }
    }

    private fun generateQrCode(content: String): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun saveQrToGallery(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "VoltPay_QR_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VoltPay")
            }
        }
        
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    Toast.makeText(this, "QR saved to Gallery!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show()
            }
        } else {
            @Suppress("DEPRECATION")
            val savedUrl = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "VoltPay QR", "My VoltPay QR Code")
            if (savedUrl != null) {
                Toast.makeText(this, "QR saved to Gallery!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareQrCode(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "my_qr.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show()
        }
    }
}
