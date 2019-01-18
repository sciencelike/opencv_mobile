package com.example.sciencelike.opencv_mobile;

import android.os.Environment;
import android.os.SystemClock;

import org.opencv.core.MatOfPoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

class LogWriter {
    static private String filePath = Environment.getExternalStorageDirectory().getPath() + "/soturon/test.csv";

    static private boolean checkStorageState(){
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    static private void saveData(String str) {
        if(checkStorageState()) {
            File file = new File(filePath);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                 OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(outputStreamWriter)
            ) {
                bw.write(str);
                bw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // SkinDetector setSkinColorRange
    static void writeData(String tag, int h_u, int h_l, int s_u, int s_l, int v_u, int v_l) {
        String str = String.valueOf(SystemClock.uptimeMillis()) + "," + tag + "," + h_l + "," + s_l + "," + v_l + "," + h_u + "," + s_u + "," + v_u + "\n";
        saveData(str);
    }

    // OutlineDetector getLineData
    static void writeData(String tag, List<MatOfPoint> hullList) {
        String str = String.valueOf(SystemClock.uptimeMillis()) + "," + tag;

        for(int i=0; i<hullList.get(0).toList().size(); i++){
            str = str + "," + hullList.get(0).toList().get(i).x + "_" + hullList.get(0).toList().get(i).y;
        }
        str = str + "\n";
        saveData(str);
    }

    // PlayerActivity onCameraFrame Touched button
    static void writeData(String tag, int buttonNumber) {
        String str = String.valueOf(SystemClock.uptimeMillis()) + "," + tag + "," + buttonNumber + "\n";
        saveData(str);
    }

    // PlayerActivity onCameraFrame Pointer
    static void writeData(String tag, float x, float y) {
        String str = String.valueOf(SystemClock.uptimeMillis()) + "," + tag + "," + x + "," + y + "\n";
        saveData(str);
    }
}
