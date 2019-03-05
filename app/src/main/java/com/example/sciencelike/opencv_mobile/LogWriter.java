package com.example.sciencelike.opencv_mobile;

import android.os.Environment;

import org.opencv.core.MatOfPoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

class LogWriter {
    static private final String filePath_all = Environment.getExternalStorageDirectory().getPath() + "/log_all.csv";
    static private final String filePath_part = Environment.getExternalStorageDirectory().getPath() + "/log_part.csv";

    static private boolean checkStorageState() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    static private void saveData_all(String str) {
        if (checkStorageState()) {
            File file = new File(filePath_all);
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

    static private void saveData_part(String str) {
        if (checkStorageState()) {
            File file = new File(filePath_part);
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
    static void writeData(long time, String tag, int h_u, int h_l, int s_u, int s_l, int v_u, int v_l) {
        String str = String.valueOf(time) + "," + tag + "," + h_l + "," + s_l + "," + v_l + "," + h_u + "," + s_u + "," + v_u + "\n";
        saveData_all(str);
    }

    // OutlineDetector getLineData
    static void writeData(long time, String tag, List<MatOfPoint> hullList) {
        StringBuilder str = new StringBuilder(String.valueOf(time) + "," + tag);

        for (int i = 0; i < hullList.get(0).toList().size(); i++) {
            str.append(",").append(hullList.get(0).toList().get(i).x).append("_").append(hullList.get(0).toList().get(i).y);
        }
        str.append("\n");
        saveData_all(str.toString());
    }

    // MainActivity + PlayerActivity onCameraFrame Touched button
    static void writeData(long time, String tag, String buttonId) {
        String str = String.valueOf(time) + "," + tag + "," + buttonId + "\n";
        saveData_all(str);
        saveData_part(str);
    }

    // PlayerActivity onCameraFrame Touched button
    static void writeData(long time, String tag, int buttonOrder) {
        String str = String.valueOf(time) + "," + tag + "," + buttonOrder + "\n";
        saveData_all(str);
        if (tag.equals("PlayerActivity_buttonclick_buttonorder")) saveData_part(str);
    }

    // PlayerActivity onCameraFrame Pointer
    static void writeData(long time, String tag, float x, float y) {
        String str = String.valueOf(time) + "," + tag + "," + x + "," + y + "\n";
        saveData_all(str);
    }

    // MainActivity onCreate camerasizeHW
    static void writeData(long time, String tag, int height, int width) {
        String str = String.valueOf(time) + "," + tag + "," + height + "," + width + "\n";
        saveData_all(str);
    }

    // PlayerActivity activity change
    static void writeData(long time, String string) {
        String str = String.valueOf(time) + "," + "etc" + "," + string + "\n";
        saveData_all(str);
    }
}
