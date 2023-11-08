package com.example.heatcam.MeasurementApp.Fragments.CameraTest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridImageListener;
import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.OLD.TestFileReader;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.HighResolutionCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolutionCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridBitmapBuilder;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.HybridImageOptions;
import com.example.heatcam.R;


public class CameraTestFragment extends Fragment implements CameraListener, HybridImageListener {

    private SerialPortModel sModel;
    private TestFileReader testFileReader;

    private ImageView camFeed;

    private String testDataFileName = "maskilasit.txt";
    private final String testDataPath = "test_data/";

    private CameraListener listener = this;
    private LowResolution16BitCamera activeCam = null;

    private SensorManager sManager;

    private TextView textAzimuth;
    private TextView textPitch;
    private TextView textRoll;
    private TextView kerroinTeksti, resoTeksti;
    private HybridBitmapBuilder hybridBitmap;
    // Gravity rotational data
    private float gravity[];
    // Magnetic rotational data
    private float magnetic[];
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth z axis
    private float azimuth;
    // pitch x axis
    private float pitch;
    // roll y axis
    private float roll;

    private SeekBar sliderAngle;
    private SeekBar sliderSpeed;
    private SeekBar sliderCamMode;
    private SeekBar transparency;
    private int sliderAngleMin = 22;
    private int sliderSpeedMin = 2;
    private TextView angleText;
    private TextView speedText;
    private TextView camModeText;
    private TextView telemetryText;
    private int telemetryCount = 0;

