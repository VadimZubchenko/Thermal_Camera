package com.example.heatcam.MeasurementApp.OLD;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.heatcam.MeasurementApp.FaceDetector.VisionProcessorBase;
import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementStartFragment;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/*
    Modified from https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/facedetector/FaceDetectorProcessor.java

 */
public class FacePositionProcessor extends VisionProcessorBase<List<Face>> {

    private static final String TAG = "FacePositionProcessor";

    private final FaceDetector detector;

    private MeasurementStartFragment msf;

    public FacePositionProcessor(Context context, FaceDetectorOptions options, MeasurementStartFragment msf) {
        super(context);
        detector = FaceDetection.getClient(options);
        this.msf = msf;
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
            msf.facePositionCheck(face, originalCameraImage.getWidth(), originalCameraImage.getHeight());
            msf.faceDetected(face);

        } else {
            msf.faceNotDetected();
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
