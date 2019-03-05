package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PlayerActivity extends AppCompatActivity implements CvCameraViewListener2 {
    // VR
    private VrPanoramaView panoWidgetView;
    private ImageLoaderTask backgroundImageLoaderTask;

    // opencv
    private CameraBridgeViewBase mOpenCvCameraView;

    // opencv_クリック検出用
    private int check = 0;
    private long lastmotionedtime = 0;
    private static float x = 0;
    private static float y = 0;
    private final ArrayList<Button> button_list = new ArrayList<>();

    // ボタンテスト
    private int buttonState = 0;
    private final List<Integer> targetList = new ArrayList<>();
    private int firstButtonId = 0;
    private int secondButtonId = 0;
    private final int amountButton = 6;
    private ProgressBar progressBar;
    private int progressVal = 0;

    // ポインタ改
    static final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout pointer;

    // マウス操作用
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ボタンサイズ変更
        Intent intent = getIntent();

        // vr
        super.onCreate(savedInstanceState);
        if ("2".equals(intent.getStringExtra("button_id"))) {
            setContentView(R.layout.activity_player_normal);
        } else if ("1".equals(intent.getStringExtra("button_id"))) {
            setContentView(R.layout.activity_player_small);
        }

        panoWidgetView = findViewById(R.id.player_Vrpanoramaview);

        // vr中のマウス操作有効化のための、リスナーset
        mGestureDetector = new GestureDetector(this, simpleOnGestureListener);
        panoWidgetView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        });
        panoWidgetView.setFullscreenButtonEnabled(false);
        panoWidgetView.setInfoButtonEnabled(false);
        panoWidgetView.setStereoModeButtonEnabled(false);

        loadPanoImage();

        // opencv
        // カメラの権限確認
        Log.d("onCreate", "Permisson Check");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("onCreate", "Permisson Granted");
            launchCamera();
        } else {
            Log.d("onCreate", "Permisson Not Found");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, 1);
        }

        // ボタンをリスト追加
        for (int i = 1; i <= 9; i++) {
            int viewId = getResources().getIdentifier("Button_" + i, "id", getPackageName());
            Button temp = findViewById(viewId);
            button_list.add(temp);
        }

        // ポインタ描画改
        pointer = findViewById(R.id.PointerCircle);

        // 画面消灯を無効化する
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ボタンを初期化
        initButtonState();

        // ボタン押す順番決定
        for (int i = 1; i <= amountButton; i++) {
            for (int j = 1; j <= amountButton; j++) {
                if (i != j) targetList.add(i * 10 + j);
            }
        }
        Collections.shuffle(targetList);

        // プログレスバー
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(30);

        LogWriter.writeData(SystemClock.uptimeMillis(), "Start PlayerActivity");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
        Log.i("OPENCV", "Camera view start");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Get a new frame
        Mat frame = inputFrame.rgba();

        // 手の最大面積の領域取得 (輪郭ではない)
        MatOfPoint contours = SkinDetector.getMaxSkinArea(frame);
        List<MatOfPoint> maxArea = new ArrayList<>();
        Point point_moment = new Point();
        MatOfInt hull = new MatOfInt();

        if (contours != null) {
            // 手の最大面積の輪郭を取得
            maxArea = OutlineDetector.getLineData(contours);
            // 最大面積の輪郭から重心計算
            point_moment = CalcPoint.calcMoment(contours);
            // 凹点描画のためのhullを取得
            Imgproc.convexHull(contours, hull);
            // 凹点集合計算
            ConvexityDefects.convexityDefects(frame, contours, hull, false);
        }

        if (Objects.requireNonNull(maxArea).size() > 0) {
            // ポインタに使う先端取得
            List<Point> point_list_tips = maxArea.get(0).toList();
            int index_maxdistancefinger = 0;
            for (int i = 0; i < point_list_tips.size(); i++) {
                Point point_tips = new Point(point_list_tips.get(i).x, point_list_tips.get(i).y);
                if (CalcPoint.calcDistance(new Point(point_list_tips.get(index_maxdistancefinger).x, point_list_tips.get(index_maxdistancefinger).y), point_moment) < CalcPoint.calcDistance(point_tips, point_moment)) {
                    index_maxdistancefinger = i;
                }
            }

            // 実画面とopencv viewのスケーリング
            x = (float) point_list_tips.get(index_maxdistancefinger).x * mOpenCvCameraView.mScale;
            y = (float) point_list_tips.get(index_maxdistancefinger).y * mOpenCvCameraView.mScale;

            // クリック判定とボタン動作
            // クリックトリガー
            if (ConvexityDefects.getPointsNumber() == 0) {
                check = 1;
                // ポインタ色変更
                setPointerColor(true);
            }
            // クリック動作
            if (ConvexityDefects.getPointsNumber() == 1 && check == 1 && SystemClock.uptimeMillis() >= lastmotionedtime + 200) {
                lastmotionedtime = SystemClock.uptimeMillis();
                check = 0;

                // ポインタ色変更
                setPointerColor(false);

                // ボタンの当たり判定処理
                int[] location = new int[2];
                for (int i = 1; i <= amountButton; i++) {
                    button_list.get(i - 1).getLocationInWindow(location);
                    if (x >= location[0] && x <= (location[0] + button_list.get(i - 1).getWidth()) && y >= location[1] && y <= (location[1] + button_list.get(i - 1).getHeight())) {
                        Log.i("PlayerActivity Touchtest", "Touched button_" + i);
                        LogWriter.writeData(SystemClock.uptimeMillis(), "PlayerActivity_onCameraFrame_Touched button", Integer.toString(i));
                        button_click(button_list.get(i - 1));
                    }
                }
            }
            if (ConvexityDefects.getPointsNumber() >= 2) {
                check = 0;
            }
        }

        LogWriter.writeData(SystemClock.uptimeMillis(), "PlayerActivity_onCameraFrame_ConvexityDefectsAmount", ConvexityDefects.getPointsNumber());
        LogWriter.writeData(SystemClock.uptimeMillis(), "PlayerActivity_onCameraFrame_Pointer", x, y);

        // ポインタ描画改
        Multithread thread_pointer = new Multithread(this);
        thread_pointer.start();

        return frame;
    }

    public void onCameraViewStopped() {
    }

    private void setPointerColor(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    pointer.getBackground().setColorFilter(Color.parseColor("#FF0000FF"), PorterDuff.Mode.SRC_IN);
                } else {
                    pointer.getBackground().setColorFilter(Color.parseColor("#FFFF0000"), PorterDuff.Mode.SRC_IN);
                }
            }
        });
    }

    private void setPointerVisibility(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    pointer.setAlpha(1);
                } else {
                    pointer.setAlpha(0);
                }
            }
        });
    }

    private void initButtonState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= 9; i++) {
                    if (i == 8) continue;
                    int viewId = getResources().getIdentifier("Button_" + i, "id", getPackageName());
                    findViewById(viewId).setVisibility(View.INVISIBLE);
                }
                if (buttonState == 0) {
                    findViewById(R.id.Button_5).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void changeButtonState(final int number) {
        initButtonState();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int order_1 = number / 10;
                int order_2 = number - (number / 10) * 10;

                int viewId_1 = getResources().getIdentifier("Button_" + Integer.toString(order_1), "id", getPackageName());
                int viewId_2 = getResources().getIdentifier("Button_" + Integer.toString(order_2), "id", getPackageName());

                firstButtonId = viewId_1;
                secondButtonId = viewId_2;

                findViewById(viewId_1).setVisibility(View.VISIBLE);
                ((Button) findViewById(viewId_1)).setText("1");
                findViewById(viewId_2).setVisibility(View.VISIBLE);
                ((Button) findViewById(viewId_2)).setText("2");

            }
        });

        buttonState = 2;
    }

    private void invisibleButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonState = 3;
                findViewById(firstButtonId).setVisibility(View.INVISIBLE);
            }
        });
    }

    // 暫定的ボタン対応
    public void button_click(View view) {
        Log.d("PlayerActivity Button", Integer.toString(buttonState));

        switch (view.getId()) {
            case R.id.Button_1:
                Log.d("PlayerActivity Button", "Touched Button1");
                if (buttonState == 2 && firstButtonId == R.id.Button_1) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_1) buttonState = 1;

                break;

            case R.id.Button_2:
                Log.d("PlayerActivity Button", "Touched Button2");
                if (buttonState == 2 && firstButtonId == R.id.Button_2) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_2) buttonState = 1;

                break;

            case R.id.Button_3:
                Log.d("PlayerActivity Button", "Touched Button3");
                if (buttonState == 2 && firstButtonId == R.id.Button_3) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_3) buttonState = 1;

                break;

            case R.id.Button_4:
                Log.d("PlayerActivity Button", "Touched Button4");
                if (buttonState == 2 && firstButtonId == R.id.Button_4) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_4) buttonState = 1;
                break;

            case R.id.Button_5:
                Log.d("PlayerActivity Button", "Touched Button5");
                if (buttonState == 0) {
                    buttonState = 1;
                }
                if (buttonState == 2 && firstButtonId == R.id.Button_5) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_5) buttonState = 1;

                break;

            case R.id.Button_6:
                Log.d("PlayerActivity Button", "Touched Button6");
                if (buttonState == 2 && firstButtonId == R.id.Button_6) invisibleButton();
                if (buttonState == 3 && secondButtonId == R.id.Button_6) buttonState = 1;

                break;
        }

        if (targetList.size() != 0) {
            if (buttonState == 1) {
                // プログレスバー進捗更新
                progressBar.setProgress(progressVal += 1);

                // ポインタ非表示
                setPointerVisibility(false);

                // ボタン非表示
                initButtonState();

                // 待ち時間
                long nextchangetime = SystemClock.uptimeMillis() + 500 + (long) (Math.random() * 1000);
                while (nextchangetime > SystemClock.uptimeMillis()) {
                }

                // ポインタ表示
                setPointerVisibility(true);

                // 次のターゲット表示
                int number = targetList.get(0);
                targetList.remove(0);
                changeButtonState(number);
                LogWriter.writeData(SystemClock.uptimeMillis(), "PlayerActivity_buttonclick_buttonorder", number);
            }
        } else if (buttonState == 1) {
            finish();
        }

    }

    public boolean onTouchEvent(MotionEvent event) {
        event.getAction();

        return mGestureDetector.onTouchEvent(event);
    }

    private final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent event) {
            Log.i("MainActivity Gesture", "Down ");
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            Log.i("MainActivity Gesture", "SingleTapUp ");

            int[] location = new int[2];
            for (int i = 1; i <= amountButton; i++) {
                button_list.get(i - 1).getLocationInWindow(location);
                if (x >= location[0] && x <= (location[0] + button_list.get(i - 1).getWidth()) && y >= location[1] && y <= (location[1] + button_list.get(i - 1).getHeight())) {
                    Log.i("PlayerActivity Touchtest", "Touched button_" + i);
                    LogWriter.writeData(SystemClock.uptimeMillis(), "PlayerActivity_onCameraFrame_Clicked button", Integer.toString(i));
                    button_click(button_list.get(i - 1));
                }
            }

            return super.onSingleTapUp(event);
        }
    };


    static public float[] getCursorPoint() {
        return new float[]{x, y};
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

        // 追記
        // VrPanoramaView.;

        // use the name of the image in the assets/ directory.
        String panoImageName = "mountain.jpg";

        // create the task passing the widget view and call execute to start.
        task = new ImageLoaderTask(panoWidgetView, viewOptions, panoImageName);
        task.execute(getAssets());
        backgroundImageLoaderTask = task;
    }

    // Initialize OpenCV manager.
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OPENCV", "OpenCV loaded successfully");
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
    private void launchCamera() {
        mOpenCvCameraView = findViewById(R.id.player_CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        // ここでカメラの最大解像度を設定する
        mOpenCvCameraView.setMaxFrameSize(960, 540);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }
}