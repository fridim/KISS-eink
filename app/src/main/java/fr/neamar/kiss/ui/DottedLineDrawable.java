package fr.neamar.kiss.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Mudita Mindful Design: Custom drawable for 1px dotted black line
 * Used as list divider for e-ink optimized display
 */
public class DottedLineDrawable extends Drawable {
    private final Paint paint;
    private final int height;

    public DottedLineDrawable() {
        this(0xFF000000, 1, 2, 4); // Black, 1dp stroke, 2dp dot, 4dp gap
    }

    public DottedLineDrawable(int color, int strokeWidth, float dashWidth, float dashGap) {
        paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashGap}, 0));
        paint.setAntiAlias(false); // Sharp pixels for e-ink
        this.height = strokeWidth;
    }

    @Override
    public void draw(Canvas canvas) {
        int y = getBounds().centerY();
        canvas.drawLine(getBounds().left, y, getBounds().right, y, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }
}
