package com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class LowResolutionCamera extends LeptonCamera implements SerialInputOutputManager.Listener {

    public LowResolutionCamera() {
        super(24, 32, 24);
    }

    @Override
    public void onNewData(byte[] data) {

        if(getHeight() == data[3]) {
            extractRow(data);
            parseData();
            setRawDataIndex(0);
            int maxRaw = (rawTelemetry[0]&0xFF) + (rawTelemetry[1]&0xFF)*256;
            int minRaw = (rawTelemetry[3]&0xFF) + (rawTelemetry[4]&0xFF)*256;
            Log.d("heatcam", "on new Frame");
            Matrix m = new Matrix();
            m.postRotate(180);
            Bitmap bMap = getBitmapInternal();
            bMap = Bitmap.createBitmap(bMap, 0,0, bMap.getWidth(), bMap.getHeight(), m, true);
            getCameraListener().updateImage(bMap);
           // getCameraListener().updateText(""+ kelvinToCelsius(maxRaw));
            getCameraListener().maxCelsiusValue(kelvinToCelsius(maxRaw));
            getCameraListener().minCelsiusValue(kelvinToCelsius(minRaw));
            if(getFrameListener() != null) {
                getFrameListener().onNewFrame(getRawData());
            }
        } else {
            extractRow(data);
            setRawDataIndex(getRawDataIndex()+data.length);
        }
    }
}
