package com.example.sciencelike.opencv_mobile;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static com.example.sciencelike.opencv_mobile.CalcPoint.calcAngle;
import static com.example.sciencelike.opencv_mobile.CalcPoint.calcDistance;
import static com.example.sciencelike.opencv_mobile.CalcPoint.calcMoment;

class ConvexityDefects {
    static private final MatOfInt4 convexityDefects = new MatOfInt4();
    static private int points = 0;
    static private Scalar color = new Scalar(0,0,0);

    static int getPointsNumber() {
        return points;
    }

    static void setColor(Scalar color_arg) {
        color = color_arg;
    }

    static void convexityDefects(Mat img, MatOfPoint contour, MatOfInt hull, boolean draw) {
        // 凹点描画をする

        // 参考
        // https://qiita.com/gutugutu3030/items/3907530ee49433420b37

        if (img == null || contour == null || hull == null || color == null) {
            throw new IllegalArgumentException("E ConvexityDefects parameter must not be null");
        }

        // 凹点計算
        Point data[] = contour.toArray();
        Imgproc.convexityDefects(contour, hull, convexityDefects);

        // 凹点カウント
        int point=0;

        // 後のために配列化
        int cd[] = {0};
        try {
            if(!convexityDefects.empty()) {
                cd = convexityDefects.toArray();
            }
        }
        catch (RuntimeException e) {
            // Log.i("ConvexityDefects","catch RuntimeException");
        }

        if(cd==null || cd.length <= 0) return;

        // 角度が指定より小さい場合 & 距離が指定より短い場合線を描画 + 凹点として採用
        try {
            Point point_moment = calcMoment(contour);
            for (int i = 0; i < cd.length; i += 4) {
                if ((calcDistance(data[cd[i]], data[cd[i+2]]) + calcDistance(data[cd[i+1]], data[cd[i+2]]))/2 > (calcDistance(data[cd[i]], point_moment) + calcDistance(data[cd[i+1]], point_moment))/2 /3 &&
                calcAngle(data[cd[i]], data[cd[i+2]], data[cd[i+1]]) < 90) {
                    point+=1;
                    if(draw) {
                        // 凸点から凹点までの線描画
                        Imgproc.line(img, data[cd[i]], data[cd[i + 2]], color);
                        Imgproc.line(img, data[cd[i + 1]], data[cd[i + 2]], color);
                        // 凹点描画
                        Imgproc.line(img, data[cd[i + 2]], data[cd[i + 2]], color, 5);
                    }
                }
            }
            points = point;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // Log.i("ConvexityDefects", e.toString());
        }
    }
}
