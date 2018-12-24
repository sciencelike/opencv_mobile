package com.example.sciencelike.opencv_mobile;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class OutlineDetector {
    private static OutlineDetector sInstance;

    public static OutlineDetector getInstance() {
        if (sInstance != null) {
            return null;
        }
        sInstance = new OutlineDetector();
        return sInstance;
    }

    public static void setSkinMarker(Mat frame, Scalar color) {
        int cols = frame.cols();
        int rows = frame.rows();
        double x = cols/2, y = rows/2;
        double range = rows/6.0;

        Imgproc.circle(frame, new Point(x, y - range), 5, color, 2); // 上
        Imgproc.circle(frame, new Point(x, y), 5, color, 2); // 真ん中
        Imgproc.circle(frame, new Point(x, y + range), 5, color, 2); // 下
        Imgproc.circle(frame, new Point(x - range, y), 5, color, 2); // 右
        Imgproc.circle(frame, new Point(x + range, y), 5, color, 2); // 左
    }

    public static byte[] setSkinColorRange(Mat frame) {
        int cols = frame.cols();
        int rows = frame.rows();
        int range = rows/6;
        int y = cols/2, x = rows/2;
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

        for (int i = 0; i <= 4; i++){
            if (Byte.toUnsignedInt(hsv_value[i][0]) > h_u) h_u = Byte.toUnsignedInt(hsv_value[i][0]);
            if (Byte.toUnsignedInt(hsv_value[i][0]) < h_l) h_l = Byte.toUnsignedInt(hsv_value[i][0]);
            if (Byte.toUnsignedInt(hsv_value[i][1]) > s_u) s_u = Byte.toUnsignedInt(hsv_value[i][1]);
            if (Byte.toUnsignedInt(hsv_value[i][1]) < s_l) s_l = Byte.toUnsignedInt(hsv_value[i][1]);
            if (Byte.toUnsignedInt(hsv_value[i][2]) > v_u) v_u = Byte.toUnsignedInt(hsv_value[i][2]);
            if (Byte.toUnsignedInt(hsv_value[i][2]) < v_l) v_l = Byte.toUnsignedInt(hsv_value[i][2]);
        }

        h_u += rangescale * (h_u + h_l) / 2;
        h_l -= rangescale * (h_u + h_l) / 2;
        s_u += rangescale * (s_u + s_l) / 2;
        s_l -= rangescale * (s_u + s_l) / 2;
        v_u += rangescale * (v_u + v_l) / 2;
        v_l -= rangescale * (v_u + v_l) / 2;

        SkinDetector.setSkinColorRange(h_u, h_l, s_u, s_l, v_u, v_l);

        return hsv_value[1];
    }

    static List<MatOfPoint> getLineData(MatOfPoint contours) {
        if (contours == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }

        List<MatOfPoint> hullList = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contours, hull);

        Point[] contourArray = contours.toArray();
        Point[] hullPoints = new Point[hull.rows()];
        List<Integer> hullContourIdxList = hull.toList();

        for (int i = 0; i < hullContourIdxList.size(); i++) {
            hullPoints[i] = contourArray[hullContourIdxList.get(i)];
        }

        hullList.add(new MatOfPoint(hullPoints));

        if (hullList.size() == 0) {
            return null;
        }else{
            return hullList;
        }
    }
}
