package com.example.sciencelike.opencv_mobile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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

// フルスクリーン表示のために追加
// カメラ権限関連


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
        enableFullscreen();

        // 画面消灯を無効化する
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // カメラ起動
    public void launchCamera() {
        mOpenCvCameraView = findViewById(R.id.CameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        // ここでカメラの最大解像度を設定する
        mOpenCvCameraView.setMaxFrameSize(1000, 1000);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    // ナビゲーションバーを隠す
    public void enableFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    // Load a network.
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera view start");
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
        }

        return frame;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int cols = frame.cols();
            int rows = frame.rows();

            int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

            int x = (int)event.getX() - xOffset;
            int y = (int)event.getY() - yOffset;

            // byte[] data = new byte[frame.channels()];

            byte[] data = SkinDetector.setSkinColorRange(frame);

            // Mat temp = new Mat();
            // frame.convertTo(temp, CvType.CV_8UC3);

            // Log.i(TAG, "Touch image point: (" + cols/2 + ", " + rows/2 + ")");
            Toast t = Toast.makeText(this, " Touch image point: (" + cols/2 + ", " + rows/2 + ")\n" + "R:" + Byte.toUnsignedInt(data[0]) + " G:" + Byte.toUnsignedInt(data[1]) + " B:" + Byte.toUnsignedInt(data[2]), Toast.LENGTH_SHORT);
            // View v = t.getView();
            // v.setBackgroundColor(Color.rgb((int)data[0], (int)data[1], (int)data[2]));
            t.show();
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private final GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent event) {
            Log.i("Gesture","LongPress ");
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

    private static final String TAG = "OpenCV/HandRecognition";
    private CameraBridgeViewBase mOpenCvCameraView;
    private GestureDetector mGestureDetector;
    public Mat frame;
}
