package com.example.sciencelike.opencv_mobile;

import android.os.SystemClock;
import android.util.Log;

import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class OutlineDetector {
    private static final MatOfInt hull = new MatOfInt();

    static MatOfInt getHullData() {
        return hull;
    }

    static List<MatOfPoint> getLineData(MatOfPoint contours) {
        if (contours == null) {
            throw new IllegalArgumentException("parameter must not be null");
        }

        // 輪郭取得
        List<MatOfPoint> hullList = new ArrayList<>();
        Imgproc.convexHull(contours, hull);

        Point[] contourArray = contours.toArray();

        // 距離が近い頂点を削除
        List<Point> hullPoint = new ArrayList<>();
        List<Integer> hullContourIdxList = new ArrayList<>(hull.toList());

        for (int i = 0; i < hullContourIdxList.size(); i++) {
            hullPoint.add(contourArray[hullContourIdxList.get(i)]);
        }

        try {
            Point point_moment = CalcPoint.calcMoment(contours);
            int size = hullContourIdxList.size();

            for (int i = 0; i < size; i++) {
                // 近さの基準は重心からの距離の平均*threshold_scaleとした
                double threshold_scale = 0.2;
                double threshold = (CalcPoint.calcDistance(hullPoint.get(i), point_moment) + CalcPoint.calcDistance(hullPoint.get(i + 1 == size ? 0 : i + 1), point_moment)) / 2 * threshold_scale;

                // Log.i("OutlineDetector", String.valueOf(i) + " " + String.valueOf(i+1 == size ? 0 : i+1) + " : " + String.valueOf(size));
                if (CalcPoint.calcDistance(hullPoint.get(i), hullPoint.get(i + 1 == size ? 0 : i + 1)) < threshold) {
                    // Log.i("OutlineDetector", "threshold " + String.valueOf(threshold) + " point-to-point " + String.valueOf(CalcPoint.calcDistance(hullPoint.get(i), hullPoint.get(i + 1))) + " p1-to-moment " + String.valueOf(CalcPoint.calcDistance(hullPoint.get(i), point_moment)) + " p2-to-moment " + String.valueOf(CalcPoint.calcDistance(hullPoint.get(i + 1), point_moment)));

                    // 領域の重心からの距離が長い方を残す
                    if (CalcPoint.calcDistance(hullPoint.get(i), point_moment) < CalcPoint.calcDistance(hullPoint.get(i + 1 == size ? 0 : i + 1), point_moment)) {
                        hullPoint.remove(hullPoint.get(i));
                        hullContourIdxList.remove(i);
                        size = hullPoint.size();
                    } else {
                        hullPoint.remove(hullPoint.get(i + 1 == size ? 0 : i + 1));
                        hullContourIdxList.remove(i + 1 == size ? 0 : i + 1);
                        size = hullPoint.size();
                    }
                    // if (i!=0) i-=1;
                    i -= 1;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.i("OutlineDetector", e.toString());
        }

        hull.fromList(hullContourIdxList);

        Point[] hullPoints = new Point[hullPoint.size()];
        for (int i = 0; i < hullPoint.size(); i++) {
            hullPoints[i] = hullPoint.get(i);
        }

        hullList.add(new MatOfPoint(hullPoints));

        if (hullList.size() == 0) {
            return null;
        } else {
            LogWriter.writeData(SystemClock.uptimeMillis(), "OutlineDetector_getLineData", hullList);
            return hullList;
        }
    }
}
