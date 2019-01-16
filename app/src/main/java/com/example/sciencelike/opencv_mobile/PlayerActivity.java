package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity implements CvCameraViewListener2 {
    // VR
    private VrPanoramaView panoWidgetView;
    private ImageLoaderTask backgroundImageLoaderTask;

    // opencv
    private static final String TAG_OPENCV = "OpenCV/HandRecognition";
    private CameraBridgeViewBase mOpenCvCameraView;
    public Mat frame;

    // opencv_クリック検出用
    static int check = 0;
    static long lastmotionedtime = 0;
    static float x = 0, y = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // vr
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // panoWidgetView = (VrPanoramaView) findViewById(R.id.player_Vrpanoramaview);
        panoWidgetView = findViewById(R.id.player_Vrpanoramaview);

        loadPanoImage();


        // opencv
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

        // ポインタ描画
        //オーバーレイビューの追加
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(new OverlayPointer(this), params);

        // 画面消灯を無効化する
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onPause() {
        panoWidgetView.pauseRendering();
        super.onPause();
    }

    @Override
    public void onResume() {
        // vr
        panoWidgetView.resumeRendering();
        super.onResume();

        // opencv
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        // Destroy the widget and free memory.
        panoWidgetView.shutdown();
        super.onDestroy();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG_OPENCV, "Camera view start");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Scalar LINE_COLOR_W = new Scalar(255,255,255);
        Scalar LINE_COLOR_b = new Scalar(0,0,0);
        Scalar LINE_COLOR_R = new Scalar(255,0,0);
        Scalar LINE_COLOR_G = new Scalar(0,255,0);
        Scalar LINE_COLOR_B = new Scalar(0,0,255);

        // Get a new frame
        frame = inputFrame.rgba();

        // 手の最大面積の領域取得 (輪郭ではない)
        MatOfPoint contours = SkinDetector.getMaxSkinArea(frame);

        List<MatOfPoint> maxArea = new ArrayList<>();

        Point point_moment = new Point();

        if (contours != null) {
            // 手の最大面積の輪郭を取得
            maxArea = OutlineDetector.getLineData(contours);

            // 最大面積の輪郭から重心計算
            point_moment = CalcPoint.calcMoment(contours);
        }

        if (maxArea.size() > 0) {
            // 先端描画
            List<Point> point_list_tips = maxArea.get(0).toList();
            int index_maxdistancefinger = 0;
            for (int i = 0; i < point_list_tips.size(); i++) {
                Point point_tips = new Point(point_list_tips.get(i).x, point_list_tips.get(i).y);
                if(CalcPoint.calcDistance(new Point(point_list_tips.get(index_maxdistancefinger).x, point_list_tips.get(index_maxdistancefinger).y), point_moment) < CalcPoint.calcDistance(point_tips, point_moment)) {
                    index_maxdistancefinger = i;
                }
            }

            // 実画面とopencv viewのスケーリング
            x = (float)point_list_tips.get(index_maxdistancefinger).x * mOpenCvCameraView.mScale;
            y = (float)point_list_tips.get(index_maxdistancefinger).y * mOpenCvCameraView.mScale;

            // Log.i("PlayerActivity Touchtest", "Cursor Point" + x + " " + y);

            // クリック判定とボタン動作
            if(ConvexityDefects.getPointsNumber() == 0) {
                check = 1;
            }
            if(ConvexityDefects.getPointsNumber() == 1 && check == 1 && SystemClock.uptimeMillis() >= lastmotionedtime+1000) {
                Log.i("MainActivity Touchtest","Single Touch " + x + " " + y);
                lastmotionedtime = SystemClock.uptimeMillis();
                check = 0;

                int[] location = new int[2];
                Button button_r = findViewById(R.id.Button_r);
                Button button_g = findViewById(R.id.Button_g);
                Button button_b = findViewById(R.id.Button_b);
                button_r.getLocationInWindow(location);
                if(x >= location[0] && x <= (location[0]+button_r.getWidth()) && y >= location[1] && y <= (location[1]+button_r.getHeight())) {
                    Log.i("MainActivity Touchtest", "Touched button_r");
                    button_click(findViewById(R.id.Button_r));
                }
                button_g.getLocationInWindow(location);
                if(x >= location[0] && x <= (location[0]+button_g.getWidth()) && y >= location[1] && y <= (location[1]+button_g.getHeight())) {
                    Log.i("MainActivity Touchtest", "Touched button_g");
                    button_click(findViewById(R.id.Button_g));
                }
                button_b.getLocationInWindow(location);
                if(x >= location[0] && x <= (location[0]+button_b.getWidth()) && y >= location[1] && y <= (location[1]+button_b.getHeight())) {
                    Log.i("MainActivity Touchtest", "Touched button_b");
                    button_click(findViewById(R.id.Button_b));
                }
            }
            if(ConvexityDefects.getPointsNumber() >= 2) {
                check = 0;
            }
        }

        return frame;
    }

    public void onCameraViewStopped() {}

    // 暫定的ボタン対応
    public void button_click (View view) {
        final String s_on = getResources().getString(R.string.button_txt_on);
        final String s_off = getResources().getString(R.string.button_txt_off);

        switch (view.getId()) {
            case R.id.Button_r:
                final Button button_r = findViewById(R.id.Button_r);
                if (button_r.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_r)).setText(s_on);
                else ((Button) findViewById(R.id.Button_r)).setText(s_off);

                finish();

                Log.d("MainActivity Button", "Touched Button0");

                break;
            case R.id.Button_g:
                final Button button_g = findViewById(R.id.Button_g);
                if (button_g.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_g)).setText(s_on);
                else ((Button) findViewById(R.id.Button_g)).setText(s_off);

                Log.d("MainActivity Button", "Touched Button1");
                /*
                Intent intent1 = new Intent(this, SimpleVrVideoActivity.class);
                startActivity(intent1);
                */

                break;
            case R.id.Button_b:
                final Button button_b = findViewById(R.id.Button_b);
                if (button_b.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_b)).setText(s_on);
                else ((Button) findViewById(R.id.Button_b)).setText(s_off);

                Log.d("MainActivity Button", "Touched Button2");

                // Intent intent2 = new Intent(this, PlayerActivity.class);
                // startActivity(intent2);

                break;
        }
    }

    static public float[] getCursorPoint() {
        float point[] = {x,y};
        return point;
    }

    // 読み込み用
    // 360画像読み込み
    private synchronized void loadPanoImage() {
        ImageLoaderTask task = backgroundImageLoaderTask;
        if (task != null && !task.isCancelled()) {
            // Cancel any task from a previous loading.
            task.cancel(true);
        }

        // pass in the name of the image to load from assets.
        VrPanoramaView.Options viewOptions = new VrPanoramaView.Options();
        viewOptions.inputType = VrPanoramaView.Options.TYPE_MONO;

        // use the name of the image in the assets/ directory.
        String panoImageName = "mountain.jpg";

        // create the task passing the widget view and call execute to start.
        task = new ImageLoaderTask(panoWidgetView, viewOptions, panoImageName);
        task.execute(getAssets());
        backgroundImageLoaderTask = task;
    }

    // Initialize OpenCV manager.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG_OPENCV, "OpenCV loaded successfully");
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

    // カメラ起動
    public void launchCamera() {
        mOpenCvCameraView = findViewById(R.id.player_CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        // ここでカメラの最大解像度を設定する
        mOpenCvCameraView.setMaxFrameSize(960, 540);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }
}