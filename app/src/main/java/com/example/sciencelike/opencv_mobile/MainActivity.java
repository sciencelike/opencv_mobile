package com.example.sciencelike.opencv_mobile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// フルスクリーン表示のために追加
import android.view.View;

// カメラ権限関連
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.Manifest;

import static org.opencv.core.Core.inRange;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    public void onResume() {
        Log.d("onResume","run");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // カメラの権限確認
        Log.d("onCreate","Permisson Check");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("onCreate","Permisson Granted");
            launchCamera();
        } else {
            Log.d("onCreate","Permisson Not Found");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, 1);
        }

        // Set up camera listener.
        launchCamera();

        //フルスクリーン化
        enableFullscreen();
    }

    // カメラ起動
    public void launchCamera() {
        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        // ここでカメラの最大解像度を設定する
        mOpenCvCameraView.setMaxFrameSize(600, 600);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    // ナビゲーションバーを隠す
    public void enableFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    // Load a network.
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera view start");
    }
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        /*
        final int IN_WIDTH = 224;
        final int IN_HEIGHT = 224;
        final float WH_RATIO = (float)IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        final double THRESHOLD = 0.2;
        */
        Scalar RECT_COLOR = new Scalar(0,255,0);

        // Get a new frame
        Mat frame = inputFrame.rgba();
        // 参考先
        // https://qiita.com/yokobonbon/items/c363502df0d3eddf97b4
        MatOfPoint maxArea = SkinDetector.getInstance().getMaxSkinArea(frame);
        // Imgproc.cvtColor(frame, frame,Imgproc.COLOR_RGB2HSV);
        // Core.inRange(frame, new Scalar(0, 60, 80), new Scalar(20, 200, 230), frame);
        if (maxArea != null) {
            Rect rectOfArea = Imgproc.boundingRect(maxArea);
            Imgproc.rectangle(frame, rectOfArea.tl(), rectOfArea.br(), RECT_COLOR, 3);
        }

        return frame;
    }
    public void onCameraViewStopped() {}
    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }
    private static final String TAG = "OpenCV/Sample/MobileNet";
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};
    private Net net;
    private CameraBridgeViewBase mOpenCvCameraView;
}
