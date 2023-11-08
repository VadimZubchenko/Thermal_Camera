package com.example.heatcam.PrivateKeyboard.Connection.Adapters;

import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.PrivateKeyboard.Connection.ConnectionListener;
import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.MainActivity;

public abstract class FormActivityAdapter implements ConnectionListener {
    @Override
    public void onConfirmQRScan(ConfirmQRScan message) {}
}
