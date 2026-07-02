package com.voltpay.app.ui.scan;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.voltpay.app.R;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.content.SharedPreferences;
import android.widget.TextView;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import android.content.ContentValues;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

public class MyQrActivity extends AppCompatActivity {

    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        findViewById(R.id.btnShareQr).setOnClickListener(v -> {
            if (qrBitmap != null) {
                shareQrCode(qrBitmap);
            }
        });

        View btnSaveQr = findViewById(R.id.btnSaveQr);
        if (btnSaveQr != null) {
            btnSaveQr.setOnClickListener(v -> {
                if (qrBitmap != null) {
                    saveQrToGallery(qrBitmap);
                }
            });
        }

        String myName = "User";
        String myUpiId = "user@upi";
        
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            myName = sharedPreferences.getString("user_name", "User");
            myUpiId = sharedPreferences.getString("user_upi_id", "user@upi");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUpiIdView = findViewById(R.id.tvUpiId);
        
        if (tvUserName != null) tvUserName.setText(myName);
        if (tvUpiIdView != null) tvUpiIdView.setText(myUpiId);

        String qrContent = "upi://pay?pa=" + myUpiId + "&pn=" + myName;

        ImageView ivQrCode = findViewById(R.id.ivQrCode);
        qrBitmap = generateQrCode(qrContent);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }
    }

    private Bitmap generateQrCode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveQrToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "VoltPay_QR_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VoltPay");
        }
        
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                OutputStream out = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Toast.makeText(this, "QR saved to Gallery!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Fallback for older devices if insert fails
            @SuppressWarnings("deprecation")
            String savedUrl = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "VoltPay QR", "My VoltPay QR Code");
            if (savedUrl != null) {
                Toast.makeText(this, "QR saved to Gallery!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save QR", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareQrCode(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "my_qr.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (IOException e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Failed to share QR code", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
