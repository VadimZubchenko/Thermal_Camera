package com.example.heatcam.MeasurementApp.FaceDetector;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;


/*
    Modified from https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/VisionImageProcessor.java
 */
/** An interface to process the images with different vision detectors and custom image models. */
public interface VisionImageProcessor {

    /** Processes ImageProxy image data, e.g. used for CameraX live preview case. */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    void processImageProxy(ImageProxy image) throws MlKitException;

    /** Stops the underlying machine learning model and release resources. */
    void stop();
}
