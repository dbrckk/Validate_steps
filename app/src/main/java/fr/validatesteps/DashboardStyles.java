package fr.validatesteps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

/** Native Material-inspired visuals. No remote assets or external UI SDK. */
public final class DashboardStyles {
    public static final int BG = 0xff09111e;
    public static final int SURFACE = 0xff152236;
    public static final int SURFACE_RAISED = 0xff1b2b42;
    public static final int TEXT = 0xfff4f7fd;
    public static final int MUTED = 0xffa6b4ca;
    public static final int ACCENT = 0xff4fe1bb;
    public static final int BLUE = 0xff83a9ff;
    public static final int WARNING = 0xffffcf78;

    private DashboardStyles() {}

    public static int dp(Context context, float value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }

    public static GradientDrawable round(Context context, int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(context, radius));
        return d;
    }

    public static GradientDrawable stroke(Context context, int fill, int border,
            float radius, float stroke) {
        GradientDrawable d = round(context, fill, radius);
        d.setStroke(dp(context, stroke), border);
        return d;
    }

    public static GradientDrawable gradient(Context context, int top, int bottom,
            float radius) {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        d.setCornerRadius(dp(context, radius));
        return d;
    }

    public static TextView text(Context context, String content, float sp, int color,
            boolean bold) {
        TextView label = new TextView(context);
        label.setText(content);
        label.setTextSize(sp);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setIncludeFontPadding(false);
        return label;
    }
}
