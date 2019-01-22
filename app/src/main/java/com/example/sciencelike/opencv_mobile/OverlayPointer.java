package com.example.sciencelike.opencv_mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class OverlayPointer extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    // 参考
    // https://qiita.com/circularuins/items/a61c5e7149f355a54a8b

    private final SurfaceHolder holder;
    private Thread thread;
    private boolean isAttached = true;

    private int x=0;
    private int y=0;

    //コンストラクタ
    public OverlayPointer(Context context) {
        super(context);

        holder = this.getHolder();
        holder.addCallback(this);

        //ビューの背景を透過させる
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //最前面に描画する
        setZOrderOnTop(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new Thread(this);
        thread.start();

        /*
        Canvas canvas = holder.lockCanvas();

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //描画例（円を描く）
        Paint p = new Paint();
        p.setARGB(255, 255, 0, 0);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, 10, p);

        Log.i("OverlayPointer Pointtest 1", "Cursor Point" + x + " " + y);

        holder.unlockCanvasAndPost(canvas);
        */
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int a, int b, int c) {
        Log.d("OverlayPointer", "change");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        Log.d("OverlayPointer", "destroy");
        isAttached = false;
        thread = null;
    }

    @Override
    public void run() {
        // メインループ（無限ループ）
        while(isAttached){
            // 表示位置
            x = (int)PlayerActivity.getCursorPoint()[0];
            y = (int)PlayerActivity.getCursorPoint()[1];
            // Log.i("OverlayPointer Pointtest 2", "Cursor Point" + x + " " + y);

            //描画処理を開始
            Canvas canvas;
            try {
                canvas = holder.lockCanvas();
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                Paint paint = new Paint();
                paint.setARGB(255/2, 255, 0, 0);
                canvas.drawCircle(x, y, 20, paint);

                //描画処理を終了
                holder.unlockCanvasAndPost(canvas);
            }
            catch (NullPointerException e){
                Log.d("OverlayPointer", e.toString());
            }
        }
    }

}