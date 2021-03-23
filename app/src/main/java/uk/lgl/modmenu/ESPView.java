package uk.lgl.modmenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Process;
import android.util.Log;
import android.view.View;
import java.util.Date;

public class ESPView extends View implements ESPViewInternalDelegate, Runnable {
    int FPS = 60;
    int esp_H;
    int esp_W;
    Paint mFilledPaint;
    Paint mStrokePaint;
    Paint mTextPaint;
    Thread mThread;
    long sleepTime;
    Date time;

    public ESPView(Context context) {
        super(context, null, 0);
        InitializePaints();
        setFocusableInTouchMode(false);
        setBackgroundColor(0);
        this.time = new Date();
        this.sleepTime = (long) (1000 / this.FPS);
        this.esp_W = FloatingModMenuService.getInstance().esp_W;
        this.esp_H = FloatingModMenuService.getInstance().esp_H;
        this.mThread = new Thread(this);
        this.mThread.start();
    }

    /* access modifiers changed from: protected */
    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        if (FloatingModMenuService.isReady() && canvas != null && getVisibility() == VISIBLE) {
            ClearCanvas(canvas);
            FloatingModMenuService.DrawOn(this, canvas, this.esp_W, this.esp_H);
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        Process.setThreadPriority(10);
        while (this.mThread.isAlive() && !this.mThread.isInterrupted()) {
            try {
                long currentTimeMillis = System.currentTimeMillis();
                postInvalidate();
                Thread.sleep(Math.max(Math.min(0L, this.sleepTime - (System.currentTimeMillis() - currentTimeMillis)), this.sleepTime));
            } catch (InterruptedException e) {
                Log.e("OverlayThread", e.getMessage());
            }
        }
    }

    public void InitializePaints() {
        this.mStrokePaint = new Paint();
        this.mStrokePaint.setAntiAlias(true);
        this.mFilledPaint = new Paint();
        this.mFilledPaint.setStyle(Paint.Style.FILL);
        this.mFilledPaint.setAntiAlias(true);
        this.mTextPaint = new Paint();
        this.mTextPaint.setTypeface(Typeface.MONOSPACE);
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void ClearCanvas(Canvas canvas) {
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
    }

    @Override
    public void DrawLine(Canvas canvas, String str, float f, float f2, float f3, float f4, float f5) {
        this.mStrokePaint.setColor(Color.parseColor(str));
        this.mStrokePaint.setStrokeWidth(f);
        canvas.drawLine(f2, f3, f4, f5, this.mStrokePaint);
    }

    @Override
    public void DrawText(Canvas canvas, String str, String str2, float f, float f2, float f3) {
        this.mTextPaint.setColor(Color.parseColor(str));
        if (getRight() > 1920 || getBottom() > 1920) {
            this.mTextPaint.setTextSize(f3 + 4.0f);
        } else if (getRight() == 1920 || getBottom() == 1920) {
            this.mTextPaint.setTextSize(f3 + 2.0f);
        } else {
            this.mTextPaint.setTextSize(f3);
        }
        canvas.drawText(str2, f, f2, this.mTextPaint);
    }

    @Override
    public void DrawCircle(Canvas canvas, String str, float f, float f2, float f3, float f4) {
        this.mStrokePaint.setColor(Color.parseColor(str));
        this.mStrokePaint.setStrokeWidth(f);
        canvas.drawCircle(f2, f3, f4, this.mStrokePaint);
    }

    @Override
    public void DrawFilledCircle(Canvas canvas, String str, float f, float f2, float f3) {
        this.mFilledPaint.setColor(Color.parseColor(str));
        canvas.drawCircle(f, f2, f3, this.mFilledPaint);
    }

    @Override
    public void DrawRect(Canvas canvas, String str, int i, float f, float f2, float f3, float f4) {
        this.mStrokePaint.setStrokeWidth((float) i);
        this.mStrokePaint.setColor(Color.parseColor(str));
        canvas.drawRect(f, f2, f + f3, f2 + f4, this.mStrokePaint);
    }

    @Override
    public void DrawFilledRect(Canvas canvas, String str, float f, float f2, float f3, float f4) {
        this.mFilledPaint.setColor(Color.parseColor(str));
        canvas.drawRect(f, f2, f + f3, f2 + f4, this.mFilledPaint);
    }
}