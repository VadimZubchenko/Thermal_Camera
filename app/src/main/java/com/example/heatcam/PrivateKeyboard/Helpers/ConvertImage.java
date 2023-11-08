package com.example.heatcam.PrivateKeyboard.Helpers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.apache.commons.net.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConvertImage {

    public static String convertImageToString(String filepath){

        InputStream inputStream = null;

        String imageString = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            inputStream = new FileInputStream(filepath);

            byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = baos.toByteArray();

            imageString = Base64.encodeBase64String(imageBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                baos.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return imageString;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SdCardPath")
    public static void convertStringToImageByteArray(String imageString){

        OutputStream outputStream = null;
        byte [] imageInByteArray = Base64.decodeBase64(
                imageString);

        try {
            LocalDateTime now = LocalDateTime.now();
            String filename = now.toString() + ".png";
            outputStream = new FileOutputStream("/data/data/com.example.heatcam/files/" + filename);

            outputStream.write(imageInByteArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                if (outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
