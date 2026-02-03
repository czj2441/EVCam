package com.kooo.evcam;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * 副屏显示设置界面 Fragment
 */
public class SecondaryDisplaySettingsFragment extends Fragment {

    private AppConfig appConfig;
    private DisplayManager displayManager;

    private SwitchMaterial secondaryDisplaySwitch;
    private Spinner cameraSpinner;
    private Spinner displaySpinner;
    private TextView displayInfoText;
    private SeekBar seekbarX, seekbarY, seekbarWidth, seekbarHeight;
    private TextView tvXValue, tvYValue, tvWidthValue, tvHeightValue;
    private Spinner rotationSpinner;
    private Spinner screenOrientationSpinner;
    private SwitchMaterial borderSwitch;
    private Button btnSaveApply;

    private List<Display> displayList = new ArrayList<>();
    private static final String[] CAMERA_OPTIONS = {"前摄像头", "后摄像头", "左摄像头", "右摄像头"};
    private static final String[] ROTATION_OPTIONS = {"0°", "90°", "180°", "270°"};
    private static final String[] ORIENTATION_OPTIONS = {"默认", "旋转90°", "旋转180°", "旋转270°"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_secondary_display_settings, container, false);

        appConfig = new AppConfig(requireContext());
        displayManager = (DisplayManager) requireContext().getSystemService(Context.DISPLAY_SERVICE);

        initViews(view);
        loadSettings();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        // Toolbar
        Button menuButton = view.findViewById(R.id.btn_menu);
        Button homeButton = view.findViewById(R.id.btn_home);

