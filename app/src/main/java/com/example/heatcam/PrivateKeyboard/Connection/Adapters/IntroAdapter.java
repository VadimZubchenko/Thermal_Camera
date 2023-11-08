package com.example.heatcam.PrivateKeyboard.Connection.Adapters;

import com.example.heatcam.PrivateKeyboard.Connection.ConnectionListener;
import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;

public abstract class IntroAdapter implements ConnectionListener {
    @Override
    public void onSendInputField(NewMessage message) {}

    @Override
    public void onUpdateTiltAngle(TiltAngle message) {}

    @Override
    public void onPressButton(TakingPicture message) {}
}
