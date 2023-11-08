package com.example.heatcam.PrivateKeyboard.Connection;

import android.util.Log;

import com.example.heatcam.PrivateKeyboard.Data.ConfirmQRScan;
import com.example.heatcam.PrivateKeyboard.Data.NewMessage;
import com.example.heatcam.PrivateKeyboard.Data.TakingPicture;
import com.example.heatcam.PrivateKeyboard.Data.TiltAngle;
import com.example.heatcam.PrivateKeyboard.Helpers.QRUtils;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import static com.example.heatcam.PrivateKeyboard.Data.EmailConfig.saveInstance;

public class ConnectionHandler {
    private String functionUrl;
    private HubConnection hubConnection;

    private ConnectionListener listener;

    private static ConnectionHandler INSTANCE;

    public static ConnectionHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionHandler();
        }

        return INSTANCE;
    }

    public ConnectionHandler initConnection() {
        if (null != hubConnection
                && hubConnection.getConnectionState().equals(HubConnectionState.CONNECTED)) {
            stop();
        }

        hubConnection = HubConnectionBuilder
                .create(functionUrl)
                .build();

        hubConnection.on("sendInputField", (message) -> {
            Log.d("NewMessage", message.text);
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            listener.onSendInputField(message);
        }, NewMessage.class);

        hubConnection.on("updateTiltAngle", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("TiltAngle", String.valueOf(message.value));
            saveInstance.put("TextViewField-Tilt", String.valueOf(message.value));
            listener.onUpdateTiltAngle(message);
        }, TiltAngle.class);

        hubConnection.on("pressButton", (message) -> {
            if (!message.sender.equals(QRUtils.connectedUuid)) return;
            Log.d("pressButton", String.valueOf(message.value));
            listener.onPressButton(message);
        }, TakingPicture.class);

        hubConnection.on("confirmQRScan", (message) -> {
            Log.d("ConfirmQRScan", message.uuid);
            if (!message.uuid.equals(QRUtils.newUuid)) return;
            listener.onConfirmQRScan(message);
        }, ConfirmQRScan.class);

        //Start the connection
        hubConnection.start().blockingAwait();

        return INSTANCE;

    }

    public void stop() {
        hubConnection.stop();
    }

    public ConnectionHandler setListener(ConnectionListener listener) {
        this.listener = listener;
        return INSTANCE;
    }

    private ConnectionHandler(String functionUrl) {
        this.functionUrl = functionUrl;
    }

    private ConnectionHandler() {
        this("https://privatekeyboard.azurewebsites.net/api");
    }

    private ConnectionHandler(ConnectionListener listener) {
        this();
        this.listener = listener;
        initConnection();
    }

    private ConnectionHandler(ConnectionListener listener, String functionUrl) {
        this(functionUrl);
        this.listener = listener;
    }

}