        menuButton.setOnClickListener(v -> {
            DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        homeButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).goToRecordingInterface();
            }
        });

        secondaryDisplaySwitch = view.findViewById(R.id.switch_secondary_display);
        cameraSpinner = view.findViewById(R.id.spinner_camera_selection);
        displaySpinner = view.findViewById(R.id.spinner_display_selection);
        displayInfoText = view.findViewById(R.id.tv_display_info);
        
        seekbarX = view.findViewById(R.id.seekbar_x);
        seekbarY = view.findViewById(R.id.seekbar_y);
        seekbarWidth = view.findViewById(R.id.seekbar_width);
        seekbarHeight = view.findViewById(R.id.seekbar_height);
        
        tvXValue = view.findViewById(R.id.tv_x_value);
        tvYValue = view.findViewById(R.id.tv_y_value);
        tvWidthValue = view.findViewById(R.id.tv_width_value);
        tvHeightValue = view.findViewById(R.id.tv_height_value);
        
        rotationSpinner = view.findViewById(R.id.spinner_rotation);
        screenOrientationSpinner = view.findViewById(R.id.spinner_screen_orientation);
        borderSwitch = view.findViewById(R.id.switch_border);
        btnSaveApply = view.findViewById(R.id.btn_save_apply);

        // Populate Spinners
        setupSpinners();
    }

    private void setupSpinners() {
        // Camera Spinner
        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, CAMERA_OPTIONS);
        cameraAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        cameraSpinner.setAdapter(cameraAdapter);

        // Display Spinner
        updateDisplayList();

        // Rotation Spinner
        ArrayAdapter<String> rotationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, ROTATION_OPTIONS);
        rotationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        rotationSpinner.setAdapter(rotationAdapter);

        // Screen Orientation Spinner
        ArrayAdapter<String> orientationAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, ORIENTATION_OPTIONS);
        orientationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        screenOrientationSpinner.setAdapter(orientationAdapter);
    }

    private void updateDisplayList() {
        displayList.clear();
        Display[] displays = displayManager.getDisplays();
        List<String> displayNames = new ArrayList<>();
        
        for (Display d : displays) {
            displayList.add(d);
            displayNames.add("Display " + d.getDisplayId() + " (" + d.getName() + ")");
        }

        ArrayAdapter<String> displayAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item, displayNames);
        displayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        displaySpinner.setAdapter(displayAdapter);
    }

    private void loadSettings() {
        secondaryDisplaySwitch.setChecked(appConfig.isSecondaryDisplayEnabled());

        String camera = appConfig.getSecondaryDisplayCamera();
        switch (camera) {
            case "front": cameraSpinner.setSelection(0); break;
            case "back": cameraSpinner.setSelection(1); break;
            case "left": cameraSpinner.setSelection(2); break;
            case "right": cameraSpinner.setSelection(3); break;
        }

        int displayId = appConfig.getSecondaryDisplayId();
        for (int i = 0; i < displayList.size(); i++) {
            if (displayList.get(i).getDisplayId() == displayId) {
                displaySpinner.setSelection(i);
                updateDisplayInfo(displayList.get(i));
                break;
            }
        }

        int x = appConfig.getSecondaryDisplayX();
        int y = appConfig.getSecondaryDisplayY();
        int w = appConfig.getSecondaryDisplayWidth();
        int h = appConfig.getSecondaryDisplayHeight();

        seekbarX.setProgress(x);
        seekbarY.setProgress(y);
        seekbarWidth.setProgress(w);
        seekbarHeight.setProgress(h);

        tvXValue.setText(String.valueOf(x));
        tvYValue.setText(String.valueOf(y));
        tvWidthValue.setText(String.valueOf(w));
        tvHeightValue.setText(String.valueOf(h));

        int rotation = appConfig.getSecondaryDisplayRotation();
        rotationSpinner.setSelection(rotation / 90);

        int screenOrientation = appConfig.getSecondaryDisplayOrientation();
        screenOrientationSpinner.setSelection(screenOrientation / 90);

        borderSwitch.setChecked(appConfig.isSecondaryDisplayBorderEnabled());
    }

    private void updateDisplayInfo(Display display) {
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        display.getRealMetrics(metrics);
        displayInfoText.setText(String.format("当前屏幕分辨率: %d x %d", metrics.widthPixels, metrics.heightPixels));
        
        // Update Seekbar max values based on screen size
        seekbarX.setMax(metrics.widthPixels);
        seekbarY.setMax(metrics.heightPixels);
        seekbarWidth.setMax(metrics.widthPixels);
        seekbarHeight.setMax(metrics.heightPixels);
    }

    private void setupListeners() {
        secondaryDisplaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !WakeUpHelper.hasOverlayPermission(requireContext())) {
                secondaryDisplaySwitch.setChecked(false);
                Toast.makeText(requireContext(), "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                WakeUpHelper.requestOverlayPermission(requireContext());
            }
        });

        displaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDisplayInfo(displayList.get(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        seekbarX.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvXValue.setText(String.valueOf(progress));
            }
        });

        seekbarY.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvYValue.setText(String.valueOf(progress));
            }
        });

        seekbarWidth.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvWidthValue.setText(String.valueOf(progress));
            }
        });

        seekbarHeight.setOnSeekBarChangeListener(new SimpleSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvHeightValue.setText(String.valueOf(progress));
            }
        });

        btnSaveApply.setOnClickListener(v -> saveAndApply());
    }

    private void saveAndApply() {
        appConfig.setSecondaryDisplayEnabled(secondaryDisplaySwitch.isChecked());
        
        String camera = "front";
        switch (cameraSpinner.getSelectedItemPosition()) {
            case 0: camera = "front"; break;
            case 1: camera = "back"; break;
            case 2: camera = "left"; break;
            case 3: camera = "right"; break;
        }
        appConfig.setSecondaryDisplayCamera(camera);
        
        int displayId = displayList.get(displaySpinner.getSelectedItemPosition()).getDisplayId();
        appConfig.setSecondaryDisplayId(displayId);
        
        appConfig.setSecondaryDisplayBounds(
                seekbarX.getProgress(),
                seekbarY.getProgress(),
                seekbarWidth.getProgress(),
                seekbarHeight.getProgress()
        );
        
        appConfig.setSecondaryDisplayRotation(rotationSpinner.getSelectedItemPosition() * 90);
        appConfig.setSecondaryDisplayOrientation(screenOrientationSpinner.getSelectedItemPosition() * 90);
        appConfig.setSecondaryDisplayBorderEnabled(borderSwitch.isChecked());

        Toast.makeText(requireContext(), "配置已保存并应用", Toast.LENGTH_SHORT).show();

        // Notify Service to update
        SecondaryDisplayService.update(requireContext());
    }

    private abstract static class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
