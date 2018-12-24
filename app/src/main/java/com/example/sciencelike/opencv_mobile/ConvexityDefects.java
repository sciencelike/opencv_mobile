package com.example.sciencelike.opencv_mobile;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ConvexityDefects {
    static void convexityDefects(Mat img, MatOfPoint contour, MatOfInt hull, Scalar color) {
        // 凹点描画をする

        // 参考
        // https://qiita.com/gutugutu3030/items/3907530ee49433420b37

        if (img == null || contour == null || hull == null || color == null) {
            throw new IllegalArgumentException("E SkinDetector parameter must not be null");
        }

        // 凹点計算
        Point data[] = contour.toArray();
        MatOfInt4 convexityDefects = new MatOfInt4();
        Imgproc.convexityDefects(contour, hull, convexityDefects);

        // 後のために配列化
        int cd[] = {0};
        try {
            cd = convexityDefects.toArray();
        }
        catch (RuntimeException e) {
            Log.i("ConvexityDefects","catch RuntimeException");
        }

        if(cd==null || cd.length <= 0) return;

        // 角度が45度より小さい場合線を描画
        try {
            for (int i = 0; i < cd.length; i += 4) {
                if(cd[i+3] < 50000) continue;

                if (CalcPoint.calcAngle(data[cd[i]], data[cd[i+2]], data[cd[i+1]]) < 45) {
                    Imgproc.line(img, data[cd[i]], data[cd[i + 2]], color, 1);
                    Imgproc.line(img, data[cd[i + 1]], data[cd[i + 2]], color, 1);
                    // Log.i("ConvexityDefects","distance " + data[cd[i + 2]].x + " " + data[cd[i + 2]].y);
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            Log.i("ConvexityDefects", e.toString());
            return;
        }

        // 凹点描画
        for (int i = 0; i < cd.length; i += 4) {
            try {
                Imgproc.line(img, data[cd[i + 2]], data[cd[i + 2]], color, 5);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                Log.i("ConvexityDefects", e.toString());
                return;
            }
        }
    }
}
