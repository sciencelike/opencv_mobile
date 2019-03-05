package com.example.sciencelike.opencv_mobile;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class JavaCameraResView extends JavaCameraView {
    public JavaCameraResView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void changeCameraFlash() {
        Camera.Parameters params = mCamera.getParameters();

        if(params.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        else params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

        mCamera.setParameters(params);
    }

    public void changeCameraWhiteBalance() {
        Camera.Parameters params = mCamera.getParameters();

        String nowWhiteBalance = params.getWhiteBalance();
        params.setAutoWhiteBalanceLock(true);

        mCamera.setParameters(params);
    }
}
