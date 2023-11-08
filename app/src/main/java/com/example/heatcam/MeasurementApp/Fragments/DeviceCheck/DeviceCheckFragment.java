package com.example.heatcam.MeasurementApp.Fragments.DeviceCheck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort.SerialPortModel;
import com.example.heatcam.PrivateKeyboard.MainActivity;
import com.example.heatcam.R;

import java.util.concurrent.TimeUnit;


public class DeviceCheckFragment extends Fragment implements CameraListener {

    private final long timerMillis = TimeUnit.MINUTES.toMillis(30);
    private CountDownTimer timer;

    @SuppressLint("DefaultLocale")
    private String millisToMMSS(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return String.format(
                "%02d:%02d",
                minutes,
                seconds - TimeUnit.MINUTES.toSeconds(minutes)
        );
    }

    private float getMillisProgress(long target, long current) {
        return (1 - ((float) current / target)) * 100;
    }

    private void changeToIntroLayout() {
        Fragment f = new IntroFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                .replace(R.id.fragmentCamera, f, "intro").commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_check_layout, container, false);
        TextView mTextField = view.findViewById(R.id.mTextField);
        CircularProgressBar progressBar = view.findViewById(R.id.timeProgressBar);

        //moving background
        ConstraintLayout constraintLayout = (ConstraintLayout) view.findViewById(R.id.device_check_constraint_layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        timer = new CountDownTimer(timerMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                mTextField.setText(millisToMMSS(millisUntilFinished));
                progressBar.setProgress(getMillisProgress(timerMillis, millisUntilFinished));
            }
            public void onFinish() {
                changeToIntroLayout();
            }
        };
        timer.start();

        Button b = view.findViewById(R.id.skipButton);
        b.setOnClickListener((View v) -> {
            timer.cancel();
            timer.onFinish();
        });

        Button pkbTest = view.findViewById(R.id.pkbTest);
        pkbTest.setOnClickListener((View v) -> {
            startActivity(new Intent(getContext(), MainActivity.class));
        });
        pkbTest.setVisibility(View.INVISIBLE);

        checkCamera(view.getContext());

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
    public void setConnectingImage() {

    }

    @Override
    public void setNoFeedImage() {

    }

    @Override
    public void updateImage(Bitmap image) {

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
}
