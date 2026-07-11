package com.voltpay.app.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.voltpay.app.R
import com.voltpay.app.ui.payment.SendMoneyActivity

class ScanQrActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        barcodeScanner = findViewById(R.id.barcodeScanner)
        
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnGallery).setOnClickListener { openGallery() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                result.text?.let {
                    barcodeScanner.pause()
                    handleQrResult(it)
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })
    }

    private fun handleQrResult(qrData: String) {
        if (qrData.startsWith("upi://pay")) {
            val uri = Uri.parse(qrData)
            val upiId = uri.getQueryParameter("pa")
            val amount = uri.getQueryParameter("am")

            if (!upiId.isNullOrEmpty()) {
                val intent = Intent(this, SendMoneyActivity::class.java).apply {
                    putExtra("EXTRA_UPI_ID", upiId)
                    if (amount != null) putExtra("EXTRA_AMOUNT", amount)
                }
                startActivity(intent)
                finish()
                return
            }
        }
        
        Toast.makeText(this, "Invalid UPI QR Code", Toast.LENGTH_SHORT).show()
        barcodeScanner.postDelayed({ barcodeScanner.resume() }, 2000)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data?.data != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                decodeBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeBitmap(bitmap: Bitmap) {
        try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            val result = MultiFormatReader().decode(binaryBitmap)
            handleQrResult(result.text)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No QR code found in image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner.pause()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private const val PICK_IMAGE_REQUEST = 102
    }
}
