package com.example.heatcam.MeasurementApp.OLD;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class FaceDetectionTool {



    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setMinFaceSize(0.35f)
                    .enableTracking()
                    .build();



   /* FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                    .build();*/

    FaceDetector faceDetector;
    LiveCameraActivity a;

    private volatile boolean isProcessing = false;

    public FaceDetectionTool(LiveCameraActivity a) {
        faceDetector = FaceDetection.getClient(options);
        this.a = a;
    }

    public void processImage(InputImage image, ImageProxy imageProxy) {

        isProcessing = true;

        Task<List<Face>> result =
                faceDetector.process(image)
                        .addOnSuccessListener(
                                faces -> {

                                    // Task completed successfully
                                    // [START_EXCLUDE]
                                    // [START get_face_info]
                                    if (faces.size() > 0) {
                                        a.incrementDetectedFrames();
                                        for (Face face : faces) {
                                            //System.out.println(face.getAllContours() + " CONTOUR");
                                            //System.out.println(face.getAllLandmarks() + " LANDMARK");
                                            Rect bounds = face.getBoundingBox();

                                            Bitmap b = image.getBitmapInternal();
                                            Canvas canvas = new Canvas(b);
                                            Paint paint = new Paint();
                                            paint.setColor(Color.GREEN);
                                            paint.setStyle(Paint.Style.STROKE);
                                            paint.setStrokeWidth(10);
                                            canvas.drawRect(bounds, paint);

                                            Paint contourPaint = new Paint();
                                            contourPaint.setColor(Color.RED);
                                            contourPaint.setStyle(Paint.Style.STROKE);
                                            contourPaint.setStrokeWidth(7.0f);

                                            a.headTilt(face.getHeadEulerAngleX(), face.getHeadEulerAngleY());

                                           Path facePath = new Path();

                                            for (FaceContour contour : face.getAllContours()) {
                                                if (contour.getFaceContourType() == FaceContour.FACE) {
                                                    facePath.moveTo(contour.getPoints().get(0).x, contour.getPoints().get(0).y);
                                                    //PointF temp = contour.getPoints().get(0);
                                                    for (PointF point : contour.getPoints()) {
                                                         facePath.lineTo(point.x, point.y);
                                                       // canvas.drawCircle(point.x, point.y, 5.0f, contourPaint);
                                                      //  canvas.drawLine(temp.x, temp.y, point.x, point.y, contourPaint);
                                                      //  temp = point;
                                                    }
                                                }

                                            }

                                            facePath.close();
                                            canvas.drawPath(facePath, contourPaint);


                                            a.drawImage(b);

                                            PointF leftEyeP = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                                            PointF rightEyeP = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();

                                            a.calculateFaceDistance(leftEyeP, rightEyeP);



                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                            // nose available):
                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                                            if (leftEar != null) {
                                                PointF leftEarPos = leftEar.getPosition();
                                            }

                                            // If classification was enabled:
                                            if (face.getSmilingProbability() != null) {
                                                float smileProb = face.getSmilingProbability();
                                            }
                                            if (face.getRightEyeOpenProbability() != null) {
                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
                                            }

                                            // If face tracking was enabled:
                                            if (face.getTrackingId() != null) {
                                                int id = face.getTrackingId();
                                            }
                                        }
                                    } else {
                                        a.drawImage(image.getBitmapInternal());
                                    }
                                    // [END get_face_info]
                                    // [END_EXCLUDE]
                                    //System.out.println("SUCCESS");
                                     //imageProxy.close();
                                })
                        .addOnCompleteListener(res -> {
                            imageProxy.close();
                            isProcessing = false;
                        })
                        .addOnFailureListener(
                                e -> {

                                    // Task failed with an exception
                                    // ...
                                    System.out.println("FAILURE");
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                    imageProxy.close();
                                    isProcessing = false;
                                });
    }

    public boolean isProcessing() {
        return isProcessing;
    }
}
