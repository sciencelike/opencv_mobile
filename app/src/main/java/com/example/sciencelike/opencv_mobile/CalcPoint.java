package com.example.sciencelike.opencv_mobile;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class CalcPoint {
    static double calcAngle (Point p1, Point p2, Point p3) {
        // p2を中心とした角度計算
        double c_1x = p1.x - p2.x;
        double c_1y = p1.y - p2.y;
        double c_2x = p3.x - p2.x;
        double c_2y = p3.y - p2.y;

        double cos_theta = (c_1x * c_2x + c_1y * c_2y) / (Math.sqrt(c_1x * c_1x + c_1y * c_1y) * Math.sqrt(c_2x * c_2x + c_2y * c_2y));

        return Math.toDegrees(Math.acos(cos_theta));
    }

    static double calcDistance (Point p1, Point p2) {
        // 二点からの距離計算
        return Math.sqrt((p2.x - p1.x)*(p2.x - p1.x) + (p2.y - p1.y)*(p2.y - p1.y));
    }

    static Point calcMoment (MatOfPoint contours) {
        // 領域からの重心計算
        Moments mu = Imgproc.moments(contours);
        return new Point(mu.m10/mu.m00, mu.m01/mu.m00);
    }
}
