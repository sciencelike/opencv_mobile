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
        // 参考
        // https://qiita.com/gutugutu3030/items/3907530ee49433420b37

        if (img == null || contour == null || hull == null || color == null) {
            throw new IllegalArgumentException("E SkinDetector parameter must not be null");
        }

        Point data[] = contour.toArray();
        MatOfInt4 convexityDefects = new MatOfInt4();
        Imgproc.convexityDefects(contour, hull, convexityDefects);
        // null check 入れる
        int cd[] = {0};
        try {
            cd = convexityDefects.toArray();
        }
        catch (RuntimeException e) {
            Log.i("ConvexityDefects","catch RuntimeException");
        }

        if(cd==null)return;
        for (int i = 0; i < cd.length; i += 4) {
            Imgproc.line(img, data[cd[i+2]], data[cd[i+2]], color, 3);
        }
    }
}
