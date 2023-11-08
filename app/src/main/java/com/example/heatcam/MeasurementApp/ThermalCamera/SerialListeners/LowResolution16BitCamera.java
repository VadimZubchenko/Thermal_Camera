package com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import com.example.heatcam.MeasurementApp.Fragments.CameraTest.CameraTestFragment;
import com.example.heatcam.MeasurementApp.Fragments.Measurement.MeasurementStartFragment;
import com.example.heatcam.MeasurementApp.Utils.HybridBitmapBuilder.ScaledHeatmap;

public class LowResolution16BitCamera extends LeptonCamera {

    public void setMaxFilter(float maxFilter) {
        this.maxFilter = maxFilter;
    }

    private float maxFilter = -1;

    public void setMinFilter(float minFilter) {
        this.minFilter = minFilter;
    }

    private float minFilter = -1;

    public LowResolution16BitCamera() {
        super(24, 32,32, 16);
    }

    private TelemetryData td;

    @Override
    public void onNewData(byte[] data) {
        if(getHeight() == data[3]) {
            extractRow(data);
            parse16bitData();
            setRawDataIndex(0);
            //int maxRaw = (rawTelemetry[0]&0xFF) + (rawTelemetry[1]&0xFF)*256;
            //int minRaw = (rawTelemetry[3]&0xFF) + (rawTelemetry[4]&0xFF)*256;
            td = new TelemetryData(rawTelemetry);

            int minFilterKelvin = min;
            int maxFilterKelvin = max;
            if(maxFilter > 0) {
                maxFilterKelvin = (int) ((maxFilter + 273.15) *100);
            }
            if(minFilter > 0) {
                minFilterKelvin = (int) ((minFilter + 273.15) *100);
            }
            //Bitmap bMap = convertTo8bit(29915, 30515);
            Bitmap bMap = convertTo8bit(min, max, minFilterKelvin, maxFilterKelvin);
            Matrix m = new Matrix();
            m.postRotate(180);
            bMap = Bitmap.createBitmap(bMap, 0,0, bMap.getWidth(), bMap.getHeight(), m, true);

            Bitmap scaledBMap = ScaledHeatmap.scaleHeatmap(min, max, minFilterKelvin, maxFilterKelvin, getRawFrame());

            if (getCameraListener() != null) {
                getCameraListener().updateImage(scaledBMap);
                getCameraListener().updateData(td);
                getCameraListener().maxCelsiusValue(kelvinToCelsius(max));
                getCameraListener().minCelsiusValue(kelvinToCelsius(min));
            }
            //getCameraListener().updateText(""+ kelvinToCelsius(maxRaw));
            max = 0;
            min = 0;
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }

    }


    private Bitmap convertTo8bit(int min, int max, int minFilter, int maxFilter) {
        int pix;
        int ind = 0;
        int[] colors = new int[getWidth() * getHeight()];
        for(int i = 0; i < getHeight(); i++) {
            for(int j = 0; j < getWidth(); j++) {
                int pixelKelvin = getRawFramePixel(j, i);
                if(pixelKelvin > maxFilter) {
                    pix = 255;
                } else if (pixelKelvin < minFilter) {
                    pix = 0;
                } else {
                    pix = ((pixelKelvin-min) * 255 )/ (max-min);
                    if(pix > 255) pix = 255;
                    else if(pix < 0) pix = 0;
                }

                colors[ind++] = getColorTable().elementAt(pix);
            }
        }
        return Bitmap.createBitmap(colors, getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    }

    public class TelemetryData {
        public int cTemp;
        public int refTemp;
        public int inVoltage;
        public int tiltAngle;
        public int sensor1;
        public int sensor2;
        public int servoCurrent;

        public TelemetryData(int[] rawTelemetry) {
            // Ref Temp counted from visible pixels
            cTemp = (rawTelemetry[6]&0xFF) + (rawTelemetry[7]&0xFF)*256;
            // Ref temp from temperature sensor  (TempCor = (float) cTempK / (float)   RefTemp
            // all send pixels have been multiplied with TempCor
            refTemp = (rawTelemetry[9]&0xFF) + (rawTelemetry[10]&0xFF)*256;
            //Input voltage to the device, should be 4000 to 4095, 4095 is 5.1V
            inVoltage = (rawTelemetry[12]&0xFF) + (rawTelemetry[13]&0xFF)*256;
            // 0 degrees is monitor fasing up, monitor fasing forward is 90 degrees monitor goes to 95 degrees, i.e fasing 5 degrees down.
            tiltAngle = (rawTelemetry[15]&0xFF) + (rawTelemetry[16]&0xFF)*256;
            // tilt force sensor, indicates if the is external force Sensor1 and Sensor2 indicate different directions of force
            // total force = Sensor1-Sensor2
            sensor1 = (rawTelemetry[18]&0xFF) + (rawTelemetry[19]&0xFF)*256;
            sensor2 = (rawTelemetry[21]&0xFF) + (rawTelemetry[22]&0xFF)*256;
            //indication of servo current consumption
            servoCurrent = (rawTelemetry[24]&0xFF) + (rawTelemetry[25]&0xFF)*256;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("cTemp: ").append(cTemp).append("\n");
            sb.append("refTemp: ").append(refTemp).append("\n");
            sb.append("inVoltage: ").append(inVoltage).append("\n");
            sb.append("tiltAngle: ").append(tiltAngle).append("\n");
            sb.append("sensor1: ").append(sensor1).append("\n");
            sb.append("sensor2: ").append(sensor2).append("\n");
            sb.append("servoCurrent: ").append(servoCurrent).append("\n");
            return sb.toString();
        }
    }

}
