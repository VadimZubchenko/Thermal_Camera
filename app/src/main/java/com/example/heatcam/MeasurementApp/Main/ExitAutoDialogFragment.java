package com.example.heatcam.MeasurementApp.Main;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.heatcam.MeasurementApp.Fragments.CameraTest.MenuFragment;
import com.example.heatcam.R;

public class ExitAutoDialogFragment extends DialogFragment {
    private EditText editTextPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View pView = inflater.inflate(R.layout.heatcam_enter_password_dialog, null);

        builder.setView(pView);
            builder.setMessage(R.string.enter_password_to_exit_msg);
            builder.setPositiveButton("OK", (dialog, id) -> {
                editTextPassword = pView.findViewById(R.id.input_password);
               if (editTextPassword.getText().toString().equals("heatcam123")) {
                    Fragment f = new MenuFragment();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentCamera, f, "menu")
                            .commit();
                    MainActivity.setAutoMode(false);
                } else if (editTextPassword.getText().toString().equals("")) {
                    //MainActivity.setAutoMode(true);
                   Toast.makeText(getContext(), R.string.no_password_msg, Toast.LENGTH_SHORT).show();
                } else {
                   Toast.makeText(getContext(), R.string.wrong_password_msg, Toast.LENGTH_SHORT).show();
               }
            });
        // User cancelled the dialog
        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
        return builder.create();
    }
}
