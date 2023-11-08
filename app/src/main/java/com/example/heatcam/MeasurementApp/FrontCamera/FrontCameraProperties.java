package com.example.heatcam.MeasurementApp.FrontCamera;

import android.graphics.PointF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Size;
import android.util.SizeF;

import androidx.camera.core.InitializationException;

public class FrontCameraProperties {
    public static final int AVERAGE_EYE_DISTANCE = 63; // in mm

    private float focalLength;
    private SizeF sensorSize;
    private float sensorX;
    private float sensorY;

    private static FrontCameraProperties INSTANCE;
    private boolean initialized = false;

    private FrontCameraProperties() {

    }

    public static FrontCameraProperties getProperties() {
        if (INSTANCE == null) {
            INSTANCE = new FrontCameraProperties();
        }
        return INSTANCE;
    }
    
    private String getFrontFacingCameraId(CameraManager cManager) throws CameraAccessException {
        for (final String cameraId : cManager.getCameraIdList()){
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
        }
        return null;
    }

    public FrontCameraProperties init(CameraManager manager) throws CameraAccessException {
        CameraCharacteristics c = manager.getCameraCharacteristics(getFrontFacingCameraId(manager));
        focalLength = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
        sensorSize = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        float angleX = (float) Math.atan(sensorSize.getWidth() / (2 * focalLength));
        float angleY = (float) Math.atan(sensorSize.getHeight() / (2 * focalLength));

        sensorX = (float) (Math.tan(Math.toRadians(angleX / 2)) * 2 * focalLength);
        sensorY = (float) (Math.tan(Math.toRadians(angleY / 2)) * 2 * focalLength);

        initialized = true;
        return this;
    }
    
    public float getDistance(Size imageSize, PointF leftEye, PointF rightEye) throws InitializationException {
        if (!initialized) throw new InitializationException("FrontCameraProperties not initialized.");

        float deltaX = Math.abs(leftEye.x - rightEye.x);
        float deltaY = Math.abs(leftEye.y - rightEye.y);

        float distance = 0f;
        if (deltaX >= deltaY) {
            distance = focalLength * (AVERAGE_EYE_DISTANCE / sensorX) * (imageSize.getWidth() / deltaX) / 100;
        } else {
            distance = focalLength * (AVERAGE_EYE_DISTANCE / sensorY) * (imageSize.getHeight() / deltaY) / 100;
        }
        
        return distance;
    }

    public SizeF getSensorPhysicalSize() {
        return sensorSize;
    }
    
    public float getSensorY() {
        return sensorY;
    }

    public float getSensorX() {
        return sensorX;
    }

    public float getFocalLength() {
        return focalLength;
    }
}
