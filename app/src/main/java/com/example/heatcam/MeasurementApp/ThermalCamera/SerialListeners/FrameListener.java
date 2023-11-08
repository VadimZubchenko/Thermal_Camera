package com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners;

public interface FrameListener {
    void onNewFrame(byte[] data);
}
