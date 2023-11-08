package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import android.graphics.Bitmap;

public interface HybridImageListener {
    void onNewHybridImage(Bitmap image);
    void sendHeatmap(Bitmap image);
}
