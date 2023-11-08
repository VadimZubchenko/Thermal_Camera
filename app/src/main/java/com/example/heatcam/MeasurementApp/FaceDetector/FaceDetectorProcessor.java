package com.example.heatcam.MeasurementApp.FaceDetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

    private static final String TAG = "IntroFaceDetectorProcessor";

    private final FaceDetector detector;

    private FaceDetectListener faceDetectListener;

    public FaceDetectorProcessor(Context context, FaceDetectorOptions options, FaceDetectListener faceDetectListener) {
        super(context);
        Log.v(MANUAL_TESTING_LOG, "Intro Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        this.faceDetectListener = faceDetectListener;
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(@NonNull List<Face> results, Bitmap originalCameraImage) {
        if (results.size() > 0) {
            Face face = results.get(0);
            faceDetectListener.faceDetected(face, originalCameraImage);
        } else {
            faceDetectListener.faceNotDetected();
        }

    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
