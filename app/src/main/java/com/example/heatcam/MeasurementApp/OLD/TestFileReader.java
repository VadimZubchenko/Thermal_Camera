package com.example.heatcam.MeasurementApp.OLD;

import android.content.Context;

import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestFileReader {
    private Context context;
    private LeptonCamera camera;
    boolean lock = false;
    public TestFileReader(Context context, LeptonCamera camera){
        this.context = context;
        this.camera = camera;
    }

    public void readTestFile(String filu) {
        if(lock) return;
        lock = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = context.getApplicationContext().getAssets().open(filu);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;

                    while ((line = reader.readLine()) != null){
                        //jokainen line on framen yksi vaakarivi
                        String[] palat = line.split(" ");
                        byte[] tavut = new byte[palat.length];
                        for(int i = 0; i < palat.length; i++){
                            try {
                                tavut[i] = Byte.parseByte(palat[i]);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        }
                        camera.onNewData(tavut);
                        Thread.sleep(0, 200000);
                    }
                    lock = false;
                } catch (IOException | InterruptedException e ) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
