package com.example.sciencelike.opencv_mobile;

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

    private static Scalar sLowerb;

    private static Scalar sUpperrb;

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

    public static MatOfPoint getMaxSkinArea(Mat rgba) {
        if (rgba == null) {
            throw new IllegalArgumentException("E SkinDetector parameter must not be null");
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);
        Imgproc.medianBlur(hsv, hsv, 3);

        Core.inRange(hsv, sLowerb, sUpperrb, hsv);

        Imgproc.morphologyEx(hsv, hsv, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_OPEN, new Size(3,3), new Point(-1, -1)));

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

        return contours.get(maxIdx);
    }
}