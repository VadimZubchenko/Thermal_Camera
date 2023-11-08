package com.example.heatcam.MeasurementApp.Fragments.IntroFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.InitializationException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.heatcam.MeasurementApp.FaceDetector.CameraXViewModel;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectListener;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectorProcessor;
import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.FrontCamera.FrontCameraProperties;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.Main.MainActivity;
import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementStartFragment;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridImageOptions;
import com.example.heatcam.PrivateKeyboard.Connection.ConnectionHandler;
import com.example.heatcam.PrivateKeyboard.Connection.ConnectionListener;
import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.Helpers.QRUtils;
import com.example.heatcam.R;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

public class IntroFragment extends Fragment implements FaceDetectListener, CameraListener, ConnectionListener {

    private final String TAG = "IntroFragment";

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;
    private ImageView heatkuva;
    private int minDistanceToMeasure = 500;
    //private TextView txtV;
    SerialPortModel serialPortModel;
    private static final float BITMAP_SCALE = 3.0f;
    private static final float BLUR_RADIUS = 25f;
    RenderScript rs;
    ScriptIntrinsicBlur theIntrinsic;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.heatcam_intro_fragment, container, false);
        SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        minDistanceToMeasure = Integer.parseInt(sharedPrefs.getString("PREFERENCE_MEASURE_START_MIN_DISTANCE", "500"));
        view.setKeepScreenOn(true);
        rs = RenderScript.create(getContext());
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
       // txtV = view.findViewById(R.id.txtDist);
        heatkuva = view.findViewById(R.id.heatkuva);
        HybridImageOptions.resolutionMultiplier = 4;

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //crop upper reference area
                ViewGroup.LayoutParams params = heatkuva.getLayoutParams();
                params.height = (int)(view.findViewById(R.id.ConstraintLayout).getHeight()*1.2);
                heatkuva.setLayoutParams(params);
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

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
        try {
            checkCamera(view.getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        serialPortModel = SerialPortModel.getInstance();
        serialPortModel.setCamListener(this);

        return view;
    }

    private void checkCamera(Context context) {
        SerialPortModel serialPortModel = SerialPortModel.getInstance();
        if(!serialPortModel.hasCamera()) {
            SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
            LowResolution16BitCamera cam = new LowResolution16BitCamera();
            cam.setMaxFilter(sharedPrefs.getFloat(getString(R.string.preference_max_filter), -1));
            cam.setMinFilter(sharedPrefs.getFloat(getString(R.string.preference_min_filter), -1));
            serialPortModel.setSioListener(cam);
            serialPortModel.scanDevices(context);
            serialPortModel.changeTiltSpeed(7);
        } else {
            serialPortModel.changeTiltAngle(75);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        QRUtils.connectedUuid = null;
        QRUtils.newUuid = null;
        ConnectionHandler cHandle = ConnectionHandler.getInstance();
        cHandle.setListener(this);
        try {
            cHandle.initConnection();
            ImageView qr = getActivity().findViewById(R.id.qr_code);
            getActivity().runOnUiThread(() -> qr.setImageBitmap(QRUtils.create()));
        } catch (Exception ignored) {/* couldn't connect to server */}

        bindAllCameraUseCases();
        System.out.println("ONRESUME");
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
        HybridImageOptions.resolutionMultiplier = 1;
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
                .setTargetResolution(new Size( 1, 1))
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

    @Override
    public void faceDetected(Face face, Bitmap originalCameraImage) {
        PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
        PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
        checkFaceDistance(leftEyeP, rightEyeP, originalCameraImage.getWidth(), originalCameraImage.getHeight());
    }

    @Override
    public void faceNotDetected() {

    }

    public void checkFaceDistance(PointF leftEye, PointF rightEye, int imgWidth, int imgHeight) {
        float dist = 0;
        try {
            dist = FrontCameraProperties.getProperties()
                    .getDistance(new Size(imgWidth, imgHeight), leftEye, rightEye);
        } catch (InitializationException e) {
            e.printStackTrace();
        }

        if (dist > 0 && dist < minDistanceToMeasure) {
            switchToMeasurementStartFragment();
        }
    }

    private void switchToMeasurementStartFragment() {

        Fragment f = new MeasurementStartFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "measure_start")
                .commit();
        MainActivity.setAutoMode(true);
    }
    public Bitmap blur(Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }
    @Override
    public void setConnectingImage() {

    }

    @Override
    public void setNoFeedImage() {

    }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> heatkuva.setImageBitmap(blur(image)));
    }

    @Override
    public void updateText(String text) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void maxCelsiusValue(double max) {

    }

    @Override
    public void minCelsiusValue(double min) {

    }

    @Override
    public void updateData(LowResolution16BitCamera.TelemetryData data) {

    }

    @Override
    public void detectFace(Bitmap image) {

    }

    @Override
    public void writeToFile(byte[] data) {

    }

    @Override
    public void onSendInputField(NewMessage message) {
        // not used
    }

    @Override
    public void onUpdateTiltAngle(TiltAngle message) {
        // not used
    }

    @Override
    public void onPressButton(TakingPicture message) {
        // not used
    }

    @Override
    public void onConfirmQRScan(ConfirmQRScan message) {
        QRUtils.connectedUuid = message.uuid;
        startActivity(new Intent(getContext(), com.example.heatcam.PrivateKeyboard.MainActivity.class));
    }
}
