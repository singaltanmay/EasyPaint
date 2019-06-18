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

import java.io.File;
import java.io.FileOutputStream;
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

    private Path brushPath;
    private Paint brushPaint;
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
    private Bitmap backgroundBitmap;
    private Canvas mCanvas;


    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    private ArrayList<Sticker> allStickers = new ArrayList<>();

    public void addSticker(Bitmap bitmap) {
        Sticker sticker = new Sticker(bitmap);

        allStickers.add(sticker);
    }

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
        brushPaint = new Paint();
        brushPaint.setAntiAlias(true);
        brushPaint.setDither(true);
        brushPaint.setColor(DEFAULT_STROKE_COLOR);
        brushPaint.setStyle(strokeStyle);
        brushPaint.setStrokeJoin(Paint.Join.ROUND);

        brushPaint.setStrokeCap(capStyle);
        brushPaint.setXfermode(null);
        brushPaint.setAlpha(0xff);
    }

    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(backgroundBitmap);

        listener = (onViewTouchedListener) getContext();

        strokeColor = DEFAULT_STROKE_COLOR;
        strokeWidth = DEFAULT_BRUSH_SIZE;

        initPaint();


    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (FingerPath fp : paths) {
            brushPaint.setColor(fp.color);
            brushPaint.setStrokeWidth(fp.strokeWidth);
            brushPaint.setMaskFilter(null);

            if (fp.emboss) {
                brushPaint.setMaskFilter(mEmboss);
            } else if (fp.blur) {
                brushPaint.setMaskFilter(mBlur);
            }

            mCanvas.drawPath(fp.path, brushPaint);
        }

//        if (importedBitmap != null) {
////            canvas.drawBitmap(importedBitmap, 0, 0, mBitmapPaint);
////        }


        for (Sticker sticker : allStickers) {
            sticker.draw(canvas);
        }

        canvas.drawBitmap(backgroundBitmap, 0, 0, mBitmapPaint);

        canvas.restore();
    }

    private void touchStart(float x, float y) {
        brushPath = new Path();
        FingerPath fp = new FingerPath(strokeColor, emboss, blur, strokeWidth, brushPath);
        paths.add(fp);

        brushPath.reset();
        brushPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            brushPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        brushPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        listener.onPaintWindowTouch();

        for (Sticker sticker : allStickers) {
            sticker.handleOnTouchEvent(event);
        }

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

    public Bitmap exportCanvas() {
        return backgroundBitmap;
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

    public void setStrokeColor(int color) {
        this.strokeColor = color;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }

    public Paint getPaint() {
        return brushPaint;
    }

    public void setPaint(Paint mPaint) {
        this.brushPaint = mPaint;
        invalidate();
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

    public boolean isEmboss() {
        return emboss;
    }

    public boolean isBlur() {
        return blur;
    }

    public void setEmboss(boolean emboss) {
        this.emboss = emboss;
    }

    public void setBlur(boolean blur) {
        this.blur = blur;
    }
}
