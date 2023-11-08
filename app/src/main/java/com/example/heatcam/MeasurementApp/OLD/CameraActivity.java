package com.example.heatcam.MeasurementApp.OLD;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.Fragments.CameraTest.LogView;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.HighResolutionCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolutionCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.R;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CameraActivity extends Fragment implements CameraListener {

    private LeptonCamera camera;
    SerialPortModel sModel;

    private TextView txtView;
    private Button scanBtn;
    private Button analysisBtn;
    private Button testBtn;
    private Button videoBtn;
    private Button cameraLayoutBtn;
    private Button tempBtn;
    private ToggleButton recordBtn;
    private ImageView imgView;
    private Spinner testDataSpinner;

    private TestFileReader testFileReader;

    private FaceDetector detector;

    private SparseArray<Face> faces;

    // test data variales
    private File testFile;
    private BufferedWriter writer;
    private String testDataFileName = "";
    private final String testDataPath = "test_data/";



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.old_activity_camera_fragment, container, false);
        txtView = (TextView) view.findViewById(R.id.textView);
        scanBtn = (Button) view.findViewById(R.id.scanBtn);
        analysisBtn = (Button) view.findViewById(R.id.analysisBtn);
        testBtn = (Button) view.findViewById(R.id.testBtn);
        videoBtn = (Button) view.findViewById(R.id.videoBtn);
        cameraLayoutBtn = (Button) view.findViewById(R.id.cameraLayoutBtn);
        imgView = (ImageView) view.findViewById(R.id.imageView);
        recordBtn = view.findViewById(R.id.recordBtn);
        testDataSpinner = view.findViewById(R.id.test_data_spinner);
        initTestDataSpinner();
        // user temperature result
        tempBtn = (Button) view.findViewById(R.id.butTemp);

        detector = new FaceDetector.Builder(view.getContext())
                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        faces = new SparseArray<>();

        camera = new LowResolutionCamera();
        camera.setCameraListener(this);
        //sModel = new SerialPortModel(this, camera);
        sModel = SerialPortModel.getInstance();
        sModel.setCamListener(this);
        sModel.setSioListener(camera);

        // register intent receiver for request
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        getContext().registerReceiver(sModel, filter);

        // camera.setListener(this);
        LeptonCamera testCam = new HighResolutionCamera();
        testCam.setCameraListener(this);
        testFileReader = new TestFileReader(view.getContext(), testCam);

        scanBtn.setOnClickListener(v -> sModel.scanDevices(requireContext()));
        analysisBtn.setOnClickListener(v -> sModel.toggleAnalysisMode());
        testBtn.setOnClickListener(v -> testFileReader.readTestFile(testDataPath + testDataFileName));
        videoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.super.getContext(), VideoActivity.class);
            startActivity(intent);
        });

        cameraLayoutBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.super.getContext(), LiveCameraActivity.class);
            startActivity(intent);
        });
        // to show user temperature result
        tempBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CameraActivity.super.getContext(), User_result.class);
            startActivity(intent);
        });

        imgView.setOnTouchListener((v, event) -> {
            camera.clickedHeatMapCoordinate(event.getX(), event.getY(), imgView.getWidth(), imgView.getHeight());
            return false;
        });

        recordBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                camera.setFrameListener(new ImageRecorder(this.getContext()));
            } else {
                camera.setFrameListener(null);
            }
        });

        view.findViewById(R.id.logs_button).setOnClickListener(v ->
                startActivity(new Intent(CameraActivity.super.getContext(), LogView.class)));

        //initWriter();

        return view;
    }

    private void initTestDataSpinner() {
        try {
            String[] lis = getContext().getAssets().list("test_data/");
            if(lis != null && lis.length != 0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, lis);
                adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                testDataSpinner.setAdapter(adapter);
                testDataFileName = lis[0];
                testDataSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        testDataFileName = (String) parent.getItemAtPosition(position);
                        System.out.println(testDataFileName);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initWriter() {
        // Creates a file in the primary external storage space of the
        // current application.
        // If the file does not exists, it is created.
        try {
            testFile = new File(getContext().getExternalFilesDir(null), "test_data/newdata.txt");
            if (!testFile.exists())
                testFile.createNewFile();

            // Adds a line to the file
            writer = new BufferedWriter(new FileWriter(testFile, true /*append*/));
        } catch (Exception e){
            Log.e("initWriter", "init failed");
        }
    }

    private void sendTestData(){
        testFileReader.readTestFile("data.txt");
    }

    @Override
    public void onStart() {
        sModel.scanDevices(getContext());
        super.onStart();
    }

    @Override
    public void setConnectingImage() {
        getActivity().runOnUiThread(() -> {
            imgView.setImageResource(R.drawable.connecting);
        });
    }

    @Override
    public void setNoFeedImage() {
        getActivity().runOnUiThread(() -> {
            imgView.setImageResource(R.drawable.noimage);
        });
    }

    @Override
    public void updateImage(Bitmap image) {
        getActivity().runOnUiThread(() -> { imgView.setImageBitmap(image);});
    }

    @Override
    public void updateText(String text) {
        getActivity().runOnUiThread(() -> {txtView.setText(text);});
    }

    @Override
    public void disconnect() {
        try {
            sModel.disconnect();
            setNoFeedImage();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        // muutetaan bitmap frameks
        Frame output = new Frame.Builder().setBitmap(image).build();
        // täs tunnistetaan framesta kasvot
        // https://developers.google.com/android/reference/com/google/android/gms/vision/face/FaceDetector#detect(com.google.android.gms.vision.Frame)
        faces = detector.detect(output);
        if (faces.size() > 0) {
            Face face = faces.valueAt(0);
            // tehään bitmap johon piirretään sit pistettä koordinaateista
            // joudutaan kopioimaan bitmap et saadaan tehtyä siitä mutable
            Bitmap bMap = image.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bMap);
            canvas.drawCircle(face.getPosition().x, face.getPosition().y, 1, new Paint(Paint.ANTI_ALIAS_FLAG));
            updateImage(bMap);
        } else {
            updateImage(image);
        }

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imgView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imgView.getLayoutParams().width = 0;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            imgView.getLayoutParams().height = 0;
            imgView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    public void writeToFile(byte[] data) {
        try {


            for(int i = 0; i <data.length; i++) {
                int value = data[i];
                if (i % 164 == 0) {
                    writer.write("\n");
                }
                writer.write(value + " ");
            }


            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{testFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the TestFile.txt file.");
        }
    }
}
