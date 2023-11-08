package com.example.heatcam.MeasurementApp.OLD;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.heatcam.MeasurementApp.FaceDetector.VisionProcessorBase;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

/*
    Modified from https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/facedetector/FaceDetectorProcessor.java

 */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {

    private static final String TAG = "FaceDetectorProcessor";

    private final FaceDetector detector;

    //private LiveCameraActivity a;

    private User_result userResult;


    public FaceDetectorProcessor(Context context, FaceDetectorOptions options, User_result userResult) {
        super(context);
        Log.v(MANUAL_TESTING_LOG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        //this.a = (LiveCameraActivity) context;
        this.userResult = userResult;
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
    protected void onSuccess(@NonNull List<Face> faces, Bitmap originalCameraImage) {
        if (faces.size() > 0) {
            for (Face face : faces) {

                int id = face.getTrackingId();
                PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

                //float faceDist = userResult.calculateFaceDistance(leftEyeP, rightEyeP);

                userResult.updateDetectedFace(face);

            }
        } else {
            userResult.noFaceDetected();
        }
    }


    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
