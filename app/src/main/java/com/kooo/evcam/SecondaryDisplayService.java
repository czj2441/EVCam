package com.kooo.evcam;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.camera.SingleCamera;

/**
 * 副屏显示服务
 * 负责在指定的显示器上显示摄像头的悬浮窗
 */
public class SecondaryDisplayService extends Service {
    private static final String TAG = "SecondaryDisplayService";

    private WindowManager windowManager;
    private View floatingView;
    private TextureView textureView;
    private View borderView;
    private AppConfig appConfig;
    private DisplayManager displayManager;
    private SingleCamera currentCamera;

    @Override
    public void onCreate() {
        super.onCreate();
        appConfig = new AppConfig(this);
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateDisplay();
        return START_STICKY;
    }

    private void updateDisplay() {
        removeFloatingView();

        if (!appConfig.isSecondaryDisplayEnabled()) {
            stopSelf();
            return;
        }

        int displayId = appConfig.getSecondaryDisplayId();
        Display display = displayManager.getDisplay(displayId);
        if (display == null) {
            AppLog.e(TAG, "找不到指定的 Display ID: " + displayId);
            return;
        }

        // 创建对应显示器的 Context
        Context displayContext = createDisplayContext(display);
        windowManager = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);

        // 加载布局
        floatingView = LayoutInflater.from(displayContext).inflate(R.layout.presentation_secondary_display, null);
        textureView = floatingView.findViewById(R.id.secondary_texture_view);
        borderView = floatingView.findViewById(R.id.secondary_border);

        // 设置边框
        borderView.setVisibility(appConfig.isSecondaryDisplayBorderEnabled() ? View.VISIBLE : View.GONE);

        // 设置悬浮窗参数
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        int x = appConfig.getSecondaryDisplayX();
        int y = appConfig.getSecondaryDisplayY();
        int width = appConfig.getSecondaryDisplayWidth();
        int height = appConfig.getSecondaryDisplayHeight();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width > 0 ? width : WindowManager.LayoutParams.WRAP_CONTENT,
                height > 0 ? height : WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;

        // 设置旋转角度
        int rotation = appConfig.getSecondaryDisplayRotation();
        textureView.setRotation(rotation);

        // 设置屏幕方向 (处理倒置等)
        int orientation = appConfig.getSecondaryDisplayOrientation();
        floatingView.setRotation(orientation); 

        // 监听 TextureView 状态
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surfaceTexture, int w, int h) {
                AppLog.d(TAG, "Secondary TextureView available: " + w + "x" + h);
                startCameraPreview(new Surface(surfaceTexture));
            }

            @Override
            public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surface, int w, int h) {}

            @Override
            public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surface) {
                stopCameraPreview();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surface) {}
        });

        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            AppLog.e(TAG, "无法添加副屏悬浮窗: " + e.getMessage(), e);
        }
    }

    private void startCameraPreview(Surface surface) {
        String cameraPos = appConfig.getSecondaryDisplayCamera();
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity == null) {
            AppLog.e(TAG, "MainActivity 未启动，无法获取相机实例");
            return;
        }

        MultiCameraManager cameraManager = mainActivity.getCameraManager();
        if (cameraManager == null) {
            AppLog.e(TAG, "MultiCameraManager 未初始化");
            return;
        }

        currentCamera = cameraManager.getCamera(cameraPos);
        if (currentCamera != null) {
            AppLog.d(TAG, "开始副屏预览: " + cameraPos);
            currentCamera.setSecondarySurface(surface);
            currentCamera.recreateSession();
        } else {
            AppLog.e(TAG, "找不到指定的摄像头: " + cameraPos);
        }
    }

    private void stopCameraPreview() {
        if (currentCamera != null) {
            AppLog.d(TAG, "停止副屏预览");
            currentCamera.setSecondarySurface(null);
            currentCamera.recreateSession();
            currentCamera = null;
        }
    }

    private void removeFloatingView() {
        stopCameraPreview();
        if (windowManager != null && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                // Ignore
            }
            floatingView = null;
            windowManager = null;
        }
    }

    @Override
    public void onDestroy() {
        removeFloatingView();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 更新服务状态
     */
    public static void update(Context context) {
        Intent intent = new Intent(context, SecondaryDisplayService.class);
        context.startService(intent);
    }
}
