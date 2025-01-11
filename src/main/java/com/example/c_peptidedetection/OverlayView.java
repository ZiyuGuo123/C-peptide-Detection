// OverlayView.java

package com.example.c_peptidedetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {

    private Paint paint;
    private float centerX, centerY, radius;
    private boolean drawCircle = false;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.darker_gray));
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(100);  // 设置透明度
    }

    public void setCircle(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        drawCircle = true;
        invalidate();
    }

    public void clearCircle() {
        drawCircle = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (drawCircle) {
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }
}
