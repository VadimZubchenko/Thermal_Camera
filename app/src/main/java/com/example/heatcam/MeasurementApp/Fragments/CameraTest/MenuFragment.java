package com.example.heatcam.MeasurementApp.Fragments.CameraTest;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.heatcam.MeasurementApp.Fragments.IntroFragment.IntroFragment;
import com.example.heatcam.MeasurementApp.Main.MainActivity;
import com.example.heatcam.R;

import java.util.List;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.heatcam_menu_layout, container, false);
        // prevent app from dimming
        view.setKeepScreenOn(true);

        Fragment f = new CameraTestFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.menu_dev_fragment, f, "camera_test").commit();

        view.findViewById(R.id.menu_start_auto_button).setOnClickListener(v -> {
            /*
            Fragment fragment = new MeasurementStartFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                    .replace(R.id.fragmentCamera, fragment, "measure_start")
                    .commit();
            MainActivity.setAutoMode(true);
             */

            // INTRO FRAGMENT
            Fragment fragment = new IntroFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                    .replace(R.id.fragmentCamera, fragment, "measure_start")
                    .commit();
            MainActivity.setAutoMode(true);
        });

        view.findViewById(R.id.menu_logs_button).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), LogView.class));
        });

        view.findViewById(R.id.menu_settings_button).setOnClickListener(v -> {
            if(MainActivity.getSettingsStatus()) {
                FragmentManager manager = getActivity().getSupportFragmentManager();
                List<Fragment> l = manager.getFragments();

                // first two are main activity and menu fragment
                // -> remove top fragment from list
                if(l.size() > 2) {
                    manager.beginTransaction()
                            .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                            .remove(l.get(l.size() - 1))
                            .commit();
                }
                MainActivity.setSettingsStatus(false);
            } else {
                Fragment fragment = new SetupFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_in_left, R.animator.slide_in_right, 0, 0)
                        .add(R.id.menu_dev_fragment, fragment, "settings")
                        .commit();
                MainActivity.setSettingsStatus(true);
            }

        });

        return view;
    }
}
