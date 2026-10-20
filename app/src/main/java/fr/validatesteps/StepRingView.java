package fr.validatesteps;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.text.NumberFormat;
import java.util.Locale;

/** Anti-aliased, animated progress ring. Only displays actual Health Connect steps. */
public final class StepRingView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private long steps = -1L;
    private float displayed;
    private ValueAnimator currentAnimation;
    private static final float GOAL = 10000f;

    public StepRingView(Context context) {
        super(context);
        setContentDescription("Pas du jour inconnus");
    }

    public void setSteps(long value) {
        // Never let a superseded animator overwrite the latest request.
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }
        steps = value;
        float target = value < 0 ? 0f : Math.min(1f, value / GOAL);
        if (value < 0) {
            // Unknown is not zero; clear the visual arc immediately too.
            displayed = 0f;
            invalidate();
        } else {
            currentAnimation = ValueAnimator.ofFloat(displayed, target);
            currentAnimation.setDuration(540);
            currentAnimation.addUpdateListener(a -> {
                displayed = (float) a.getAnimatedValue();
                invalidate();
            });
            currentAnimation.start();
        }
        setContentDescription(value < 0
            ? "Pas du jour non disponibles" : value + " pas aujourd'hui");
    }

    @Override protected void onDetachedFromWindow() {
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }
        super.onDetachedFromWindow();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int desired = DashboardStyles.dp(getContext(), 218);
        int w = resolveSize(desired, widthSpec);
        int h = resolveSize(desired, heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float radius = Math.min(w, h) * .5f - DashboardStyles.dp(getContext(), 15);
        float cx = w / 2f, cy = h / 2f;
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(DashboardStyles.dp(getContext(), 13));
        p.setColor(0xff2a4861);
        canvas.drawArc(bounds, -90, 360, false, p);
        p.setColor(DashboardStyles.ACCENT);
        canvas.drawArc(bounds, -90, displayed * 360, false, p);

        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(DashboardStyles.MUTED);
        p.setTextSize(DashboardStyles.dp(getContext(), 13));
        canvas.drawText("PAS AUJOURD'HUI", cx, cy - DashboardStyles.dp(getContext(), 25), p);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setColor(DashboardStyles.TEXT);
        p.setTextSize(DashboardStyles.dp(getContext(), 35));
        String count = steps < 0 ? "—" :
            NumberFormat.getIntegerInstance(Locale.FRANCE).format(steps);
        canvas.drawText(count, cx, cy + DashboardStyles.dp(getContext(), 15), p);
        p.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        p.setColor(DashboardStyles.MUTED);
        p.setTextSize(DashboardStyles.dp(getContext(), 12));
        canvas.drawText("OBJECTIF 10 000", cx, cy + DashboardStyles.dp(getContext(), 39), p);
    }
}
