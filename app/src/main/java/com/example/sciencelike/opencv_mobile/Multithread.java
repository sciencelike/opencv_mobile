package com.example.sciencelike.opencv_mobile;

import android.content.Context;
import android.widget.LinearLayout;

class Multithread extends Thread {
    private final Context context;

    Multithread(Context context_arg) {
        context = context_arg;
    }

    public void run() {
        setAsyncTask();
    }
    private void setAsyncTask() {
        PlayerActivity.handler.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout pointer = ((com.example.sciencelike.opencv_mobile.PlayerActivity) context).findViewById(R.id.PointerCircle);
                int[] location = new int[2];
                pointer.getLocationOnScreen(location);
                float shift = pointer.getWidth();
                pointer.setX((int)PlayerActivity.getCursorPoint()[0]-shift/2);
                pointer.setY((int)PlayerActivity.getCursorPoint()[1]-shift/2);
            }
        });
    }
}