    private CheckBox temperature, opacity, smooth, bounds;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.heatcam_camera_test_layout, container, false);

        camFeed = view.findViewById(R.id.camera_test_view);
        hybridBitmap = new HybridBitmapBuilder(this, view);
        sModel = SerialPortModel.getInstance();
        sModel.setCamListener(this);

        textAzimuth = view.findViewById(R.id.textAzimuth);
        textPitch = view.findViewById(R.id.textPitch);
        textRoll = view.findViewById(R.id.textRoll);
        sliderAngle = view.findViewById(R.id.seekBar);
        sliderSpeed = view.findViewById(R.id.seekBarSpeed);
        sliderCamMode = view.findViewById(R.id.seekBarCamMode);
        angleText = view.findViewById(R.id.textView6);
        speedText = view.findViewById(R.id.textViewSpeed);
        camModeText = view.findViewById(R.id.textViewCamMode);
        telemetryText = view.findViewById(R.id.textTelemetry);
        //liveFeed = view.findViewById(R.id.livefeed);
        kerroinTeksti = view.findViewById(R.id.kerroinText);
        resoTeksti = view.findViewById(R.id.resot);
        temperature = view.findViewById(R.id.temperature);
        transparency = view.findViewById(R.id.transparency);
        transparency.setProgress(HybridImageOptions.transparency);
        temperature.setChecked(HybridImageOptions.temperature);
        bounds = view.findViewById(R.id.facebounds);
        bounds.setChecked(HybridImageOptions.facebounds);
        smooth = view.findViewById(R.id.smooth);
        smooth.setChecked(HybridImageOptions.smooth);


        sManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        getContext().registerReceiver(sModel, filter);

        LeptonCamera testCam = new HighResolutionCamera();
        testCam.setCameraListener(this);
        testFileReader = new TestFileReader(view.getContext(), testCam);

        view.findViewById(R.id.camera_test_data_button).setOnClickListener(v -> {
            testFileReader.readTestFile(testDataPath + testDataFileName);
        });

        view.findViewById(R.id.ylos).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                HybridImageOptions.yOffset--;
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
                getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putInt("offsety", HybridImageOptions.yOffset).apply();
            }
            return false;
        });
        view.findViewById(R.id.alas).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                HybridImageOptions.yOffset++;
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
                getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putInt("offsety", HybridImageOptions.yOffset).apply();
            }
            return false;
        });
        view.findViewById(R.id.oikea).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                HybridImageOptions.xOffset++;
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
                getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putInt("offsetx", HybridImageOptions.xOffset).apply();
            }
            return false;
        });
        view.findViewById(R.id.vasen).setOnTouchListener((v, e)-> {
            if(e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_DOWN){
                HybridImageOptions.xOffset--;
                getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
                getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putInt("offsetx", HybridImageOptions.xOffset).apply();
            }
            return false;
        });
       /* view.findViewById(R.id.plus).setOnClickListener(v -> {
            ModifyHeatmap.setRes(1.1);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
        });
        view.findViewById(R.id.miinus).setOnClickListener(v -> {
            ModifyHeatmap.setRes(0.9);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(ModifyHeatmap.teksti()));
        });*/
        view.findViewById(R.id.scaleplus).setOnClickListener(v -> {
            hybridBitmap.setScale(1.1);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
            getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putFloat("scale", HybridImageOptions.scale).apply();
        });
        view.findViewById(R.id.scalemiinus).setOnClickListener(v -> {
            hybridBitmap.setScale(0.9);
            getActivity().runOnUiThread(() -> kerroinTeksti.setText(hybridBitmap.teksti()));
            getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putFloat("scale", HybridImageOptions.scale).apply();
        });
        view.findViewById(R.id.smooth).setOnClickListener(v -> {
            HybridImageOptions.smooth = ((CheckBox) v).isChecked();
            getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putBoolean("smooth", HybridImageOptions.smooth).apply();
        });
        transparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putInt("transparency", HybridImageOptions.transparency).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                HybridImageOptions.transparency = progress;
            }
        });
        view.findViewById(R.id.temperature).setOnClickListener(v -> {
            HybridImageOptions.temperature = ((CheckBox) v).isChecked();
            getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putBoolean("temperature", HybridImageOptions.temperature).apply();
        });
        view.findViewById(R.id.facebounds).setOnClickListener(v -> {
            HybridImageOptions.facebounds = ((CheckBox) v).isChecked();
            getContext().getSharedPreferences("heatmapPrefs", Context.MODE_PRIVATE).edit().putBoolean("facebounds", HybridImageOptions.facebounds).apply();
        });

        Spinner setting = view.findViewById(R.id.camera_setting_spinner);
        String[] list = new String[] {"24x32 16bit", "24x32 8bit", "160x120 8bit"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, list);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        setting.setAdapter(adapter);

        setting.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);
                disconnect();
                LeptonCamera cam = null;
                activeCam = null;
                SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
                switch (position) {
                    case 0 :
                        cam = new LowResolution16BitCamera();
                        activeCam = (LowResolution16BitCamera) cam;
                        activeCam.setMaxFilter(sp.getFloat(getString(R.string.preference_max_filter), -1));
                        activeCam.setMinFilter(sp.getFloat(getString(R.string.preference_min_filter), -1));
                        break;
                    case 1 :
                        cam = new LowResolutionCamera();
                        break;
                    case 2 :
                        cam = new HighResolutionCamera();
                        break;
                }
                if(cam != null) {
                    cam.setCameraListener(listener);
                    sModel.setSioListener(cam);
                    sModel.scanDevices(getContext());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EditText maxFilter = view.findViewById(R.id.edit_max_filter);
        SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
        float maxFilterVal = sp.getFloat(getString(R.string.preference_max_filter), 0);
        if(maxFilterVal > 0 ) {
            maxFilter.setText(Float.toString(maxFilterVal));
        }
        maxFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if(activeCam != null) {
                    float value = -1;
                    try {
                        value = Float.parseFloat(s.toString());

                    } catch(Exception e) {
                    }
                    getActivity().getPreferences(Context.MODE_PRIVATE)
                            .edit()
                            .putFloat(getString(R.string.preference_max_filter), value)
                            .apply();
                    activeCam.setMaxFilter(value);

                }
            }
        });
        EditText minFilter = view.findViewById(R.id.edit_min_filter);
        float minFilterVal = sp.getFloat(getString(R.string.preference_min_filter), 0);
        if(minFilterVal > 0 ) {
            minFilter.setText(Float.toString(minFilterVal));
        }
        minFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(activeCam != null) {
                    float value = -1;
                    try {
                        value = Float.parseFloat(s.toString());
                    } catch(Exception e) {

                    }
                    getActivity().getPreferences(Context.MODE_PRIVATE)
                            .edit()
                            .putFloat(getString(R.string.preference_min_filter), value)
                            .apply();
                    activeCam.setMinFilter(value);
                }
            }
        });

        sliderAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int valueSlider = sliderAngleMin;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueSlider = sliderAngleMin + seekBar.getProgress();
                angleText.setText("Angle: " + valueSlider);
                sModel.changeTiltAngle(valueSlider);
            }
        });

        sliderSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int speedValue = sliderSpeedMin;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                speedValue = sliderSpeedMin + seekBar.getProgress();
                speedText.setText("Speed " + speedValue);
                sModel.changeTiltSpeed(speedValue);

            }
        });

        sliderCamMode.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                camModeText.setText("Mode: " + seekBar.getProgress());
                sModel.changeVideoMode(seekBar.getProgress());
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        //disconnect();
        super.onPause();
        sManager.unregisterListener(myDeviceOrientationListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(myDeviceOrientationListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void setConnectingImage() {
        getActivity().runOnUiThread(() -> {

            camFeed.setImageResource(R.drawable.connecting);
        });
    }

    @Override
    public void setNoFeedImage() {
        getActivity().runOnUiThread(() -> {
            camFeed.setImageResource(R.drawable.noimage);
        });
    }

    @Override
    public void updateImage(Bitmap image) {
        sendHeatmap(image);
        //getActivity().runOnUiThread(() -> camFeed.setImageBitmap(image));
    }

    @Override
    public void updateText(String text) {
        getActivity().runOnUiThread(() -> ((TextView)getActivity().findViewById(R.id.camera_status_text)).setText(text));
    }

    @Override
    public void disconnect() {
        try {
            sModel.disconnect();
            setNoFeedImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void maxCelsiusValue(double max) {
        TextView text = getActivity().findViewById(R.id.camera_max_raw_value);
        if(text == null) return;
        getActivity().runOnUiThread(() -> text.setText((String.valueOf(max))));
    }

    @Override
    public void minCelsiusValue(double min) {
        TextView text = getActivity().findViewById(R.id.camera_min_raw_value);
        if(text == null) return;
        getActivity().runOnUiThread(() -> text.setText((String.valueOf(min))));
    }

    @Override
    public void detectFace(Bitmap image) {

    }

    @Override
    public void writeToFile(byte[] data) {

    }

    public void updateTelemetryText(String telemetry) {
        if (telemetryCount == 5) {
            getActivity().runOnUiThread(() -> telemetryText.setText(telemetry + "\n"));
            telemetryCount = 0;
        } else {
            getActivity().runOnUiThread(() -> telemetryText.append(telemetry + "\n"));
        }
        telemetryCount++;
    }

    public void updateOrientationText() {
        getActivity().runOnUiThread(() -> {
            textAzimuth.setText(String.format("Azimuth: %.02f", azimuth));
            textPitch.setText(String.format("Pitch: %.02f", pitch));
            textRoll.setText(String.format("Roll: %.02f", roll));
        });
    }

    private SensorEventListener myDeviceOrientationListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);


                azimuth = values[0] * 57.2957795f;
                pitch = values[1] * 57.2957795f;
                roll = values[2] * 57.2957795f;
                mags = null;
                accels = null;
                updateOrientationText();
            }
        }
    };

    @Override
    public void onNewHybridImage(Bitmap image) {
        getActivity().runOnUiThread(() -> camFeed.setImageBitmap(image));
    }

    @Override
    public void sendHeatmap(Bitmap image) {
        hybridBitmap.setHeatmap(image);
    }

    public void updateData(LowResolution16BitCamera.TelemetryData data) {
        getActivity().runOnUiThread(() -> telemetryText.setText(data.toString()));
    }
}
