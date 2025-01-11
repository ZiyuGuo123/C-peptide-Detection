package com.example.c_peptidedetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ImageSelectionActivity";
    private static final String PREFS_NAME = "ImageSelectionPrefs";
    private static final String KEY_IMAGE_INDEX = "currentImageIndex";
    private static final String KEY_LAST_CONFIRM_TIME = "lastConfirmTime";

    private ImageView imageView;
    private OverlayView overlayView;
    private Bitmap selectedBitmap;
    private PointF start = new PointF();
    private PointF end = new PointF();
    private boolean isDrawing = false;

    private String newTextDirectoryPath;
    private int currentImageIndex;
    private Interpreter tflite;
    private long lastConfirmTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_selection);
        Log.d(TAG, "onCreate: Activity created");

        imageView = findViewById(R.id.imageView);
        overlayView = findViewById(R.id.overlayView);
        Button btnConfirm = findViewById(R.id.btnconfirm);
        Button btnCancel = findViewById(R.id.btnCancel);

        Intent intent = getIntent();
        Uri imageUri = intent.getParcelableExtra("imageUri");
        newTextDirectoryPath = intent.getStringExtra("newTextDirectoryPath");

        // 读取 SharedPreferences 中的 currentImageIndex
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentImageIndex = prefs.getInt(KEY_IMAGE_INDEX, 1);
        lastConfirmTime = prefs.getLong(KEY_LAST_CONFIRM_TIME, 0);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onCreate: Failed to load TensorFlow Lite model", e);
            return;
        }

        if (imageUri != null) {
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedBitmap);
                Log.d(TAG, "onCreate: Image loaded successfully");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "onCreate: Error loading image", e);
            }
        } else {
            Log.e(TAG, "onCreate: Image URI is null");
        }

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        start.set(event.getX(), event.getY());
                        isDrawing = true;
                        Log.d(TAG, "onTouch: ACTION_DOWN at (" + start.x + ", " + start.y + ")");
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (isDrawing) {
                            end.set(event.getX(), event.getY());
                            Log.d(TAG, "onTouch: ACTION_MOVE to (" + end.x + ", " + end.y + ")");
                            drawOverlay();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isDrawing) {
                            end.set(event.getX(), event.getY());
                            Log.d(TAG, "onTouch: ACTION_UP at (" + end.x + ", " + end.y + ")");
                            drawOverlay();
                            isDrawing = false;
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnConfirm: Confirm button clicked");
                processSelection();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnCancel: Cancel button clicked");
                overlayView.clearCircle();
            }
        });
    }

    private void drawOverlay() {
        float[] mappedStart = mapPointToBitmap(start.x, start.y);
        float[] mappedEnd = mapPointToBitmap(end.x, end.y);

        float radius = Math.max(Math.abs(start.x - end.x), Math.abs(start.y - end.y)) / 2;
        float centerX = (start.x + end.x) / 2;
        float centerY = (start.y + end.y) / 2;
        Log.d(TAG, "drawOverlay: Drawing circle at (" + centerX + ", " + centerY + ") with radius " + radius);
        overlayView.setCircle(centerX, centerY, radius);
    }

    private float[] mapPointToBitmap(float x, float y) {
        Matrix matrix = new Matrix();
        imageView.getImageMatrix().invert(matrix);

        float[] mappedPoint = new float[2];
        matrix.mapPoints(mappedPoint, new float[]{x, y});
        Log.d(TAG, "mapPointToBitmap: Mapped point (" + x + ", " + y + ") to (" + mappedPoint[0] + ", " + mappedPoint[1] + ")");
        return mappedPoint;
    }

    private Bitmap getCroppedBitmap() {
        float[] mappedStart = mapPointToBitmap(start.x, start.y);
        float[] mappedEnd = mapPointToBitmap(end.x, end.y);

        float radius = Math.max(Math.abs(mappedEnd[0] - mappedStart[0]), Math.abs(mappedEnd[1] - mappedStart[1])) / 2;
        float centerX = (mappedStart[0] + mappedEnd[0]) / 2;
        float centerY = (mappedStart[1] + mappedEnd[1]) / 2;

        Log.d(TAG, "getCroppedBitmap: Cropping bitmap with center (" + centerX + ", " + centerY + ") and radius " + radius);

        Bitmap output = Bitmap.createBitmap((int) (2 * radius), (int) (2 * radius), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        Rect rect = new Rect(0, 0, (int) (2 * radius), (int) (2 * radius));

        canvas.drawCircle(radius, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(selectedBitmap, new Rect((int) (centerX - radius), (int) (centerY - radius), (int) (centerX + radius), (int) (centerY + radius)), rect, paint);

        return output;
    }

    private void processSelection() {
        if (currentImageIndex > 5) {
            currentImageIndex = 1;
        }

        Bitmap croppedBitmap = getCroppedBitmap();
        File file = new File(newTextDirectoryPath, currentImageIndex + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.d(TAG, "Saved image to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error saving cropped image", e);
        }

        float predictedValue = performPrediction(croppedBitmap);

        File textFile = new File(newTextDirectoryPath, currentImageIndex + ".txt");
        try (FileOutputStream out = new FileOutputStream(textFile)) {
            out.write(String.valueOf(predictedValue).getBytes());
            Log.d(TAG, "Saved prediction to " + textFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error saving prediction", e);
        }

        String timestamp = getCurrentTimestamp();
        File timeFile = new File(newTextDirectoryPath, currentImageIndex + "_time.txt");
        try (FileOutputStream out = new FileOutputStream(timeFile)) {
            out.write(timestamp.getBytes());
            Log.d(TAG, "Saved timestamp to " + timeFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error saving timestamp", e);
        }

        lastConfirmTime = System.currentTimeMillis();


        currentImageIndex++;

        // 更新 SharedPreferences 中的 currentImageIndex
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_IMAGE_INDEX, currentImageIndex);
        editor.putLong(KEY_LAST_CONFIRM_TIME, lastConfirmTime);
        editor.apply();

        Intent resultIntent = new Intent(ImageSelectionActivity.this, ResultActivity.class);
        resultIntent.putExtra("newTextDirectoryPath", newTextDirectoryPath);
        resultIntent.putExtra("currentImageIndex", currentImageIndex - 1);
        resultIntent.putExtra("lastConfirmTime", lastConfirmTime);
        startActivity(resultIntent);
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private float performPrediction(Bitmap bitmap) {
        Mat img = new Mat();
        Utils.bitmapToMat(bitmap, img);

        if (img.empty()) {
            Log.e(TAG, "performPrediction: Failed to load image!");
            return 0.0f;
        }

        Mat grayImg = new Mat();
        Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_BGR2GRAY);

        Mat redChannel = new Mat();
        Mat greenChannel = new Mat();
        Mat blueChannel = new Mat();
        Core.extractChannel(img, blueChannel, 0);
        Core.extractChannel(img, greenChannel, 1);
        Core.extractChannel(img, redChannel, 2);

        double avgRed = Core.mean(redChannel).val[0];
        double avgGreen = Core.mean(greenChannel).val[0];
        double avgBlue = Core.mean(blueChannel).val[0];
        double avgGray = Core.mean(grayImg).val[0];

        double stdRed = calculateStdDev(redChannel);
        double stdGreen = calculateStdDev(greenChannel);
        double stdBlue = calculateStdDev(blueChannel);
        double stdGray = calculateStdDev(grayImg);

        Mat edges = new Mat();
        Imgproc.Canny(grayImg, edges, 100, 200);
        double edgeDensity = Core.countNonZero(edges) / (double) (grayImg.rows() * grayImg.cols());

        double entropyValue = calculateEntropy(grayImg);

        double prediction = predictAbsorbance(avgRed, avgGreen, avgBlue, avgGray, stdRed, stdGreen, stdBlue, stdGray, edgeDensity, entropyValue);
        return (float) prediction;
    }

    private double predictAbsorbance(double avgRed, double avgGreen, double avgBlue, double avgGray,
                                     double stdRed, double stdGreen, double stdBlue, double stdGray,
                                     double edgeDensity, double entropyValue) {
        float[] input = new float[]{
                (float) avgRed, (float) avgGreen, (float) avgBlue, (float) avgGray,
                (float) stdRed, (float) stdGreen, (float) stdBlue, (float) stdGray,
                (float) edgeDensity, (float) entropyValue
        };
        float[][] output = new float[1][1];
        tflite.run(input, output);
        return output[0][0];
    }

    private double calculateStdDev(Mat channel) {
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(channel, mean, std);
        return std.toArray()[0];
    }

    private double calculateEntropy(Mat grayImg) {
        double[] histogram = new double[256];
        for (int i = 0; i < grayImg.rows(); i++) {
            for (int j = 0; j < grayImg.cols(); j++) {
                int value = (int) grayImg.get(i, j)[0];
                histogram[value]++;
            }
        }

        double entropy = 0.0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                double p = histogram[i] / (grayImg.rows() * grayImg.cols());
                entropy -= p * Math.log(p) / Math.log(2);
            }
        }
        return entropy;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("multiply10.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}




