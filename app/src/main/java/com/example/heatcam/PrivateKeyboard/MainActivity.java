package com.example.heatcam.PrivateKeyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.heatcam.MeasurementApp.FaceDetector.CameraXViewModel;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectListener;
import com.example.heatcam.MeasurementApp.FaceDetector.FaceDetectorProcessor;
import com.example.heatcam.MeasurementApp.FaceDetector.VisionImageProcessor;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.PrivateKeyboard.Connection.Adapters.FormActivityAdapter;
import com.example.heatcam.PrivateKeyboard.Connection.ConnectionHandler;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.Helpers.SendMail;
import com.example.heatcam.R;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.example.heatcam.PrivateKeyboard.Data.EmailConfig.saveInstance;

public class MainActivity extends AppCompatActivity implements FaceDetectListener {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, -90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    String fileImage = null;
    private LinearLayout linearLayout;
    private ImageView profileImageView;
    private File visitorCardImageFile;
    // Deployment function URL: https://privatekeyboard.azurewebsites.net/api
    // Development function URL (example): http://192.168.1.149:7071/api

    ConnectionHandler cHandler;
    Button sendEmailButton;
    Button openCustomCameraButton;
    ImageView qrImage;

    private VisionImageProcessor imageProcessor;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysisCase;

    private ScheduledThreadPoolExecutor idleExecutor;

    private SharedPreferences sharedPrefs;

    private View decorView;

