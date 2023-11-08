package com.example.heatcam;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.HighResolutionCamera;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.example.heatcam.MeasurementApp.Utils.ImageUtils;

import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.*;

public class LeptonCameraTest {

    @Test
    public void parseData() {
        int width = 160;
        int height = 120;
        byte[] testData = generateTestData((width + 4), height);
        LeptonCamera lc = new HighResolutionCamera();
        Vector<Integer> colorTable = ImageUtils.createColorTable();


        assertTrue(lc.parseData(testData));
        int rowStart = 0;
        for(int i = 0; i < height; i++) {
            rowStart = i*(width + 4);
            for(int j = 0; j < width; j++) {
                int dataInd = rowStart + j + 4;
                int color = colorTable.elementAt(testData[dataInd] & 0xff);
                assertEquals(color, lc.getRawFramePixel(j, i));
            }
        }

        assertEquals(0x78, lc.rawTelemetry[18]);
        assertEquals(0x5F, lc.rawTelemetry[19]);
        assertEquals(0x75, lc.rawTelemetry[21]);
        assertEquals(0x77, lc.rawTelemetry[22]);

    }

    private byte[] generateTestData(int rawWidth, int rawHeight) {
        int telemetryWidth = 118;
        byte[] testData = new byte[rawHeight*rawWidth + telemetryWidth];

        // add image pixel data
        for(int rowIndex = 0; rowIndex < rawHeight; rowIndex++) {
            int row = rowIndex*rawWidth;
            // set line starting bytes
            for(int lineIndex = 0; lineIndex  < 3; lineIndex++) {
                testData[row + lineIndex] = (byte) 0xff;

            }
            // set row number
            testData[row + 3] = (byte) (row / 164);
            // set pixel data
            for(int lineIndex = 4; lineIndex < rawWidth; lineIndex++) {
                testData[row + lineIndex] = (byte) (lineIndex - 4);
            }
        }

        // add telemetry data
        int startIndex = 164 * 120;
        // set line starting bytes
        for(int lineIndex = 0; lineIndex  < 3; lineIndex++) {
            testData[startIndex+ lineIndex] = (byte) 0xff;
        }
        // set telemetry line number
        testData[startIndex + 3] = 120;
        startIndex += 4;
        // max raw 30815
        testData[startIndex + 18] = (byte) 0x78;
        testData[startIndex + 19] = (byte) 0x5F;
        // min raw 29815
        testData[startIndex + 21] = (byte) 0x75;
        testData[startIndex + 22] = (byte) 0x77;
        return testData;
    }
}