package com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.Utils.ImageUtils;
import com.example.heatcam.MeasurementApp.ThermalCamera.ThermalCamera;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.nio.charset.StandardCharsets;
import java.util.Vector;

public abstract class LeptonCamera implements ThermalCamera, SerialInputOutputManager.Listener {
    private static Vector<Integer> colorTable = ImageUtils.createColorTable();

    // max width and height of image
    private static int width;
    private static int height;
    private int telemetryWidth;

    // raw data arrays
    private static int[][] rawTempFrame;
    private int[][] rawFrame;
    int[] rawTelemetry; // default visibility for tests
    private byte[] rawData;
    public static final byte[] START_BYTES = new byte[]{-1, -1, -1};

    public int getRawDataIndex() {
        return rawDataIndex;
    }

    private int rawDataIndex = 0;

    int min = 0;
    int max = 0;

    private CameraListener cameraListener;
    private FrameListener frameListener;

    public LeptonCamera(int width, int height, int telemetryWidth) {
        this.width = width;
        this.height = height;
        this.telemetryWidth = telemetryWidth;
        this.rawFrame = new int[height][width];
        this.rawTempFrame = new int[height][width];
        this.rawData = new byte[height*(width+4) + telemetryWidth + 4];
        this.rawTelemetry = new int[telemetryWidth];
        System.out.println("created new leptoncamera");
    }

    public LeptonCamera(int width, int height, int telemetryWidth, int bits) {
        this.width = width;
        this.height = height;
        this.telemetryWidth = telemetryWidth;
        this.rawFrame = new int[height][width];
        this.rawTempFrame = new int[height][width];
        this.rawData = new byte[height*((width*(bits/8))+4) + telemetryWidth + 4];
        this.rawTelemetry = new int[telemetryWidth];
        System.out.println("created new leptoncamera");
    }

    public void clickedHeatMapCoordinate(float xTouch, float yTouch, float xImg, float yImg){
        float xScale = (float)this.width/xImg;
        float yScale = (float)this.height/yImg;

        int xPiste = (int)(xTouch*xScale);
        int yPiste = (int)(yTouch*yScale);

        System.out.println(rawFrame[yPiste][xPiste]);
    }

   // public abstract void onNewData(byte[] data);

    @Override
    public void onRunError(Exception e) {
        Log.d("heatcam", e.getMessage());
        cameraListener.disconnect();
    }

    public double kelvinToCelsius(int luku){
        return Math.round(((double)luku/100 - 273.15)*100.0)/100.0;//kahden desimaalin pyöristys
    }

    boolean parseData() {
        return parseData(rawData);
    }

    // parse byte data into rawFrame 2d array
    boolean parseData(byte[] data) {
        int bytesRead = data.length;
        int byteindx = 0;
        int lineNumber;
        int i;
        String rowBytes = new String(data, StandardCharsets.UTF_8);
        String pattern = new String(START_BYTES, StandardCharsets.UTF_8);
        byteindx = rowBytes.indexOf(pattern);

        for (i = byteindx; i < bytesRead; i += (width+4)) {
            lineNumber = data[i + 3];

            if (lineNumber < height) { // picture
                for (int j = 0; j < width; j++) {
                    int dataInd = i + j + 4;
                    if (dataInd < bytesRead) {
                        rawTempFrame[lineNumber][j] = (data[dataInd] & 0xff);
                        rawFrame[lineNumber][j] = colorTable.elementAt(data[dataInd] & 0xff);
                    }
                }
            } else if (lineNumber == height) { // telemetry
                for (int j = 0; j < telemetryWidth; j++) {
                    rawTelemetry[j] = data[i + 4 + j];
                }
                return true;

            }
        }
        return false;
    }

    boolean parse16bitData() {
        return parse16bitData(rawData);
    }

    boolean parse16bitData(byte[] data) {
        int bytesRead = data.length;
        int byteindx = 0;
        int lineNumber;
        int i;
        String rowBytes = new String(data, StandardCharsets.UTF_8);
        String pattern = new String(START_BYTES, StandardCharsets.UTF_8);
        byteindx = rowBytes.indexOf(pattern);

        for(i = byteindx; i < bytesRead; i += (width*2+4)) { // row
            lineNumber = data[i + 3];
            if(lineNumber < height) {
                int colInd = 0;
                for (int j = 0; j < width*2; j+=2) {
                    int dataInd = i + j + 4;
                    if (dataInd < bytesRead) {
                        int val = (data[dataInd] & 0xff) + (data[dataInd+1] & 0xff)*256;
                        rawTempFrame[lineNumber][colInd] = (data[dataInd] & 0xff) + (data[dataInd+1] & 0xff)*256;
                        rawFrame[lineNumber][colInd++] = (data[dataInd] & 0xff) + (data[dataInd+1] & 0xff)*256;
                        if (val > max) {
                            max = val;
                            if(min == 0) {
                                min = val;
                            }
                        } else if (val < min) {
                            min = val;
                        }
                    }
                }
            } else if(lineNumber == height) {
                for (int j = 0; j < telemetryWidth; j++) {
                    rawTelemetry[j] = data[i + 4 + j];
                }
                return true;
            }
        }
        return false;
    }

    public static Vector<Integer> getColorTable() {
        return colorTable;
    }

    protected void extractRow(byte[] data) {
        System.arraycopy(data, 0, rawData, rawDataIndex, data.length);
    }

    protected Bitmap getBitmapInternal() {
        return ImageUtils.bitmapFromArray(rawFrame);
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int[][] getTempFrame(){ return rawTempFrame;}

    public int[][] getRawFrame(){ return rawFrame;}

    public void setRawTempFrame(int[][] data){rawTempFrame = data;}

    public void setRawDataIndex(int rawDataIndex) {
        this.rawDataIndex = rawDataIndex;
    }

    public CameraListener getCameraListener() {
        return cameraListener;
    }

    public void setCameraListener(CameraListener cameraListener) {
        this.cameraListener = cameraListener;
    }

    public FrameListener getFrameListener() {
        return frameListener;
    }

    @Override
    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }
    public byte[] getRawData() {
        return rawData;
    }

    int getRawFramePixel(int width, int height) {
        return rawFrame[height][width];
    }

}
