package com.example.easypaint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class PaintView extends View {

    //    Default size of the brush stroke in pixels
    private final int DEFAULT_BRUSH_SIZE = 20;
    private final int DEFAULT_STROKE_COLOR = Color.RED;
    private final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    private final Paint.Style DEFAULT_STROKE_STYLE = Paint.Style.STROKE;
    private final Paint.Cap DEFAULT_BRUSH_CAP = Paint.Cap.ROUND;

    //    Minimum pixels to be moved for touch to be registered
    private static final float TOUCH_TOLERANCE = 0;
    private float mX, mY;

    private Path mPath;
    private Paint mPaint;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int strokeColor = DEFAULT_STROKE_COLOR;
    private int backgroundColor = DEFAULT_BACKGROUND_COLOR;
    private int strokeWidth = DEFAULT_BRUSH_SIZE;
    private Paint.Style strokeStyle = DEFAULT_STROKE_STYLE;
    private Paint.Cap capStyle = DEFAULT_BRUSH_CAP;
    private boolean emboss;

    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    onViewTouchedListener listener;

    public interface onViewTouchedListener {
        void onPaintWindowTouch();
    }

    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);


        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }

    public void initPaint() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_STROKE_COLOR);
        mPaint.setStyle(strokeStyle);
        mPaint.setStrokeJoin(Paint.Join.ROUND);

        mPaint.setStrokeCap(capStyle);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);
    }

    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;


        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        listener = (onViewTouchedListener) getContext();

        strokeColor = DEFAULT_STROKE_COLOR;
        strokeWidth = DEFAULT_BRUSH_SIZE;

        initPaint();


    }

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
//        backgroundColor = DEFAULT_BACKGROUND_COLOR;
        paths.clear();
        normal();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss) {
                mPaint.setMaskFilter(mEmboss);
            } else if (fp.blur) {
                mPaint.setMaskFilter(mBlur);
            }

            mCanvas.drawPath(fp.path, mPaint);
        }

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(strokeColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        listener.onPaintWindowTouch();

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }

        return true;
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setPaint(Paint mPaint) {
        this.mPaint = mPaint;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setCapStyle(Paint.Cap capStyle) {
        this.capStyle = capStyle;
        initPaint();
    }
}
