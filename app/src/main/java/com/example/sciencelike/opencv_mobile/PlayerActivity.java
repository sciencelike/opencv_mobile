package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
    static ArrayList<Button> button_list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // vr
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

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

        // ボタンをリスト追加
        for(int i=1; i<=9; i++){
            int viewId = getResources().getIdentifier("Button_" + i, "id", getPackageName());
            Button temp = findViewById(viewId);
            button_list.add(temp);
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
        // Get a new frame
        frame = inputFrame.rgba();

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

        if (maxArea.size() > 0) {
            // ポインタに使う先端取得
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

            // クリック判定とボタン動作
            // クリックトリガー
            if(ConvexityDefects.getPointsNumber() == 0) {
                check = 1;
            }
            // クリック動作
            if(ConvexityDefects.getPointsNumber() == 1 && check == 1 && SystemClock.uptimeMillis() >= lastmotionedtime+500) {
                Log.i("MainActivity Touchtest","Single Touch " + x + " " + y);

                lastmotionedtime = SystemClock.uptimeMillis();
                check = 0;

                int[] location = new int[2];

                // ボタンの当たり判定処理
                for(int i=1; i<=9; i++){
                    button_list.get(i-1).getLocationInWindow(location);
                    if(x >= location[0] && x <= (location[0]+button_list.get(i-1).getWidth()) && y >= location[1] && y <= (location[1]+button_list.get(i-1).getHeight())) {
                        Log.i("MainActivity Touchtest", "Touched button_" + i);
                        button_click(button_list.get(i-1));
                    }
                }
            }
            if(ConvexityDefects.getPointsNumber() >= 2) {
                check = 0;
            }
        }

        writeData();

        return frame;
    }

    public void onCameraViewStopped() {}

    // ファイル書き込み用
    private String filePath = Environment.getExternalStorageDirectory().getPath() + "/soturon/test.csv";

    public void writeData() {
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            String str = String.valueOf(SystemClock.uptimeMillis()) + ", " + x + ", " + y + "\n";
            File file = new File(filePath);

            try(FileOutputStream fileOutputStream =
                    new FileOutputStream(file, true);
                OutputStreamWriter outputStreamWriter =
                    new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter bw =
                    new BufferedWriter(outputStreamWriter)
            ) {
                bw.write(str);
                bw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 暫定的ボタン対応
    public void button_click (View view) {
        // final String s_on = getResources().getString(R.string.button_txt_on);
        // final String s_off = getResources().getString(R.string.button_txt_off);

        switch (view.getId()) {
            case R.id.Button_1:
                // finish();
                Log.d("PlayerActivity Button", "Touched Button1");
                finish();
                break;

            case R.id.Button_2:
                Log.d("PlayerActivity Button", "Touched Button2");
                break;

            case R.id.Button_3:
                Log.d("PlayerActivity Button", "Touched Button3");
                break;

            case R.id.Button_4:
                Log.d("PlayerActivity Button", "Touched Button4");
                break;

            case R.id.Button_5:
                Log.d("PlayerActivity Button", "Touched Button5");
                break;

            case R.id.Button_6:
                Log.d("PlayerActivity Button", "Touched Button6");
                break;

            case R.id.Button_7:
                Log.d("PlayerActivity Button", "Touched Button7");
                break;

            case R.id.Button_8:
                Log.d("PlayerActivity Button", "Touched Button8");
                break;

            case R.id.Button_9:
                Log.d("PlayerActivity Button", "Touched Button9");
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