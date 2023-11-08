package com.example.heatcam.MeasurementApp.Fragments.Result;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.InitializationException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.heatcam.MeasurementApp.FaceDetector.CameraXViewModel;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectListener;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectorProcessor;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementAccessObject;
import com.example.heatcam.MeasurementApp.FrontCamera.FrontCameraProperties;
import com.example.heatcam.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ResultFragment extends Fragment implements FaceDetectListener {

    private final String TAG = "ResultFragment";


    // min and max values for graph's yaxis
    private float YAXIS_MIN = 33f;
    private float YAXIS_MAX = 40f;

    private final int PREV_MEASUREMENT_COLOR = Color.rgb(36, 252, 223);
    private final int USER_MEASUREMENT_COLOR =  Color.rgb(36, 252, 223); //(25, 45, 223) (6, 95, 174)
    private final int HIGH_TEMP_LINE_COLOR = Color.rgb(175, 70, 70);
    private final int HIGH_TEMP_LINE_TEXT_COLOR = Color.rgb(129, 48, 48);
    private final int RISING_TEMP_LINE_COLOR = Color.rgb(235, 235, 91);
    private final int NORMAL_TEMP_LINE_COLOR = Color.rgb(30, 189, 24);

    // at which y coordinate to draw the horizontal line to indicate high/rising temp
    private float highTempLineValue = 38.1f;
    private float risingTempLineValue = 37.4f;
    private float normalTempLineValue = 35.5f;

    private TextView text, text1, text2;
    private ImageView imgView;

    private LineChart measuresChart;
    private MeasurementAccessObject measurementAccessObject;
    private JSONArray measurementsJSON;

    private ArrayList<Double> previouslyMeasuredTemps;
    private double userTemp;

    private LineData graphData;

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;

    private ScheduledThreadPoolExecutor idleExecutor;

    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.heatcam_result_fragment, container, false);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());

        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        text = view.findViewById(R.id.textView);
        text1 = view.findViewById(R.id.textView1);
        text2 = view.findViewById(R.id.textView2);
        text.setText(R.string.qr_instruction);

        measuresChart = view.findViewById(R.id.measuresChart);

        measurementAccessObject = new MeasurementAccessObject();


        // text1.setText(R.string.FeedBack);
        if (getArguments() != null && !getArguments().isEmpty()) {
            System.out.println(getArguments() + " argumentit");
            double temp = (double)getArguments().get("user_temp");
            double avgTemp = (double) getArguments().get("avg_user_temp");
            userTemp = avgTemp;
            //text1.setText("Your temp was: " + temp);
            //text1.append("\nYour avg temp was: " + avgTemp);
            if (37.4 > userTemp && userTemp >= 35.5) {
                text1.setText(R.string.msgNormTmprt);

            } else if (userTemp >= 37.4) {
                text1.setText(R.string.msgHightTmprt);
            } else {
                text1.setText(R.string.msgLowTmprt);
            }

        }
        text2.setText(R.string.title);

        // commented off to test graph from measurement data
        //imgView = view.findViewById(R.id.qr_code);
        //imgView.setImageResource(R.drawable.qr_code);

        previouslyMeasuredTemps = new ArrayList<>();
        getPreviousMeasurements();
        graphData = initChartData();
        // uncomment in the future
        /*
        if (previouslyMeasuredTemps.size() > 0) {
            if (checkForHighTemp()) {
                // display high temp layout
                text1.setText(R.string.msgHightTmprt);
            } else {
                // display normal temp layout
                text1.setText(R.string.msgNormTmprt);
            }
        }
         */
        initChart();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        getViewLifecycleOwner(),
                        provider -> {
                            cameraProvider = provider;
                            bindAllCameraUseCases();
                        }
                );

        return view;
    }

    private boolean checkForHighTemp() {
        if (previouslyMeasuredTemps != null && previouslyMeasuredTemps.size() > 0) {
            double avgFromPrevious = previouslyMeasuredTemps.stream()
                    .mapToDouble(v -> v)
                    .average()
                    .getAsDouble();
            highTempLineValue = (float) avgFromPrevious;
        }
        // probably need to change this logic
        return userTemp > highTempLineValue;
    }

    private void getPreviousMeasurements() {
        try {
            measurementsJSON = measurementAccessObject.read(getContext());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindFaceAnalysisUseCase();
        }
    }

    private void bindFaceAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisCase != null) {
            cameraProvider.unbind(analysisCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {
            FaceDetectorOptions faceDetectOptions = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.40f)
                    .enableTracking()
                    .build();

            imageProcessor = new FaceDetectorProcessor(getContext(), faceDetectOptions, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        analysisCase = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1, 1))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisCase.setAnalyzer(
                ContextCompat.getMainExecutor(getContext()),
                imageProxy -> {
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                    }
                }
        );

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisCase);
    }

    private void initChart() {
        XAxis xAxis = measuresChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(10f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineWidth(2f);
        xAxis.setGridLineWidth(3f);
        xAxis.setGridColor(Color.WHITE);
        xAxis.setDrawLabels(false);
        xAxis.setLabelCount(10);
        //xAxis.setDrawGridLines(false);

        YAxis yAxis = measuresChart.getAxisLeft();
        yAxis.setAxisMinimum(YAXIS_MIN);
        yAxis.setAxisMaximum(YAXIS_MAX);
        yAxis.setAxisLineWidth(2f);
        yAxis.setGridLineWidth(3f);
        yAxis.setGridColor(Color.WHITE);
        yAxis.setTextSize(20f);
        yAxis.setTextColor(Color.WHITE);
        //yAxis.setDrawLabels(false);
        //yAxis.setLabelCount(3);
        yAxis.setLabelCount((int) YAXIS_MAX);
        yAxis.setGranularityEnabled(true);
        yAxis.setGranularity(1.0f);
        yAxis.setValueFormatter(new YAxisValueFormatter());
        yAxis.setDrawGridLines(false);

        String highTempLineText = (String) getContext().getResources().getText(R.string.high_temp_line_text);
        LimitLine highTempLine = new LimitLine(highTempLineValue);
        highTempLine.setTextColor(HIGH_TEMP_LINE_TEXT_COLOR);
        highTempLine.setTextSize(18f);
        highTempLine.setLineWidth(3f);
        highTempLine.setLineColor(HIGH_TEMP_LINE_COLOR);
        yAxis.addLimitLine(highTempLine);

        LimitLine risingTempLine = new LimitLine(risingTempLineValue);
        risingTempLine.setLineWidth(3f);
        risingTempLine.setLineColor(RISING_TEMP_LINE_COLOR);
        yAxis.addLimitLine(risingTempLine);

        LimitLine normalTempLine = new LimitLine(normalTempLineValue);
        normalTempLine.setLineWidth(3f);
        normalTempLine.setLineColor(NORMAL_TEMP_LINE_COLOR);
        yAxis.addLimitLine(normalTempLine);


        measuresChart.setVisibleXRangeMaximum(10);
       // measuresChart.fitScreen();
        measuresChart.setDrawBorders(true);
        measuresChart.setBorderWidth(3f);
        measuresChart.setBorderColor(Color.WHITE);
        //measuresChart.setDrawGridBackground(true);
        measuresChart.getDescription().setEnabled(false);
        measuresChart.getAxisRight().setEnabled(false);
        measuresChart.getLegend().setTextSize(20f);

        measuresChart.setData(graphData);

        measuresChart.getLegend().setTextColor(Color.WHITE);
        measuresChart.setTouchEnabled(false);
    }

    private LineData initChartData() {
        ArrayList<Entry> measurements = new ArrayList<>();
        ArrayList<Entry> userMeasurement = new ArrayList<>();
        // colors for red green and blue, depending on y axis value for measurement
        // so the graph draws different color circles for measurements
        List<Integer> colors = new ArrayList<>();
        //Drawable testDrawable = new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        int z = 0;
        // we want to get only the last 11 measurements, the last will be the most recent measurement
        // meaning the last is what the user just measured
        if (measurementsJSON.length() > 11) { // if there are more than 11 measurements
            for (int x = measurementsJSON.length() - 11; x < measurementsJSON.length() - 1; x++) {
                try {
                    measurements.add(new Entry(z, (float) measurementsJSON.getJSONObject(x).getDouble("Measured")));
                    previouslyMeasuredTemps.add(measurementsJSON.getJSONObject(x).getDouble("Measured"));

                    // check if temps are lower/higher than what we've set in YAXIS_MIN and MAX
                    // so we can adjust the graphs min and max accordingly
                    if (measurementsJSON.getJSONObject(x).getDouble("Measured") < YAXIS_MIN) {
                        YAXIS_MIN = (float) Math.floor(measurementsJSON.getJSONObject(x).getDouble("Measured"));
                    }
                    if (measurementsJSON.getJSONObject(x).getDouble("Measured") > YAXIS_MAX) {
                        YAXIS_MAX = (float) Math.ceil(measurementsJSON.getJSONObject(x).getDouble("Measured"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                z++;
            }
        } else { // there are less than 11 measurements so we can just loop the json
            for (int x = 0; x < measurementsJSON.length() - 1; x++) {
                try {
                    measurements.add(new Entry(z, (float) measurementsJSON.getJSONObject(x).getDouble("Measured")));

                    // check if temps are lower/higher than what we've set in YAXIS_MIN and MAX
                    // so we can adjust the graphs min and max accordingly
                    if (measurementsJSON.getJSONObject(x).getDouble("Measured") < YAXIS_MIN) {
                        YAXIS_MIN = (float) Math.floor(measurementsJSON.getJSONObject(x).getDouble("Measured"));
                    }
                    if (measurementsJSON.getJSONObject(x).getDouble("Measured") > YAXIS_MAX) {
                        YAXIS_MAX = (float) Math.ceil(measurementsJSON.getJSONObject(x).getDouble("Measured"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                z++;
            }
        }

        // need to add the last measurement to its own arraylist
        // so we can color the last dot in the graph with different color
        // TODO: use userTemp variable from bundle arguments instead of reading JSON
        try {
            userMeasurement.add(new Entry(z, (float) measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured")));

            // check if temps are lower/higher than what we've set in YAXIS_MIN and MAX
            // so we can adjust the graphs min and max accordingly
            if (measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured") < YAXIS_MIN) {
                YAXIS_MIN = (float) Math.floor(measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured"));
            }
            if (measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured") > YAXIS_MAX) {
                YAXIS_MAX = (float) Math.ceil(measurementsJSON.getJSONObject(measurementsJSON.length()-1).getDouble("Measured"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

       // green circle: normal temp, 35.5–37.3°C
       // yellow circle: rising temp, 37.4–38°C
       // red circle: fever, 38.1–43.0°C
        for (Entry e : measurements) {
            if (e.getY() < 43.0f && e.getY() > 38.1f) {
                colors.add(Color.RED);
            } else if (e.getY() < 38.0f && e.getY() > 37.4f) {
                colors.add(Color.YELLOW);
            } else if (e.getY() < 37.3f && e.getY() > 35.5) {
                colors.add(Color.GREEN);
            } else {
                colors.add(Color.BLUE);
            }
        }

        LineDataSet lineDataSet;
        LineDataSet userDataSet;
        if (measuresChart.getData() != null && measuresChart.getData().getDataSetCount() > 0) {
            lineDataSet = (LineDataSet) measuresChart.getData().getDataSetByIndex(0);
            lineDataSet.setValues(measurements);
            userDataSet = (LineDataSet) measuresChart.getData().getDataSetByIndex(0);
            userDataSet.setValues(userMeasurement);
            measuresChart.getData().notifyDataChanged();
            measuresChart.notifyDataSetChanged();
        } else {
            // change style for measurements before the user
            String prevMeasurementLocalized = (String) getContext().getResources().getText(R.string.prev_measurements);
            lineDataSet = new LineDataSet(measurements, "");
            lineDataSet.setCircleColors(colors);
            lineDataSet.setCircleRadius(8f);
            //lineDataSet.setColors(colors);
            //lineDataSet.setCircleColor(PREV_MEASUREMENT_COLOR);
            lineDataSet.setValueTextSize(0f); // to draw only dots on the graph
            lineDataSet.setDrawCircleHole(false);
            lineDataSet.setDrawFilled(false);
            lineDataSet.setDrawValues(false);
            lineDataSet.setFormLineWidth(4f);
            lineDataSet.setFormSize(0f);
            lineDataSet.enableDashedLine(0, 1, 0);

            //change style for the measurement the user got
            String userMeasurementLocalized = (String) getContext().getResources().getText(R.string.user_measurement);
            userDataSet = new LineDataSet(userMeasurement, userMeasurementLocalized);
            userDataSet.setDrawCircleHole(false);
            userDataSet.setValueTextSize(0f); // to draw only dots on the graph
            userDataSet.setColor(USER_MEASUREMENT_COLOR);
            userDataSet.setCircleColor(USER_MEASUREMENT_COLOR);
            userDataSet.setCircleRadius(8f);
            userDataSet.setDrawFilled(false);
            userDataSet.setFormLineWidth(4f);
            userDataSet.setFormSize(20f);
        }
        ArrayList<ILineDataSet> dataSet = new ArrayList<>();
        dataSet.add(lineDataSet);
        dataSet.add(userDataSet);
        LineData data = new LineData(dataSet);
        return data;
    }

    @Override
    public void faceDetected(Face face, Bitmap originalCameraImage) {
        Size imgSize = new Size(originalCameraImage.getWidth(), originalCameraImage.getHeight());
        PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
        PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
        int minFaceDist = Integer.parseInt(sharedPrefs.getString("PREFERENCE_RESULT_IDLE_CANCEL_DISTANCE", "600"));
        try {
            float dist = FrontCameraProperties.getProperties().getDistance(imgSize, leftEyeP, rightEyeP);
            // TODO: try with different minFaceDist values
            if (dist < minFaceDist) {
                stopIdleExecutor();
            } else {
                startIdleExecutor();
            }
        } catch (InitializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void faceNotDetected() {
        startIdleExecutor();
    }

    private void startIdleExecutor() {
        if (idleExecutor == null) {
            idleExecutor = new ScheduledThreadPoolExecutor(1);
        }
        // schedule the layout change if there isn't already a task going for it
        if (idleExecutor.getTaskCount() == 0) {
            int seconds = Integer.parseInt(sharedPrefs.getString("PREFERENCE_SECONDS_TO_SWITCH_INTRO_RESULT", "10"));
            idleExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    changeLayout();
                }
            }, seconds, TimeUnit.SECONDS);
        }
    }

    private void stopIdleExecutor() {
        if (idleExecutor != null) {
            idleExecutor.shutdownNow();
            idleExecutor = null;
        }
    }

    private void changeLayout() {
        Fragment f = new IntroFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "menu").commit();
    }

    class YAxisValueFormatter extends ValueFormatter {

        // to make the graph's yaxis only show min and max value on the left side
        @Override
        public String getFormattedValue(float value) {
            if (value == YAXIS_MIN || value == YAXIS_MAX) {
                return super.getFormattedValue(value);
            } else {
                return "";
            }
        }
    }
}