//package com.example.c_peptidedetection;
//
//import android.Manifest;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.res.AssetFileDescriptor;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.widget.Button;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.content.FileProvider;
//import org.opencv.android.OpenCVLoader;
//import org.tensorflow.lite.Interpreter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//public class MainActivity extends AppCompatActivity {
//    private static final int REQUEST_IMAGE_CAPTURE = 1;
//    private static final int REQUEST_IMAGE_PICK = 2;
//    private static final int REQUEST_PERMISSIONS = 3;
//    private static final String TAG = "MainActivity";
//
//    private String currentPhotoPath;
//    private Interpreter tflite;
//
//    static {
//        if (!OpenCVLoader.initDebug()) {
//            Log.e("OpenCV", "Unable to load OpenCV");
//        } else {
//            Log.d("OpenCV", "OpenCV loaded successfully");
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        Button buttonStart = findViewById(R.id.buttonStart);
//        buttonStart.setOnClickListener(v -> showImageSourceDialog());
//
//        try {
//            tflite = new Interpreter(loadModelFile());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void showImageSourceDialog() {
//        Log.d(TAG, "Showing image source dialog");
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Choose Image Source")
//                .setItems(new String[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (which == 0) {
//                            openGallery();
//                        } else {
//                            dispatchTakePictureIntent();
//                        }
//                    }
//                });
//        builder.create().show();
//    }
//
//    private void openGallery() {
//        Log.d(TAG, "Opening gallery");
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, REQUEST_IMAGE_PICK);
//    }
//
//    private void dispatchTakePictureIntent() {
//        Log.d(TAG, "Checking permissions for camera and storage");
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
//                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "Requesting permissions");
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
//        } else {
//            Log.d(TAG, "Permissions already granted, taking picture");
//            takePicture();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_PERMISSIONS) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "Permissions granted, taking picture");
//                takePicture();
//            } else {
//                Log.e(TAG, "Permissions denied");
//            }
//        }
//    }
//
//    private void takePicture() {
//        Log.d(TAG, "Taking picture");
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            File photoFile = null;
//            try {
//                photoFile = createImageFile();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//                Log.e(TAG, "Error occurred while creating the File", ex);
//            }
//            if (photoFile != null) {
//                Uri photoURI = FileProvider.getUriForFile(this, "com.example.c_peptidedetection.fileprovider", photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//                Log.d(TAG, "Started camera activity");
//            } else {
//                Log.e(TAG, "Photo file is null");
//            }
//        } else {
//            Log.e(TAG, "No camera app found!");
//        }
//    }
//
//    private File createImageFile() throws IOException {
//        Log.d(TAG, "Creating image file");
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(null);
//        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
//        currentPhotoPath = image.getAbsolutePath();
//        Log.d(TAG, "Image file created: " + currentPhotoPath);
//        return image;
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG, "onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);
//        if (resultCode == RESULT_OK) {
//            Uri imageUri = null;
//            if (requestCode == REQUEST_IMAGE_CAPTURE) {
//                imageUri = Uri.fromFile(new File(currentPhotoPath));
//                Log.d(TAG, "Camera image saved at path: " + currentPhotoPath);
//            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
//                imageUri = data.getData();
//                Log.d(TAG, "Gallery image selected with URI: " + imageUri);
//            }
//
//            if (imageUri != null) {
//                launchImageSelectionActivity(imageUri);
//            } else {
//                Log.e(TAG, "Image URI is null");
//            }
//        } else {
//            Log.e(TAG, "Result code not OK: " + resultCode);
//        }
//    }
//
//    private void launchImageSelectionActivity(Uri imageUri) {
//        Log.d(TAG, "Launching ImageSelectionActivity with image URI: " + imageUri);
//        Intent intent = new Intent(this, ImageSelectionActivity.class);
//        intent.putExtra("imageUri", imageUri);
//        startActivity(intent);
//    }
//
//    private MappedByteBuffer loadModelFile() throws IOException {
//        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("multiply.tflite");
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
//}

package com.example.c_peptidedetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import org.opencv.android.OpenCVLoader;
import org.tensorflow.lite.Interpreter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 3;
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "ImageSelectionPrefs";
    private static final String KEY_IMAGE_INDEX = "currentImageIndex";

    private String currentPhotoPath;
    private String newTextDirectoryPath;
    private int currentImageIndex;
    private Interpreter tflite;
    private TextView textFilepath;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV");
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully");
        }
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textFilepath = findViewById(R.id.textFilepath);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentImageIndex = prefs.getInt(KEY_IMAGE_INDEX, 1);

        Button buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(v -> {
            showImageSourceDialog();
            textFilepath.setText(" ");
        });

        Button buttonNewText = findViewById(R.id.buttonNewText);
        buttonNewText.setOnClickListener(v -> {
            createNewTextDirectory();
            resetCurrentImageIndex();
            textFilepath.setText("New test is created successfully!");
        });

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showImageSourceDialog() {
        Log.d(TAG, "Showing image source dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image Source")
                .setItems(new String[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openGallery();
                        } else {
                            dispatchTakePictureIntent();
                        }
                    }
                });
        builder.create().show();
    }

    private void openGallery() {
        Log.d(TAG, "Opening gallery");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void dispatchTakePictureIntent() {
        Log.d(TAG, "Checking permissions for camera and storage");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        } else {
            Log.d(TAG, "Permissions already granted, taking picture");
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions granted, taking picture");
                takePicture();
            } else {
                Log.e(TAG, "Permissions denied");
            }
        }
    }

    private void takePicture() {
        Log.d(TAG, "Taking picture");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "Error occurred while creating the File", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.c_peptidedetection.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                Log.d(TAG, "Started camera activity");
            } else {
                Log.e(TAG, "Photo file is null");
            }
        } else {
            Log.e(TAG, "No camera app found!");
        }
    }

    private File createImageFile() throws IOException {
        Log.d(TAG, "Creating image file");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "Image file created: " + currentPhotoPath);
        return image;
    }

    private void createNewTextDirectory() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String dirName = "New_Text_" + timeStamp;
        File storageDir = new File(getExternalFilesDir(null), dirName);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        newTextDirectoryPath = storageDir.getAbsolutePath();
        Log.d(TAG, "New text directory created: " + newTextDirectoryPath);
    }

    private void resetCurrentImageIndex() {
        currentImageIndex = 1;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_IMAGE_INDEX, currentImageIndex);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (resultCode == RESULT_OK) {
            Uri imageUri = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imageUri = Uri.fromFile(new File(currentPhotoPath));
                Log.d(TAG, "Camera image saved at path: " + currentPhotoPath);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
                Log.d(TAG, "Gallery image selected with URI: " + imageUri);
            }

            if (imageUri != null) {
                launchImageSelectionActivity(imageUri);
            } else {
                Log.e(TAG, "Image URI is null");
            }
        } else {
            Log.e(TAG, "Result code not OK: " + resultCode);
        }
    }

    private void launchImageSelectionActivity(Uri imageUri) {
        Log.d(TAG, "Launching ImageSelectionActivity with image URI: " + imageUri);
        Intent intent = new Intent(this, ImageSelectionActivity.class);
        intent.putExtra("imageUri", imageUri);
        intent.putExtra("newTextDirectoryPath", newTextDirectoryPath);
        intent.putExtra("currentImageIndex", currentImageIndex);
        startActivity(intent);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("multiply.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}

