package com.example.heatcam.PrivateKeyboard.Connection;

import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;

public interface ConnectionListener {
    void onSendInputField(NewMessage message);
    void onUpdateTiltAngle(TiltAngle message);
    void onPressButton(TakingPicture message);
    void onConfirmQRScan(ConfirmQRScan message);
}
