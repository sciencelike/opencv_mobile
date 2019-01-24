package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import android.widget.Toast;

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
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


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
        mGestureDetector = new GestureDetector(this, simpleOnGestureListener);

        // カメラの権限確認
        Log.d("onCreate","Permisson Check");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d("onCreate","Permisson Granted: CAMERA");
            // Set up camera listener.
            launchCamera();
        } else {
            Log.d("onCreate","Permisson Not Found: CAMERA");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, 1);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d("onCreate","Permisson Granted: WRITE_EXTERNAL_STORAGE");
        }  else {
            Log.d("onCreate","Permisson Not Found: WRITE_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }

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

    // カメラ起動
    public void launchCamera() {
        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        // ここでカメラの最大解像度を設定する
        mOpenCvCameraView.setMaxFrameSize(960, 540);
        mOpenCvCameraView.setCvCameraViewListener(this);

        LogWriter.writeData("MainActivity_onCreate_camerasizeHW", mOpenCvCameraView.mFrameHeight, mOpenCvCameraView.mFrameWidth);
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera view start");
    }

    // 暫定的ボタン対応
    public void button_click (View view) {
        final String s_on = getResources().getString(R.string.button_txt_on);
        final String s_off = getResources().getString(R.string.button_txt_off);
        Intent intent = new Intent(this, PlayerActivity.class);

        switch (view.getId()) {
            case R.id.Button_r:
                final Button button_r = findViewById(R.id.Button_r);
                if (button_r.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_r)).setText(s_on);
                else ((Button) findViewById(R.id.Button_r)).setText(s_off);

                Log.d("MainActivity Button", "Touched Button0");

                break;
            case R.id.Button_g:
                final Button button_g = findViewById(R.id.Button_g);
                if (button_g.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_g)).setText(s_on);
                else ((Button) findViewById(R.id.Button_g)).setText(s_off);

                Log.d("MainActivity Button", "Touched Button1");

                intent.putExtra("button_id", "1");
                startActivity(intent);

                break;
            case R.id.Button_b:
                final Button button_b = findViewById(R.id.Button_b);
                if (button_b.getText().equals(s_off))
                    ((Button) findViewById(R.id.Button_b)).setText(s_on);
                else ((Button) findViewById(R.id.Button_b)).setText(s_off);

                Log.d("MainActivity Button", "Touched Button2");

                intent.putExtra("button_id", "2");
                startActivity(intent);

                break;
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Scalar LINE_COLOR_W = new Scalar(255,255,255);
        Scalar LINE_COLOR_b = new Scalar(0,0,0);
        Scalar LINE_COLOR_R = new Scalar(255,0,0);
        Scalar LINE_COLOR_G = new Scalar(0,255,0);
        Scalar LINE_COLOR_B = new Scalar(0,0,255);

        // Get a new frame
        frame = inputFrame.rgba();

        // 参考先
        // https://qiita.com/yokobonbon/items/c363502df0d3eddf97b4
        // https://qiita.com/gutugutu3030/items/3907530ee49433420b37
        // http://imoto-yuya.hatenablog.com/entry/2017/03/12/123357
        // https://docs.opencv.org/3.4/d7/d1d/tutorial_hull.html
        // http://www.kochi-tech.ac.jp/library/ron/2011/2011ele/1141009.pdf 指先検出とかいろいろ
        // https://dev.to/amarlearning/finger-detection-and-tracking-using-opencv-and-python-586m 実例

        // 目印描画
        SkinDetector.setSkinMarker(frame, LINE_COLOR_G);

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
            // 生最大面積輪郭描画
            List<MatOfPoint> temp = new ArrayList<>();
            temp.add(contours);
            // Imgproc.drawContours(frame, temp, -1, LINE_COLOR_R, 2);

            // 先端描画
            List<Point> point_list_tips = maxArea.get(0).toList();
            int index_maxdistancefinger = 0;
            for (int i = 0; i < point_list_tips.size(); i++) {
                Point point_tips = new Point(point_list_tips.get(i).x, point_list_tips.get(i).y);
                Imgproc.circle(frame, point_tips, 5, LINE_COLOR_W, 2);

                if(CalcPoint.calcDistance(new Point(point_list_tips.get(index_maxdistancefinger).x, point_list_tips.get(index_maxdistancefinger).y), point_moment) < CalcPoint.calcDistance(point_tips, point_moment)) {
                    index_maxdistancefinger = i;
                }
            }

            Imgproc.circle(frame, new Point(point_list_tips.get(index_maxdistancefinger).x, point_list_tips.get(index_maxdistancefinger).y), 5, LINE_COLOR_b, 5);

            // 凸包輪郭描画
            Imgproc.drawContours(frame, maxArea, -1, LINE_COLOR_G);

            // 重心描画
            Imgproc.line(frame, point_moment, point_moment, LINE_COLOR_W, 5);

            // 凹点集合描画
            ConvexityDefects.setColor(LINE_COLOR_B);
            ConvexityDefects.convexityDefects(frame, contours, OutlineDetector.getHullData(), true);


            // デバッグ用
            // 手の面積表示
            // Log.i("MainActivity",String.valueOf(Imgproc.contourArea(contours)));

            // 実画面とopencv viewのスケーリング
            float x = (float)point_list_tips.get(index_maxdistancefinger).x * mOpenCvCameraView.mScale;
            float y = (float)point_list_tips.get(index_maxdistancefinger).y * mOpenCvCameraView.mScale;
            if(ConvexityDefects.getPointsNumber() == 0) {
                check = 1;
            }

            // クリック判定とボタン動作
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
                    findViewById(R.id.Button_r).setBackgroundColor(0x96ffffff);
                    LogWriter.writeData("MainActivity_onCameraFrame_Touched button", "Button_r");
                }
                button_g.getLocationInWindow(location);
                if(x >= location[0] && x <= (location[0]+button_g.getWidth()) && y >= location[1] && y <= (location[1]+button_g.getHeight())) {
                    Log.i("MainActivity Touchtest", "Touched button_g");
                    findViewById(R.id.Button_g).setBackgroundColor(0x96ffffff);
                    LogWriter.writeData("MainActivity_onCameraFrame_Touched button", "Button_g");
                    // button_click(findViewById(R.id.Button_g));
                }
                button_b.getLocationInWindow(location);
                if(x >= location[0] && x <= (location[0]+button_b.getWidth()) && y >= location[1] && y <= (location[1]+button_b.getHeight())) {
                    Log.i("MainActivity Touchtest", "Touched button_b");
                    LogWriter.writeData("MainActivity_onCameraFrame_Touched button", "Button_b");
                    findViewById(R.id.Button_b).setBackgroundColor(0x96ffffff);
                    // button_click(findViewById(R.id.Button_b));
                }
            }
            if(ConvexityDefects.getPointsNumber() >= 2) {
                check = 0;
            }
        }

        // TODO 手の輪郭検出→領域少し拡大→マスク作成→Cannyでエッジ検出→ハフ変換で指の直線成分検出→→指の向きを知りたい
        /*
        // 参考 https://www.cellstat.net/makemask/ 領域からのマスク作成
        // ハフ変換テスト
        Mat frame_canny = new Mat();
        Imgproc.cvtColor(frame, frame_canny, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(frame_canny, frame_canny,100, 100);

        return frame_canny;
        */

        return frame;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown (MotionEvent event) {
            Log.i("MainActivity Gesture","Down ");
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            Log.i("MainActivity Gesture","SingleTapUp ");
            if(OutlineDetector.check==0) OutlineDetector.check=1;
            else OutlineDetector.check=0;
            return super.onSingleTapUp(event);
        }
        @Override
        public void onLongPress(MotionEvent event) {
            Log.i("MainActivity Gesture","LongPress ");
            byte[] data = SkinDetector.setSkinColorRange(frame);
            Context context = getApplicationContext();
            Toast t = Toast.makeText(context, "HSV調整: " + "H:" + Byte.toUnsignedInt(data[0]) + " S:" + Byte.toUnsignedInt(data[1]) + " V:" + Byte.toUnsignedInt(data[2]), Toast.LENGTH_LONG);
            t.show();
        }
    };

    public void onCameraViewStopped() {}

    // クリック検出用
    static int check = 0;
    static long lastmotionedtime = 0;

    private static final String TAG = "OpenCV/HandRecognition";
    private CameraBridgeViewBase mOpenCvCameraView;
    private GestureDetector mGestureDetector;
    public Mat frame;
}
