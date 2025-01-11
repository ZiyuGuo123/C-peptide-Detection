package com.example.c_peptidedetection;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private ImageView imageView1, imageView2, imageView3, imageView4, imageView5;
    private TextView textView1, textView2, textView3, textView4, textView5;
    private TextView textTime, textPredection;
    private String newTextDirectoryPath;
    private int currentImageIndex;
    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private PointsGraphSeries<DataPoint> pointSeries;

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);
        imageView3 = findViewById(R.id.imageView3);
        imageView4 = findViewById(R.id.imageView4);
        imageView5 = findViewById(R.id.imageView5);
        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);
        textView5 = findViewById(R.id.textView5);
        textTime = findViewById(R.id.textTime);
        textPredection = findViewById(R.id.textPredection);
        graph = findViewById(R.id.graph);

        newTextDirectoryPath = getIntent().getStringExtra("newTextDirectoryPath");
        currentImageIndex = getIntent().getIntExtra("currentImageIndex", 1);

        series = new LineGraphSeries<>();
        pointSeries = new PointsGraphSeries<>();
        graph.addSeries(series);
        graph.addSeries(pointSeries);

        setupGraph();
        loadImagesAndPredictions();
    }

    private void setupGraph() {
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(5);


    }

    private void loadImagesAndPredictions() {
        List<Date> timeList = new ArrayList<>();
        List<Float> predictionList = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            File imageFile = new File(newTextDirectoryPath, i + ".png");
            File textFile = new File(newTextDirectoryPath, i + ".txt");
            File timeFile = new File(newTextDirectoryPath, i + "_time.txt");

            if (imageFile.exists() && textFile.exists() && timeFile.exists()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imageFile));
                    float pred = Float.parseFloat(readTextFile(textFile)) / 1000;
                    String prediction = String.format("%.2f", pred);
                    String timeString = readTextFile(timeFile);

                    Date time = dateFormat.parse(timeString);
                    if (time != null) {
                        timeList.add(time);
                        predictionList.add(pred);
                        int i1 = 3*(i-1);
                        setImageViewAndText(i, bitmap, prediction, timeString);
                    }
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error loading image or prediction", e);
                }
            }
        }

        if (!timeList.isEmpty()) {
            Date startTime = timeList.get(0);
            for (int i = 0; i < timeList.size(); i++) {
                long diffInMillis = timeList.get(i).getTime() - startTime.getTime();
                float diffInMinutes = diffInMillis / (1000 * 60.0f);

                addDataPointToGraph(diffInMinutes, predictionList.get(i));
            }
        }
    }

    private String readTextFile(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            int ch;
            while ((ch = fis.read()) != -1) {
                text.append((char) ch);
            }
        }
        return text.toString();
    }

    private void setImageViewAndText(int index, Bitmap bitmap, String prediction, String time) {
        switch (index) {
            case 1:
                imageView1.setImageBitmap(bitmap);
                textView1.setText(prediction);

                break;
            case 2:
                imageView2.setImageBitmap(bitmap);
                textView2.setText(prediction);
                break;
            case 3:
                imageView3.setImageBitmap(bitmap);
                textView3.setText(prediction);
                break;
            case 4:
                imageView4.setImageBitmap(bitmap);
                textView4.setText(prediction);
                break;
            case 5:
                imageView5.setImageBitmap(bitmap);
                textView5.setText(prediction);
                break;
        }
        textTime.setText("Detection Time: " + time);
        textPredection.setText("Concentration: " + prediction + " ng/ml");
    }

    private void addDataPointToGraph(float time, float prediction) {
        series.appendData(new DataPoint(time, prediction), true, 5);
        pointSeries.appendData(new DataPoint(time, prediction), true, 5);
        Log.d(TAG, "Added data point: time = " + time + ", prediction = " + prediction);
    }
}

