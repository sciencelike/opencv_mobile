package com.example.sciencelike.opencv_mobile;

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

public class SkinDetector {
    private static SkinDetector sInstance;

    private static Scalar sLowerb = new Scalar(0, 60, 70);
    private static Scalar sUpperrb = new Scalar(25, 255, 255);

    /*
    private SkinDetector() {
        sLowerb = new Scalar(0, 60, 70);
        sUpperrb = new Scalar(25, 255, 255);
    }

    public static SkinDetector getInstance() {
        if (sInstance != null) {
            return null;
        }
        sInstance = new SkinDetector();
        return sInstance;
    }
    */

    public static void setSkinColorRange(int h_u, int h_l, int s_u, int s_l, int v_u, int v_l) {
        sLowerb = new Scalar(h_l, s_l, v_l);
        sUpperrb = new Scalar(h_u, s_u, v_u);

        Log.i("SkinDetector", "set hsv range: " + h_l + " " + s_l + " " + v_l + " → " + h_u + " " + s_u + " " + v_u);
    }

    public static MatOfPoint getMaxSkinArea(Mat rgba) {
        if (rgba == null) {
            throw new IllegalArgumentException("E SkinDetector parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.medianBlur(hsv, hsv, 3);
        Core.inRange(hsv, sLowerb, sUpperrb, hsv);

        Log.i("SkinDetector", "check hsv range: " + sLowerb.val[0] + " " + sLowerb.val[1] + " " + sLowerb.val[2] + " → " + sUpperrb.val[0] + " " + sUpperrb.val[1] + " " + sUpperrb.val[2]);

        // MORPH_OPEN → オープニング処理(小さい点を除去して、残った領域を膨張する)
        // MORPH_CROSS → オープニング処理をどの形でやるか CROSSは十字の形
        // Size → CROSSのサイズと多分基準点 -1 -1 で中心?

        Imgproc.morphologyEx(hsv, hsv, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3), new Point(-1, -1)));

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
        }else{
            return contours.get(maxIdx);
        }
    }
}