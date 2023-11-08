package com.example.heatcam.MeasurementApp.OLD;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.heatcam.MeasurementApp.FaceDetector.CameraXViewModel;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.Fragments.CameraTest.MenuFragment;
import com.example.heatcam.MeasurementApp.Fragments.Result.ResultFragment;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.MeasurementApp.Utils.RenderScriptTools;
import com.example.heatcam.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class User_result extends Fragment implements CameraListener {

    private final String TAG = "UserResult";

    // the value could be used of user temperature when userTemp is 100/real 39C etc.
    private double userTemp = 0, correcTemp = 0;
    double temp = 0;

    private Button buttonStart3, buttonQR;
    private TextView text, text2, textDistance, textMeasuring, debugs;
    private ImageView imgView;
    private boolean ready = false;
    private boolean hasMeasured = false;

    private int laskuri = 0;
    private int progress = 0;
    ProgressBar vProgressBar;
    SerialPortModel serialPortModel;

    private Executor executor = Executors.newSingleThreadExecutor();

    private RenderScriptTools rs;
    private FaceDetectTool fTool;

    private final int AVERAGE_EYE_DISTANCE = 63; // in mm
    private final int IMAGE_WIDTH = 480;
    private final int IMAGE_HEIGHT = 640;

    private float focalLength;
    private float sensorX;
    private float sensorY;

    private Rect naamarajat;
    float korkeussuhde = (float) LeptonCamera.getHeight()/(float)IMAGE_HEIGHT;//32/640
    float leveyssuhde = (float)LeptonCamera.getWidth()/(float)IMAGE_WIDTH;//24/480

    private AsyncTask tempMeasureTask;
    private HuippuLukema huiput = new HuippuLukema();
    private MutableLiveData<Face> detectedFace = new MutableLiveData<>();

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;
    private Preview previewCase;

    private int detectedFrames;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.old_activity_user_result, container, false);
        // prevent app from dimming
        view.setKeepScreenOn(true);

        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.ConstraintLayout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        // takes approx. 1min30sec to go from 2000 to 10
        detectedFrames = 2000;

        //new asyncTaskUpdateProgress().execute();
        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);
        buttonStart3 = view.findViewById(R.id.start3);
        buttonQR = view.findViewById(R.id.QRbutton);
        text = view.findViewById(R.id.textView);
        text2 = view.findViewById(R.id.textView2);
        //debugs = view.findViewById(R.id.debugs);
        text2.setText(R.string.title);
        textDistance = view.findViewById(R.id.textDistance);
        textMeasuring = view.findViewById(R.id.textMeasuring);
        imgView = view.findViewById(R.id.imageView);
        vProgressBar = view.findViewById(R.id.vprogressbar3);
        rs = new RenderScriptTools(view.getContext());
        fTool = new FaceDetectTool(this);

        android.hardware.Camera cam = getFrontCam();
        android.hardware.Camera.Parameters camP = cam.getParameters();
        focalLength = camP.getFocalLength();
        float angleX = camP.getHorizontalViewAngle();
        float angleY = camP.getVerticalViewAngle();
        sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
        sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);
        cam.stopPreview();
        cam.release();

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

       // startCamera();

        buttonStart3.setOnClickListener(v -> {
            ready = true;
            userTemp = 0;
            huiput = new HuippuLukema();
        });
        buttonQR.setOnClickListener(v -> {
            Fragment qr = new ResultFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentCamera, qr, "default").commit();
        });

        detectedFace.observe(getViewLifecycleOwner(), new Observer<Face>() {
            @Override
            public void onChanged(Face face) {
                int fId = face.getTrackingId();
                PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
                float faceDistance = calculateFaceDistance(leftEyeP, rightEyeP);

                if (!ready && !hasMeasured) {
                    userTemp = 0;
                }

                if (faceDistance < 300 && !hasMeasured) {
                    getActivity().runOnUiThread(() -> textDistance.setText("Distance: " + faceDistance));
                    ready = true;
                } else if (!hasMeasured) {
                    ready = false;
                    progress = 0;
                    laskuri = 0;
                    huiput = new HuippuLukema();
                    if (tempMeasureTask != null) {
                        tempMeasureTask.cancel(true);
                    }
                    getActivity().runOnUiThread(() -> textDistance.setText("Come closer.\nDistance: " + faceDistance));
                    getActivity().runOnUiThread(() -> textMeasuring.setText("Measuring temp: FALSE"));
                }
            }
        });


        return view;
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
           // bindPreviewUseCase();
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
                    .setMinFaceSize(0.35f)
                    .enableTracking()
                    .build();

            imageProcessor = new FaceDetectorProcessor(getContext(), faceDetectOptions, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        analysisCase =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();


        analysisCase.setAnalyzer(
                // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                // thus we can just runs the analyzer itself on main thread.
                ContextCompat.getMainExecutor(getContext()),
                imageProxy -> {
                    /*
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        boolean isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT;
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                        } else {
                            /*
                            graphicOverlay.setImageSourceInfo(
                                    imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                        }
                    }
                     */
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());

                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisCase);
    }

    @Override
    public void setConnectingImage() { }

    @Override
    public void setNoFeedImage() { }

    @Override
    public void updateImage(Bitmap image) {
        // copy image to make it mutable
        Bitmap bMap = image.copy(Bitmap.Config.ARGB_8888, true);

        if(naamarajat != null && ready){
            //huiput = new HuippuLukema();
            huiput = laskeAlue();

            Canvas canvas = new Canvas(bMap);
            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);

            Paint paint2 = new Paint();
            paint2.setColor(Color.MAGENTA);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setStrokeWidth(1);

            canvas.drawCircle(huiput.x, huiput.y, 1, paint2);
            Rect rect = new Rect();
            rect.set((int)(leveyssuhde*naamarajat.left),(int)(korkeussuhde*naamarajat.top),(int)(leveyssuhde*naamarajat.right),(int)(korkeussuhde*naamarajat.bottom));
            canvas.drawRect(rect, paint);
        }

        getActivity().runOnUiThread(() -> imgView.setImageBitmap(bMap));
    }

    public HuippuLukema laskeAlue(){

        int maxleveys = LeptonCamera.getWidth()-1;
        int maxkorkeus = LeptonCamera.getHeight()-1;

        int vasen = (int)(naamarajat.left*leveyssuhde); if(vasen < 0) vasen = 0; if(vasen > maxleveys) vasen = maxleveys;
        int oikea = (int)(naamarajat.right*leveyssuhde); if(oikea < 0) oikea = 0; if(oikea > maxleveys) oikea = maxleveys;
        int yla = (int)(naamarajat.top*korkeussuhde); if(yla < 0) yla = 0; if(yla > maxkorkeus) yla = maxkorkeus;
        int ala = (int)(naamarajat.bottom*korkeussuhde); if(ala < 0) ala = 0; if(ala > maxkorkeus) ala = maxkorkeus;

        int[][] tempFrame = LeptonCamera.getTempFrame();

        try{
            if(tempFrame != null /*&& tempFrame.length > maxkorkeus && tempFrame[tempFrame.length-1].length > maxleveys*/){
                for(int y = yla; y <= ala; y++){
                    for(int x = vasen; x <= oikea; x++){
                        double lampo = (tempFrame[y][x]- 27315)/100.0;
                        if(lampo > huiput.max){
                            huiput.max = lampo;
                            huiput.y = y;
                            huiput.x = x;
                        }
                    }
                }
            }
        }catch (Exception e){

        }
        return  huiput;
    }
    class HuippuLukema{
        int x = 0;
        int y = 0;
        double max = 0;
    }

    @Override
    public void updateText(String text) {
        getActivity().runOnUiThread(() -> text2.setText(text));
    }

    @Override
    public void disconnect() { }

    @Override
    public void maxCelsiusValue(double max) {
        if (ready) {
            getActivity().runOnUiThread(() ->textMeasuring.setText("Measuring temp: TRUE " + laskuri + "/100"));
            if (laskuri < 100) {
                huiput = laskeAlue();
                if (huiput.max > userTemp) {
                    userTemp = huiput.max;
            }
                laskuri++;
            } else {
                ready = false;
                hasMeasured = true;
                laskuri = 0;
                correcTemp = userTemp + 2.5;
                tempMeasureTask = new asyncTaskUpdateProgress().execute();

                if (37.5 >= correcTemp && correcTemp >= 35.5) {
                    text.setText(R.string.msgNormTmprt);

                } else if (correcTemp > 37.5) {
                    text.setText(R.string.msgHightTmprt);
                } else {
                    text.setText(R.string.msgLowTmprt);
                }
                getActivity().runOnUiThread(() -> textMeasuring.setText("Measuring temp: FALSE"));
            }
        }
    }

    @Override
    public void minCelsiusValue(double min) { }

    @Override
    public void updateData(LowResolution16BitCamera.TelemetryData data) {

    }

    @Override
    public void detectFace(Bitmap image) { }

    @Override
    public void writeToFile(byte[] data) { }


    public class asyncTaskUpdateProgress extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
           // buttonStart3.setClickable(true);


        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progress = 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            vProgressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            double tmin = 29, tmax = 39;

            updateText(Double.toString(correcTemp));
            temp = (userTemp - tmin)/(tmax - tmin)*100;
            while (progress < temp) {
                progress++;
                publishProgress(progress);
                SystemClock.sleep(1);
            }
            return null;
        }

    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {

                }
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
       // Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());


      // preview.setSurfaceProvider(cameraFeed.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);


    }

    private android.hardware.Camera getFrontCam() {
        android.hardware.Camera cam = null;
        android.hardware.Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
        int camCount = android.hardware.Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < camCount; camIdx++) {
            android.hardware.Camera.getCameraInfo(camIdx, camInfo);
            if (camInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = android.hardware.Camera.open(camIdx);
                } catch (Exception e) {
                    System.out.println("Failed to open cam");
                    e.printStackTrace();
                }
            }
        }
        return cam;
    }

    public float calculateFaceDistance(PointF leftEye, PointF rightEye) {
        float deltaX = Math.abs(leftEye.x - rightEye.x);
        float deltaY = Math.abs(leftEye.y - rightEye.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (IMAGE_WIDTH / deltaX);
        } else {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (IMAGE_HEIGHT / deltaY);
        }


        return  dist;
    }

    public void updateDetectedFace(Face face) {
        detectedFace.setValue(face);
        naamarajat = face.getBoundingBox();
        if (detectedFrames > 2000) {
            detectedFrames = 2000;
        } else {
            detectedFrames++;
        }
    }

    // gets called when face wasn't detected to decrement counter
    // and when counter value goes below 10 it will change fragment
    public void noFaceDetected() {
        detectedFrames--;
        if (detectedFrames < 10) {
            Fragment f = new MenuFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                    .replace(R.id.fragmentCamera, f, "menu").commit();
        }
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
            if (img != null) {

                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);

                InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                fTool.processImage(inputImage, image); // face detection
            }
        }
    }

}