package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.mtp.MtpConstants;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
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
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;


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
        // enableFullscreen();

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
    }

    // Load a network.
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera view start");
    }

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
                /*
                Intent intent2 = new Intent(this, PlayerActivity.class);
                startActivity(intent2);
                */

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
        MatOfInt hull = new MatOfInt();

        Point point_moment = new Point();

        if (contours != null) {
            // 手の最大面積の輪郭を取得
            maxArea = OutlineDetector.getLineData(contours);

            // 凹点描画のためのhullを取得
            Imgproc.convexHull(contours, hull);

            // 最大面積の輪郭から重心計算
            point_moment = CalcPoint.calcMoment(contours);
        }

        if (maxArea.size() > 0) {
            // 生最大面積輪郭描画
            List<MatOfPoint> temp = new ArrayList<>();
            temp.add(contours);
            Imgproc.drawContours(frame, temp, -1, LINE_COLOR_R);

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

            // 凹点集合描画
            ConvexityDefects.convexityDefects(frame, contours, hull, LINE_COLOR_B);

            // 重心描画
            Imgproc.line(frame, point_moment, point_moment, LINE_COLOR_W, 5);


            // デバッグ用
            // 手の面積表示
            // Log.i("MainActivity",String.valueOf(Imgproc.contourArea(contours)));

            // 実画面とopencv viewのスケーリング
            float x = (float)point_list_tips.get(index_maxdistancefinger).x * mOpenCvCameraView.mScale;
            float y = (float)point_list_tips.get(index_maxdistancefinger).y * mOpenCvCameraView.mScale;
            if(ConvexityDefects.getPointsNumber() == 0) {
                check = 1;
            }
            if(ConvexityDefects.getPointsNumber() == 1 && check == 1 && SystemClock.uptimeMillis() >= lastmotionedtime+1000) {
                Log.i("MainActivity Touchtest","Single Touch " + x + " " + y);
                lastmotionedtime = SystemClock.uptimeMillis();
                check = 0;

                /*
                MotionEvent ev = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis()+50, MotionEvent.ACTION_DOWN, x, y, 0);
                this.onTouchEvent(ev);
                ev = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis()+50, MotionEvent.ACTION_UP, x, y, 0);
                this.onTouchEvent(ev);
                ev.recycle();
                */
                
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
            return super.onSingleTapUp(event);
        }
        @Override
        public void onLongPress(MotionEvent event) {
            Log.i("MainActivity Gesture","LongPress ");
            int cols = frame.cols();
            int rows = frame.rows();

            int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

            int x = (int)event.getX() - xOffset;
            int y = (int)event.getY() - yOffset;

            byte[] data = SkinDetector.setSkinColorRange(frame);

            Context context = getApplicationContext();
            Toast t = Toast.makeText(context, "MainActivity Touch image point: (" + cols/2 + ", " + rows/2 + ")\n" + "R:" + Byte.toUnsignedInt(data[0]) + " G:" + Byte.toUnsignedInt(data[1]) + " B:" + Byte.toUnsignedInt(data[2]), Toast.LENGTH_SHORT);
            t.show();
        }
    };

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

    // クリック検出用
    static int check = 0;
    static long lastmotionedtime = 0;

    private static final String TAG = "OpenCV/HandRecognition";
    private CameraBridgeViewBase mOpenCvCameraView;
    private GestureDetector mGestureDetector;
    public Mat frame;
}
