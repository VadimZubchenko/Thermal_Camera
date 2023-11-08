package com.example.heatcam.MeasurementApp.ThermalCamera.SerialPort;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.example.heatcam.BuildConfig;
import com.example.heatcam.MeasurementApp.Fragments.CameraListener;
import com.example.heatcam.MeasurementApp.ThermalCamera.SerialListeners.LeptonCamera;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.Executors;

public class SerialPortModel extends BroadcastReceiver {

    private enum UsbPermission { Unknown, Requested, Granted, Denied };
    private final int ID_VENDOR_TEENSY = 5824;
    private final int ID_PRODUC_TEENSY = 1155;
    private final Vector<Integer> ALLOWED_VENDORS = new Vector<>(Arrays.asList(ID_VENDOR_TEENSY));
    private final Vector<Integer> ALLOWED_PRODUCTS = new Vector<>(Arrays.asList(ID_PRODUC_TEENSY));
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";

    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private SerialInputOutputManager usbIoManager;

    private CameraListener camListener;
    private SerialInputOutputManager.Listener sioListener;

    private boolean analysisMode;

    private static SerialPortModel instance;

    public static SerialPortModel getInstance() {
        if (instance == null) {
            instance = new SerialPortModel();
        }
        return instance;
    }

    public boolean hasCamera() {
        return sioListener != null;
    }

    public void setSioListener(SerialInputOutputManager.Listener sioListener) {
        this.sioListener = sioListener;
    }

    public void setCamListener(CameraListener camListener) {
        this.camListener = camListener;
        if(sioListener != null) {
            ((LeptonCamera) sioListener).setCameraListener(this.camListener);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
            synchronized (this) {
                scanDevices(context);
            }
        }

        if (INTENT_ACTION_GRANT_USB.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        connect();
                    }
                }
                else {
                    Log.d("permission", "permission denied for device " + device);
                }
            }
        }
    }


    public void scanDevices(Context context) {
        // Find all available drivers from attached devices.
        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(Objects.requireNonNull(manager));
        if (availableDrivers.isEmpty())  {
            Log.i("heatcam", "No USB serial drivers for the connected device");
            return;
        }
        driver = availableDrivers.get(0);
        UsbDevice foundDevice = driver.getDevice();

        // check that device is allowed
        if(ALLOWED_PRODUCTS.contains(foundDevice.getProductId()) && ALLOWED_VENDORS.contains(foundDevice.getVendorId())) {
            String deviceInfo = foundDevice.getProductName() + " - " + foundDevice.getManufacturerName() +
                    " - " + foundDevice.getProductId() + " - " + foundDevice.getVendorId();
            if (camListener != null) camListener.updateText(deviceInfo);

            // check device permission
            if (!manager.hasPermission(foundDevice)) {
                Log.d("heatcam", "No permission, requesting...");
                usbPermission = UsbPermission.Requested;
                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);

                // register intent receiver for request
                IntentFilter filter = new IntentFilter(INTENT_ACTION_GRANT_USB);
                context.registerReceiver(this, filter);

                // request usb permission
                manager.requestPermission(foundDevice, usbPermissionIntent);
            } else {
                connect();
            }
        } else {
            Log.i("scanDevices", "Not Allowed Device");
        }
    }

    private void connect() {
        if (usbIoManager != null) {
            return;
        }
        Log.d("hetacam","Connecting...");
        if (camListener != null) camListener.setConnectingImage();
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        usbSerialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            usbSerialPort.open(connection);
            usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            usbSerialPort.setDTR(true);
            usbSerialPort.setRTS(true);
            //calibrate();
        } catch (Exception e) {
            e.printStackTrace();
            if (camListener != null) camListener.setNoFeedImage();
        }

        usbIoManager = new SerialInputOutputManager(usbSerialPort, sioListener);
        Executors.newSingleThreadExecutor().submit(usbIoManager);
    }

    public void disconnect() throws IOException {
        if(usbIoManager != null) {
            usbIoManager.stop();
        }
        usbIoManager = null;
        usbSerialPort.close();
        //usbPermission = UsbPermission.Unknown;
        if (camListener != null) camListener.updateText("Disconnected");
    }

    public void calibrate() throws IOException {
        usbSerialPort.write("C".getBytes(), 1);
    }

    // Analysis mode
    // Send bytes 0x42 and 0x08 to activate it
    // Send bytes 0x42 and 0x02 to disable it
    // Note 0x42 is character ‘B’
    public void toggleAnalysisMode(){
        if (!analysisMode) {
            try {
                usbSerialPort.write("B".getBytes(), 1);
                usbSerialPort.write(new byte[]{0x08}, 1);
                analysisMode = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                usbSerialPort.write("B".getBytes(), 1);
                usbSerialPort.write(new byte[]{0x02}, 1);
                analysisMode = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void changeTiltAngle(int angle) {
        if(angle < 22 || angle > 95) return;
        angle = angle*100;
        byte[] data = new byte[]{
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                1,
                (byte) (angle & 0xff),
                (byte) (angle >> 8)
        };
        System.out.println("tilt angle " + ((data[4]&0xFF) + (data[5]&0xFF)*256));
        try {
            usbSerialPort.write(data, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeTiltSpeed(int speed) {
        byte[] data = new byte[]{
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                2,
                (byte) speed,
                0
        };
        System.out.println("tilt speed " + ((data[4]&0xFF) + (data[5]&0xFF)*256));
        try {
            usbSerialPort.write(data, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // to change thermal camera mode
    // mode 0: average of 64 frames from thermal cam - use this or mode 2 for temp measurement
    // mode 1: average of 4 frames
    // mode 2: combination of modes 0 & 1, this is on by default
    // mode 3: shows movement only and highlights it for 2 seconds
    // mode 4: shows movement only, no highlight
    public void changeVideoMode(int mode) {
        byte[] data = new byte[] {
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                3,
                (byte) mode,
                0

        };
        System.out.println("video mode " + ((data[4]&0xFF) + (data[5]&0xFF)*256 -2));
        try {
            usbSerialPort.write(data, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