    private SerialPortModel serialPortModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pkb_activity_main);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        serialPortModel = SerialPortModel.getInstance();

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


        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.main_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();
    }

    //Reactive stuffs after getting back from other activities
    @Override
    protected void onResume() {
        super.onResume();
        bindAllCameraUseCases();

        //Status & Navigation bars hiding using decorView 1/2
        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if (visibility == 0)
                decorView.setSystemUiVisibility(hideSystemBars());
        });

        linearLayout = findViewById(R.id.input_layout);
        qrImage = findViewById(R.id.qrImage);
        profileImageView = findViewById(R.id.takenImage);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            try {
                String savedImagePath = bundle.getString("image_path");
                this.fileImage = savedImagePath;
                File file = new File(savedImagePath);
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                ((ImageView) findViewById(R.id.visitorImage)).setImageBitmap(bitmap);
                profileImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sendEmailButton = findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(view -> {
            updateVisitorCardFields();
            saveVisitorCard();
            sendEmail();
        });

        openCustomCameraButton = findViewById(R.id.buttonCam);
        openCustomCameraButton.setOnClickListener(v -> {
            saveInstance();
            Intent intent = new Intent(MainActivity.this, CustomCameraActivity.class);
            startActivity(intent);
        });

        if (!saveInstance.isEmpty()) {
            getInstance(saveInstance);
        }

        cHandler = ConnectionHandler.getInstance();
        cHandler.setListener(new FormActivityAdapter() {

            @Override
            public void onSendInputField(NewMessage message) {
                LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(message.targetInput);
                runOnUiThread(() -> ((EditText) inputField.getChildAt(1)).setText(message.text));
            }

            @Override
            public void onUpdateTiltAngle(TiltAngle message) {
                // the tilt angle motor only does angles between 22 and 95 (2200-9500)
                int angle;
                if (message.value < 22) {
                    angle = 22;
                } else if (message.value > 95) {
                    angle = 95;
                } else {
                    angle = message.value;
                }
                saveInstance.put("TextViewField-Tilt", String.valueOf(angle));
                TextView tiltTextView = findViewById(R.id.tiltValue);
                runOnUiThread(() -> tiltTextView.setText("Angle:" + angle));
                serialPortModel.changeTiltAngle(angle);
            }

            @Override
            public void onPressButton(TakingPicture message) {
                if (message.value.equals("on")) {
                    openCustomCameraButton.callOnClick();
                    //cHandler.stop();
                }else if (message.value.equals("sendEmail")) {
                    Log.d("call", "calllled");
                    runOnUiThread(() -> sendEmailButton.callOnClick());
                }
            }
        });
        //cHandler.initConnection();

        /*
        String functionUrl = "https://privatekeyboard.azurewebsites.net/api";
        HubConnection hubConnection = HubConnectionBuilder.create(functionUrl).build();

        hubConnection.on("sendInputField", (message) -> {
            Log.d("NewMessage", message.text);
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(message.targetInput);
            runOnUiThread(() -> ((EditText) inputField.getChildAt(1)).setText(message.text));
        }, NewMessage.class);

//        hubConnection.on("selectRadioGroup", (message) -> {
//            Log.d("NewCheckRadio", String.valueOf(message.targetRadioButton));
//            if (!message.sender.equals(QRUtils.connectedUuid)) return;
//
//            LinearLayout fieldLinearLayout = (LinearLayout) linearLayout.getChildAt(message.targetRadioGroup);
//            Log.d("NewMessageRadio", message.targetRadioGroup.toString());
//            RadioGroup radioGroup = (RadioGroup) fieldLinearLayout.getChildAt(1);
//            runOnUiThread(() -> ((RadioButton) radioGroup.getChildAt(message.targetRadioButton)).setChecked(true));
//        }, NewCheckRadio.class);

        hubConnection.on("updateTiltAngle", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("TiltAngle", String.valueOf(message.value));
            saveInstance.put("TextViewField-Tilt", String.valueOf(message.value));
            TextView tiltTextView = findViewById(R.id.tiltValue);
            runOnUiThread(() -> tiltTextView.setText("Angle:" + message.value));
        }, TiltAngle.class);

        hubConnection.on("pressButton", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("pressButton", String.valueOf(message.value));
            if (message.value.equals("on")) {
                openCustomCameraButton.callOnClick();
                hubConnection.stop();
            }else if (message.value.equals("sendEmail")) {
                Log.d("call", "calllled");
                runOnUiThread(() -> sendEmailButton.callOnClick());
            }
        }, TakingPicture.class);

        hubConnection.on("confirmQRScan", (message) -> {
            Log.d("ConfirmQRScan", message.uuid);
            if (!message.uuid.equals(QRUtils.newUuid)) return;
            // Set new QR bitmap to avoid duplicate connection
            QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
            // hide the QR view after connecting successfully
            qrImage.setVisibility(View.INVISIBLE);
            // Set connection ID
            QRUtils.connectedUuid = message.uuid;
        }, ConfirmQRScan.class);
        //Start the connection
        hubConnection.start().blockingAwait();
*/
        //Check if is there already a connection when go back from other activity
        //QRUtils.SetNewQRBitmap(findViewById(R.id.qrImage), linearLayout);
        /*
        Bitmap qrBitmap = QRUtils.create();
        qrImage.setImageBitmap(qrBitmap);
        if (QRUtils.connectedUuid != null) {
            qrImage.setVisibility(View.INVISIBLE);

        }*/
        qrImage.setVisibility(View.INVISIBLE);
    }

    //Status & Navigation bars hiding 2/3
    //this method gets called whenever the the window focus is changed
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(hideSystemBars());

        }

    }
    //Status & Navigation bars hiding 3/3
    private int hideSystemBars() {
        return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        cameraProvider.unbindAll();
        stopIdleExecutor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    private void saveInstance() {
//        saveInstance.put("RadioField-Sex", "No Response");
        saveInstance.clear();

        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            LinearLayout fieldLayout = (LinearLayout) linearLayout.getChildAt(i);
            String fieldTag = (String) linearLayout.getChildAt(i).getTag();
            if (!fieldTag.equals("hidden")) {
                if (fieldLayout.getChildAt(1) instanceof EditText) {
                    saveInstance.put("InputField-" + i + "-" + ((TextView) fieldLayout.getChildAt(0)).getText(), ((EditText) fieldLayout.getChildAt(1)).getText().toString().trim());
                    Log.d("InputField", "InputField-" + i + "-" + ((TextView) fieldLayout.getChildAt(0)).getText());
                } /*else if (fieldLayout.getChildAt(1) instanceof RadioGroup) {
//                    if (((RadioButton) ((RadioGroup) fieldLayout.getChildAt(1)).getChildAt(0)).isChecked())
//                        saveInstance.put("RadioField-Sex", "Male");
//                    else if (((RadioButton) ((RadioGroup) fieldLayout.getChildAt(1)).getChildAt(1)).isChecked())
//                        saveInstance.put("RadioField-Sex", "Female");
//                }*/

            }
        }
    }

    private void rotateImageToUpright(Bitmap source) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        float angle = ORIENTATIONS.get(rotation);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(MainActivity.this, "Landscape Mode", Toast.LENGTH_LONG).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(MainActivity.this, "Portrait Mode", Toast.LENGTH_LONG).show();
        }
        rotateImageToUpright(((BitmapDrawable) profileImageView.getDrawable()).getBitmap());
    }

    private void getInstance(HashMap<String, String> hashMap) {
        Set<String> keySet = hashMap.keySet();
        for (String key : keySet) {
            String[] arrOfStr = key.split("-", 3);
            Log.d("Instance",hashMap.get(key));

            if (arrOfStr[0].equals("InputField")) {
                LinearLayout inputField = (LinearLayout) linearLayout.getChildAt(Integer.parseInt(arrOfStr[1]));
                ((EditText) inputField.getChildAt(1)).setText(hashMap.get(key));
            } /*else if ((arrOfStr[0].equals("RadioField"))) {
//                RadioGroup radio = findViewById(R.id.radioSex);
//                switch (hashMap.get(key)) {
//                    case "Male":
//                        radio.check(R.id.radioMale);
//                        break;
//                    case "Female":
//                        radio.check(R.id.radioFemale);
//                        break;
//                }
            }*/ else {
                TextView tiltTextView = findViewById(R.id.tiltValue);
                tiltTextView.setText("Angle:" + hashMap.get(key));
            }
        }
    }

    private void updateVisitorCardFields() {
        ((TextView) findViewById(R.id.visitorName)).setText(((EditText) findViewById(R.id.fullNameText)).getText().toString());
        ((TextView) findViewById(R.id.hostName)).setText(((EditText) findViewById(R.id.hostNameText)).getText().toString());
        //((TextView) findViewById(R.id.companyName)).setText(((EditText) findViewById(R.id.companyNameText)).getText().toString());
        Date validDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        ((TextView) findViewById(R.id.visitDate)).setText(formatter.format(validDate));
    }

    private void saveVisitorCard() {
        View v1 = findViewById(R.id.visitorCard);
        v1.setDrawingCacheEnabled(true);
        v1.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        v1.layout(0, 0, v1.getMeasuredWidth(), v1.getMeasuredHeight());
        Bitmap testBitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);
        createVisitorCardFile(testBitmap);
    }

    private void createVisitorCardFile(Bitmap imageBitmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] fileData = stream.toByteArray();
            File path = new File(getApplicationContext().getFilesDir(), "Images");
            if (!path.exists()) {
                path.mkdirs();
            }
            visitorCardImageFile = new File(path, UUID.randomUUID().toString() + ".jpg");
            Log.d("CREATED_FILE", "createFile: " + visitorCardImageFile.getPath());
            FileOutputStream out = new FileOutputStream(visitorCardImageFile);
            out.write(fileData);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail() {
        //Getting content for clientEmail
        SendMail sm = new SendMail(this, ((EditText) findViewById(R.id.emailText)).getText().toString(), "Personal Information", visitorCardImageFile.getPath());
        sm.execute();
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

            imageProcessor = new FaceDetectorProcessor(this, faceDetectOptions, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        analysisCase = new ImageAnalysis.Builder()
                .setTargetResolution(new Size( 1, 1))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysisCase.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    try {
                        imageProcessor.processImageProxy(imageProxy);
                    } catch (MlKitException e) {
                        Log.e("PrivateKeyboardMainActivity", "Failed to process image. Error: " + e.getLocalizedMessage());
                    }
                }
        );

        cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, analysisCase);

    }

    private void startIdleExecutor() {
        if (idleExecutor == null) {
            idleExecutor = new ScheduledThreadPoolExecutor(1);
        }
        // schedule the layout change if there isn't already a task going for it
        if (idleExecutor.getTaskCount() == 0) {
            int seconds = Integer.parseInt(sharedPrefs.getString("PREFERENCE_SECONDS_TO_SWITCH_INTRO_PKBMAIN", "10"));
            idleExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        ConnectionHandler.getInstance().stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    changeToHeatcamIntro();
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

    private void changeToHeatcamIntro() {
        Intent intent = new Intent(this, com.example.heatcam.MeasurementApp.Main.MainActivity.class);
        intent.putExtra("skip_device_check", true);
        startActivity(intent);
        this.finish();
    }

    public void emailCallBack() {
        try {
            ConnectionHandler.getInstance().stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        changeToHeatcamIntro();
    }

    @Override
    public void faceDetected(Face face, Bitmap originalCameraImage) {
        stopIdleExecutor();
    }

    @Override
    public void faceNotDetected() {
        startIdleExecutor();
    }
}