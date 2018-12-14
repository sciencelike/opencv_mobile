package com.example.sciencelike.opencv_mobile;

import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
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
