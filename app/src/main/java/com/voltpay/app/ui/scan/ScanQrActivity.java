package com.voltpay.app.ui.scan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.voltpay.app.R;
import com.voltpay.app.ui.payment.SendMoneyActivity;

import java.util.List;

public class ScanQrActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 102;
    private DecoratedBarcodeView barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        barcodeScanner = findViewById(R.id.barcodeScanner);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        View btnImportGallery = findViewById(R.id.btnImportGallery);
        if (btnImportGallery != null) {
            btnImportGallery.setOnClickListener(v -> openGallery());
        }

        // Request Camera Permission Dynamically (User Rule: Ask only when opening scanner)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    barcodeScanner.pause();
                    handleQrResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    private void handleQrResult(String qrData) {
        // Simple parsing for upi://pay?pa=...
        if (qrData.startsWith("upi://pay")) {
            Uri uri = Uri.parse(qrData);
            String upiId = uri.getQueryParameter("pa");
            String amount = uri.getQueryParameter("am"); // Might be null

            if (upiId != null && !upiId.isEmpty()) {
                Intent intent = new Intent(this, SendMoneyActivity.class);
                intent.putExtra("EXTRA_UPI_ID", upiId);
                if (amount != null) intent.putExtra("EXTRA_AMOUNT", amount);
                startActivity(intent);
                finish();
                return;
            }
        }
        
        Toast.makeText(this, "Invalid UPI QR Code", Toast.LENGTH_SHORT).show();
        // Resume scanning after 2 seconds
        barcodeScanner.postDelayed(() -> barcodeScanner.resume(), 2000);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                decodeBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void decodeBitmap(Bitmap bitmap) {
        try {
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            Result result = new MultiFormatReader().decode(binaryBitmap);
            handleQrResult(result.getText());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "No QR code found in image", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }
}
