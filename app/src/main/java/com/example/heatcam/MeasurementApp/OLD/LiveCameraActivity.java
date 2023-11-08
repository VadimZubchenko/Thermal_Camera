package com.example.heatcam.MeasurementApp.OLD;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.example.heatcam.MeasurementApp.Utils.RenderScriptTools;
import com.example.heatcam.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class LiveCameraActivity extends AppCompatActivity implements HeadTiltListener {

    // https://medium.com/@akhilbattula/android-camerax-java-example-aeee884f9102
    // https://developer.android.com/training/camerax/preview#java

    private Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private final int IMAGE_WIDTH = 480;
    private final int IMAGE_HEIGHT = 640;

    private final String TAG = "LiveCameraActivity";

    private Button cameraBtn;
    private PreviewView cameraFeed;
    private ImageView cameraView;
    private TextView yRotationTeksti;
    private TextView xRotationTeksti;
    private TextView answer;
    private ProgressBar yBar;
    private ProgressBar xBar;
    private HeadTiltAnalyzer headTiltAnalyzer;

    private FaceDetectionTool fTool;

    private boolean startDetect = false;

    private PoseDetectionTool poseTool;
    private RenderScriptTools rs;
    private TextView posetext;
    private boolean poseStatus = false;

    private VideoView videoView;

    private MutableLiveData<Integer> detectedFrames = new MutableLiveData<>();

    private float focalLength = 0f;
    private final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private float angleX;
    private float angleY;

    private float sensorX;
    private float sensorY;

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;
    private Preview previewCase;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.old_activity_device_camera);

        detectedFrames.setValue(50);

        videoView = (VideoView) findViewById(R.id.videoView2);
        videoView.setMediaController(new MediaController(this));

        videoView.setOnCompletionListener(mp -> videoView.start());


        cameraBtn = (Button) findViewById(R.id.cameraBtn);
        cameraFeed = (PreviewView) findViewById(R.id.cameraFeed);
        cameraView = (ImageView) findViewById(R.id.imageCamera);
        posetext = findViewById(R.id.pose_text);
        yRotationTeksti = (TextView) findViewById(R.id.yrotation);
        xRotationTeksti = (TextView) findViewById(R.id.xrotation);
        xBar = (ProgressBar) findViewById(R.id.xBar);
        yBar = (ProgressBar) findViewById(R.id.yBar);
        answer = (TextView) findViewById(R.id.answer);

        rs = new RenderScriptTools(this);
        poseTool = new PoseDetectionTool(this);
        fTool = new FaceDetectionTool(this);

        headTiltAnalyzer = new HeadTiltAnalyzer(this, xRotationTeksti, yRotationTeksti, xBar, yBar);

        if (allPermissionsGranted()) {
            /*
            joudutaan käyttämään vanhaa camera APIa että saadaan arvot kasvojen etäisyyden mittaukseen
            https://ivanludvig.github.io/blog/2019/07/20/calculating-screen-to-face-distance-android.html
            https://github.com/IvanLudvig/Screen-to-face-distance
             */
            android.hardware.Camera cam = getFrontCam();
            android.hardware.Camera.Parameters camP = cam.getParameters();
            focalLength = camP.getFocalLength();
            angleX = camP.getHorizontalViewAngle();
            angleY = camP.getVerticalViewAngle();
            sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
            sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);
            cam.stopPreview();
            cam.release();

            /*
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

            new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                    .get(CameraXViewModel.class)
                    .getProcessCameraProvider()
                    .observe(
                            this,
                            provider -> {
                                cameraProvider = provider;
                                bindAllCameraUseCases();
                            }
                    );
             */
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        detectedFrames.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if (integer < 10) {
                    System.out.println("VIDEO START");
                    if (!videoView.isPlaying()) {
                        //String path = "android.resource://" + getPackageName() + "/" + R.raw.test;
                        //videoView.setVideoURI(Uri.parse(path));
                        videoView.setVisibility(View.VISIBLE);
                        videoView.start();
                    }
                } else if (integer > 80) {
                    videoView.stopPlayback();
                    videoView.setVisibility(View.INVISIBLE);
                    System.out.println(integer);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
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
            bindPreviewUseCase();
           // bindFaceAnalysisUseCase();
        }
    }


    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewCase != null) {
            cameraProvider.unbind(previewCase);
        }

        previewCase = new Preview.Builder().build();
        previewCase.setSurfaceProvider(cameraFeed.createSurfaceProvider());
        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewCase);
    }


    /*
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

            imageProcessor = new FaceDetectorProcessor(this, faceDetectOptions);
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
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());

                    }
                });

        cameraProvider.bindToLifecycle(/* lifecycleOwner=  this, cameraSelector, analysisCase);
    }
*/

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {

                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(executor, new MyAnalyzer());


        preview.setSurfaceProvider(cameraFeed.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);


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

    public void calculateFaceDistance(PointF leftEye, PointF rightEye) {
        float deltaX = Math.abs(leftEye.x - rightEye.x);
        float deltaY = Math.abs(leftEye.y - rightEye.y);

        float dist = 0f;
        if (deltaX >= deltaY) {
            dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (IMAGE_WIDTH / deltaX);
        } else {
           dist = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (IMAGE_HEIGHT / deltaY);
        }

        float finalDist = dist / 10;
        runOnUiThread(() -> posetext.setText(String.format("%.0f", finalDist) + "cm"));
    }



    @Override
    public void answerYes() {
        runOnUiThread(() -> answer.setText("kyllä"));
    }

    @Override
    public void answerNo(){
        runOnUiThread(() -> answer.setText("ei"));
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (!fTool.isProcessing()) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                @SuppressLint("UnsafeExperimentalUsageError") Image img = image.getImage();
                if (img != null) {
                    //Bitmap bMap = previewToBitmap(img);
                    //Bitmap bMap = cameraFeed.getBitmap();


                    Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);

                    InputImage inputImage = InputImage.fromBitmap(bMap, 0);
                    fTool.processImage(inputImage, image); // face detection


                 /* Eeron pose detection

                Bitmap bMap = rs.YUV_420_888_toRGB(img, img.getWidth(), img.getHeight(), rotationDegrees);
                InputImage poseImg = InputImage.fromBitmap(bMap, 0);
                poseTool.processImage(poseImg, image); // <-- tää funktio kutsuu lopuks drawImage()


                  */

                    if (rotationDegrees == 0) {
                        cameraView.setRotation(-90);
                    } else if (rotationDegrees == 270) {
                        cameraView.setRotation(0);
                    }
                    //  detectFace(bMap);
                }
                //image.close();
           } else {
                image.close();
           }
        }
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    // https://stackoverflow.com/a/58568495
    private Bitmap previewToBitmap (Image img) {
        Image.Plane[] planes = img.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public void drawImage(Bitmap image) {
        runOnUiThread(() -> cameraView.setImageBitmap(image));
    }

    public void updateText(boolean handUp) {
        if(handUp != poseStatus) { // ignore update if status hasn't changed
            poseStatus = handUp;
            String txt = "The hand is ";
            if(poseStatus) {
                txt += "up";
            } else {
                txt += "down";
            }
            final String status = txt;
            runOnUiThread(() -> posetext.setText(status));
        }
    }
    public void headTilt(float x, float y){
        headTiltAnalyzer.setTilt(x, y);
    }

    public void incrementDetectedFrames() {
        if (detectedFrames.getValue() > 100) {
            detectedFrames.setValue(100);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() + 1);
        }
    }

    public void decrementDetectedFrames() {
        if (detectedFrames.getValue() < 0) {
            detectedFrames.setValue(0);
        } else {
            detectedFrames.setValue(detectedFrames.getValue() - 1);
        }
    }
}
