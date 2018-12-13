package com.example.sciencelike.opencv_mobile;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class OutlineDetector {
    private static OutlineDetector sInstance;

    private static Scalar sLowerb;

    private static Scalar sUpperrb;

    private OutlineDetector() {
        sLowerb = new Scalar(0, 60, 50);
        sUpperrb = new Scalar(20, 200, 255);
    }

    public static OutlineDetector getInstance() {
        if (sInstance != null) {
            return null;
        }
        sInstance = new OutlineDetector();
        return sInstance;
    }

    static List<MatOfPoint> getLineData(Mat rgba) {
        if (rgba == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.medianBlur(hsv, hsv, 3);
        Core.inRange(hsv, sLowerb, sUpperrb, hsv);

        // MORPH_OPEN → オープニング処理(小さい点を除去して、残った領域を膨張する)
        // MORPH_CROSS → オープニング処理をどの形でやるか CROSSは十字の形
        // Size → CROSSのサイズと多分基準点 -1 -1 で中心?

        Imgproc.morphologyEx(hsv, hsv, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(5,5), new Point(-1, -1)));

        List<MatOfPoint> contours = new ArrayList<>();
        // Mat hierarchy = new Mat(hsv.cols(), hsv.rows(), CvType.CV_32SC1);
        Mat hierarchy = new Mat();
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

        List<MatOfPoint> hullList = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contours.get(maxIdx), hull);

        Point[] contourArray = contours.get(maxIdx).toArray();
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
