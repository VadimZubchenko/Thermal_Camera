package com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;

public class HybridImageOptions {
    public static int transparency = 200;
    public static boolean smooth = true;
    public static boolean facebounds = true;
    public static boolean temperature = false;

    public static int xOffset = -32;
    public static int yOffset = -71;
    public static int scaledWidth = LeptonCamera.getWidth();
    public static int scaledHeight = LeptonCamera.getHeight();
    public static float scale = 8.79f;
    public static int resolutionMultiplier = 1;
    public static double distanceCorrection = 0.0033;

    public static int getScaledWidth() {
        if(scaledWidth == 0)
            scaledWidth = LeptonCamera.getWidth();
        return scaledWidth;
    }

    public static int getScaledHeight() {
        if(scaledHeight == 0)
            scaledHeight = LeptonCamera.getHeight();
        return scaledHeight;
    }
}

