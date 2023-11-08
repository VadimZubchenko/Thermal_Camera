package com.example.heatcam.MeasurementApp.Utils;

import com.example.heatcam.MeasurementApp.FrontCamera.FrontCameraProperties;

import java.util.Timer;
import java.util.TimerTask;

public class TiltAngleHandler {
    private Timer timer = new Timer();
    private boolean timerIsRunning = false;
    private boolean isAtTargetAngle = true;
    private int timerDelay = 200;
    private int currentTiltAngle = 0;
    private int targetTiltAngle = 0;

    public TiltAngleHandler() {

    }

    public int newCorrection(float distance, float imgHeight, float targetY, float currentY) {
        if(currentTiltAngle == targetTiltAngle && timerIsRunning && !isAtTargetAngle) {
            isAtTargetAngle = true;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerIsRunning = false;
                }
            }, timerDelay);
        }

        if (!timerIsRunning && isAtTargetAngle) {
            timerIsRunning = true;
            isAtTargetAngle = false;
            FrontCameraProperties props = FrontCameraProperties.getProperties();
            float objHeightPix = targetY - currentY;
            float objHeightSensor = (props.getSensorPhysicalSize().getHeight() * objHeightPix) / imgHeight;
            float realObjHeight = (distance * objHeightSensor) / props.getFocalLength();
            int angleCorrection = (int) Math.round(Math.sin(realObjHeight*0.7 / distance)*100) *100;
            int angle = currentTiltAngle - angleCorrection;
            if (angle < 2200) {
                targetTiltAngle = 2200;
            } else if (angle > 9500) {
                targetTiltAngle = 9500;
            } else {
                targetTiltAngle = angle;
            }

            return targetTiltAngle / 100;
        }

        return -1;
    }

    public void stop() {
        timer.cancel();
    }

    public void setTimerDelay(int timerDelay) {
        this.timerDelay = timerDelay;
    }

    public int getCurrentTiltAngle() {
        return currentTiltAngle;
    }

    public void setCurrentTiltAngle(int currentTiltAngle) {
        this.currentTiltAngle = currentTiltAngle;
    }

    public int getTargetTiltAngle() {
        return targetTiltAngle;
    }

    public void setTargetTiltAngle(int targetTiltAngle) {
        this.targetTiltAngle = targetTiltAngle;
    }


    public void setIsAtTargetAngle(boolean b) {
        isAtTargetAngle = b;
    }
}
