package com.example.sciencelike.opencv_mobile;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class OutlineDetector {
    private static OutlineDetector sInstance;

    private static Scalar sLowerb;

    private static Scalar sUpperrb;

    private OutlineDetector() {
        sLowerb = new Scalar(0, 60, 70);
        sUpperrb = new Scalar(20, 200, 230);
    }

    public static OutlineDetector getInstance() {
        if (sInstance != null) {
            return null;
        }
        sInstance = new OutlineDetector();
        return sInstance;
    }

    static MatOfPoint getLineData(Mat rgba) {
        if (rgba == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.medianBlur(hsv, hsv, 3);

        Core.inRange(hsv, sLowerb, sUpperrb, hsv);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
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

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contours.get(maxIdx), hull);

        return contours.get(maxIdx);
    }
}
