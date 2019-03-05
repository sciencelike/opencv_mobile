package com.example.sciencelike.opencv_mobile;

import android.os.SystemClock;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

class SkinDetector {
    private static Scalar sLowerb = new Scalar(0, 60, 70);
    private static Scalar sUpperrb = new Scalar(25, 255, 255);

    public static void setSkinMarker(Mat frame, Scalar color) {
        // 色調整用のマーカーを描画する
        int cols = frame.cols();
        int rows = frame.rows();
        double x = cols / 2.0, y = rows / 2.0;
        double range = rows / 6.0;

        Imgproc.circle(frame, new Point(x, y - range), 5, color, 2); // 上
        Imgproc.circle(frame, new Point(x, y), 5, color, 2); // 真ん中
        Imgproc.circle(frame, new Point(x, y + range), 5, color, 2); // 下
        Imgproc.circle(frame, new Point(x - range, y), 5, color, 2); // 右
        Imgproc.circle(frame, new Point(x + range, y), 5, color, 2); // 左
    }

    private static void setSkinColorRange(int h_u, int h_l, int s_u, int s_l, int v_u, int v_l) {
        sLowerb = new Scalar(h_l, s_l, v_l);
        sUpperrb = new Scalar(h_u, s_u, v_u);

        Log.i("SkinDetector", "set hsv range: " + h_l + " " + s_l + " " + v_l + " → " + h_u + " " + s_u + " " + v_u);
        LogWriter.writeData(SystemClock.uptimeMillis(), "SkinDetector_setSkinColorRange", h_l, s_l, v_l, h_u, s_u, v_u);
    }

    public static byte[] setSkinColorRange(Mat frame) {
        // hsvの範囲計算

        int cols = frame.cols();
        int rows = frame.rows();
        int range = rows / 6;
        int y = cols / 2, x = rows / 2;
        double rangescale = 0.30;

        Mat hsv = new Mat(cols, rows, CvType.CV_8U);

        byte[][] hsv_value = new byte[5][3];

        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_RGB2HSV, 3);

        hsv.get(x, y - range, hsv_value[0]); // 上
        hsv.get(x, y, hsv_value[1]); // 真ん中
        hsv.get(x, y + range, hsv_value[2]); // 下
        hsv.get(x - range, y, hsv_value[3]); // 右
        hsv.get(x + range, y, hsv_value[4]); // 左

        int h_u = 0, h_l = 255, s_u = 0, s_l = 255, v_u = 0, v_l = 255;

        for (int i = 0; i <= 4; i++) {
            if (Byte.toUnsignedInt(hsv_value[i][0]) > h_u)
                h_u = Byte.toUnsignedInt(hsv_value[i][0]);
            if (Byte.toUnsignedInt(hsv_value[i][0]) < h_l)
                h_l = Byte.toUnsignedInt(hsv_value[i][0]);
            if (Byte.toUnsignedInt(hsv_value[i][1]) > s_u)
                s_u = Byte.toUnsignedInt(hsv_value[i][1]);
            if (Byte.toUnsignedInt(hsv_value[i][1]) < s_l)
                s_l = Byte.toUnsignedInt(hsv_value[i][1]);
            if (Byte.toUnsignedInt(hsv_value[i][2]) > v_u)
                v_u = Byte.toUnsignedInt(hsv_value[i][2]);
            if (Byte.toUnsignedInt(hsv_value[i][2]) < v_l)
                v_l = Byte.toUnsignedInt(hsv_value[i][2]);
        }

        h_u += rangescale * (h_u + h_l) / 2;
        h_l -= rangescale * (h_u + h_l) / 2;
        s_u += rangescale * (s_u + s_l) / 2;
        s_l -= rangescale * (s_u + s_l) / 2;
        v_u += rangescale * (v_u + v_l) / 2;
        v_l -= rangescale * (v_u + v_l) / 2;

        setSkinColorRange(h_u, h_l, s_u, s_l, v_u, v_l);

        return hsv_value[1];
    }

    public static MatOfPoint getMaxSkinArea(Mat rgba) {
        if (rgba == null) {
            throw new IllegalArgumentException("E SkinDetector parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);

        // TODO ぼかしの種類をいろいろ試す
        // Imgproc.medianBlur(hsv, hsv, 3);
        Imgproc.blur(hsv, hsv, new Size(3, 3));

        Core.inRange(hsv, sLowerb, sUpperrb, hsv);

        // MORPH_OPEN → オープニング処理(小さい点を除去して、残った領域を膨張する)
        // MORPH_CROSS → オープニング処理をどの形でやるか CROSSは十字の形
        // Size → CROSSのサイズと多分基準点 -1 -1 で中心?

        Imgproc.morphologyEx(hsv, hsv, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3), new Point(-1, -1)));

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat(hsv.cols(), hsv.rows(), CvType.CV_32SC1);
        Imgproc.findContours(hsv, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        if (contours.size() == 0) {
            return null;
        }

        double maxArea = 0.0f;
        int maxIdx = 0;
        for (int i = 0; i < contours.size(); i++) {
            double tmpArea = Imgproc.contourArea(contours.get(i));
            if (maxArea < tmpArea) {
                maxArea = tmpArea;
                maxIdx = i;
            }
        }

        if (contours.size() == 0) {
            return null;
        } else {
            return contours.get(maxIdx);
        }
    }
}