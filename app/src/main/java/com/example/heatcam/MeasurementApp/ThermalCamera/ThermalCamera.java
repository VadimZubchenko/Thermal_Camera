package com.example.heatcam.MeasurementApp.ThermalCamera;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.FrameListener;

public interface ThermalCamera {
    void setFrameListener(FrameListener listener);
}
