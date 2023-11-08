package com.example.heatcam.MeasurementApp.OLD;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.heatcam.MeasurementApp.OLD.HeadTiltListener;

public class HeadTiltAnalyzer {

    private TextView xRotationTeksti;
    private TextView yRotationTeksti;
    private ProgressBar xBar;
    private ProgressBar yBar;

    private HeadMoved headUp = new HeadMoved();
    private HeadMoved headDown = new HeadMoved();
    private HeadMoved headLeft = new HeadMoved();
    private HeadMoved headRight = new HeadMoved();
    private HeadTiltListener headTiltListener;

    //testailuun
    public HeadTiltAnalyzer(HeadTiltListener headTiltListener, TextView xRotationTeksti, TextView yRotationTeksti, ProgressBar xBar, ProgressBar yBar){
        this.xRotationTeksti = xRotationTeksti;
        this.yRotationTeksti = yRotationTeksti;
        this.xBar = xBar;
        this.yBar = yBar;
        this.headTiltListener = headTiltListener;
    }

    public HeadTiltAnalyzer(HeadTiltListener headTiltListener){
        this.headTiltListener = headTiltListener;
    }

    protected void setTilt(float rotX, float rotY){
        xRotationTeksti.setText("X: "+rotX);
        yRotationTeksti.setText("Y: "+rotY);

        int newX = (int)rotX+50;
        int newY = (int)rotY+50;

        yBar.setProgress(newY);
        xBar.setProgress(newX);

        if(newX >= 70) headUpCall();
        if(newX <= 40) headDownCall();
        if(newY >= 70) headRightCall();
        if(newY <= 30) headLeftCall();
    }

    boolean movementIsWithinTimeLimit(long time1, long time2){
        long diff = Math.abs(time1 - time2);
        return diff < 1000 && diff > 250;
    }
    void headDownCall(){
        headDown.time = System.currentTimeMillis();
        if(movementIsWithinTimeLimit(headDown.time, headUp.time))
            headTiltListener.answerYes();
    }
    void headUpCall(){
        headUp.time = System.currentTimeMillis();
        if(movementIsWithinTimeLimit(headDown.time, headUp.time))
            headTiltListener.answerYes();
    }
    void headLeftCall(){
        headLeft.time = System.currentTimeMillis();
        if(movementIsWithinTimeLimit(headLeft.time, headRight.time))
            headTiltListener.answerNo();
    }
    void headRightCall(){
        headRight.time = System.currentTimeMillis();
        if(movementIsWithinTimeLimit(headLeft.time, headRight.time))
            headTiltListener.answerNo();
    }

    class HeadMoved{
        long time = 0;
    }
}
