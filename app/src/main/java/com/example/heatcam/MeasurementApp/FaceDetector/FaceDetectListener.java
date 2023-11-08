package com.example.heatcam.MeasurementApp.FaceDetector;

import android.graphics.Bitmap;

import com.google.mlkit.vision.face.Face;

public interface FaceDetectListener {

    void faceDetected(Face face, Bitmap originalCameraImage);
    void faceNotDetected();
}
