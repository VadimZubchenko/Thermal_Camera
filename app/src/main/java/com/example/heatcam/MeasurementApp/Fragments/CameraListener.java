package com.example.heatcam.MeasurementApp.Fragments;

import android.graphics.Bitmap;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LowResolution16BitCamera;

public interface CameraListener {

    void setConnectingImage();
    void setNoFeedImage();
    void updateImage(Bitmap image);
    void updateText(String text);
    void disconnect();
    void maxCelsiusValue(double max);
    void minCelsiusValue(double min);
    void updateData(LowResolution16BitCamera.TelemetryData data);
    void detectFace(Bitmap image);
    void writeToFile(byte[] data);

}
