package tw.tib.financisto.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.core.content.res.ResourcesCompat;

import greendroid.widget.QuickAction;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 7/25/11 9:56 PM
 */
public class MyQuickAction extends QuickAction {
    public static int NO_FILTER = -1;

    private static final ColorFilter BLACK_CF = new LightingColorFilter(Color.BLACK, Color.BLACK);

    public MyQuickAction(Context ctx, int drawableId, int titleId) {
        super(ctx, buildDrawable(ctx, drawableId), titleId);
    }

    public MyQuickAction(Context ctx, int drawableId, @ColorInt int color, int titleId) {
        super(ctx, buildColorDrawable(ctx, color, drawableId), titleId);
    }

    private static Drawable buildDrawable(Context ctx, int drawableId) {
        Drawable d = ResourcesCompat.getDrawable(ctx.getResources(), drawableId, null).mutate();
        d.setColorFilter(BLACK_CF);
        return d;
    }

    private static Drawable buildColorDrawable(Context ctx, @ColorInt int color, int drawableId) {
        if (color == NO_FILTER) {
            return ResourcesCompat.getDrawable(ctx.getResources(), drawableId, null);
        }
        else {
            Drawable d = ResourcesCompat.getDrawable(ctx.getResources(), drawableId, null).mutate();
            d.setColorFilter(new LightingColorFilter(Color.BLACK, color));
            return d;
        }
    }
}
