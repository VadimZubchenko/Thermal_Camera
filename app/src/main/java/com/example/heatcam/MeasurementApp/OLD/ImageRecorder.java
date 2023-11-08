package com.example.heatcam.MeasurementApp.OLD;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.FrameListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ImageRecorder implements FrameListener {

    private File file;
    private BufferedWriter writer;
    private Context context;
    private boolean fristFrame = true;
    public ImageRecorder(Context context) {
        this.context = context;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy-HHmm");
        String formatted = formatter.format(c.getTime());
        String fileName = "recording-" + formatted + ".txt";
        try {
            file = new File(context.getFilesDir(), fileName);
            writer = new BufferedWriter(new FileWriter(file, false));
        } catch (Exception e){
            Log.e("ImageWriter", "init failed");
        }
    }

    public void onNewFrame(byte[] data) {
        if (writer == null) return;

        try {
            if(fristFrame) {
                fristFrame = false;
                for(int i = 0; i <data.length; i++) {
                    int value = data[i];

                    if (i % 164 == 0 && i != 0) {
                        writer.write("\n");
                    }
                    writer.write(value + " ");
                }
            } else {
                for(int i = 0; i <data.length; i++) {
                    int value = data[i];

                    if (i % 164 == 0) {
                        writer.write("\n");
                    }
                    writer.write(value + " ");
                }
            }

            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(context,
                    new String[]{file.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the TestFile.txt file.");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        writer.close();
        super.finalize();
    }
}
