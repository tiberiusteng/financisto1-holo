package tw.tib.financisto.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import androidx.core.content.ContextCompat;

import java.util.Set;

import tw.tib.financisto.R;

public class PillSpan extends ReplacementSpan {

    private final int backgroundColor;
    private final int textColor;
    private final int strokeColor;
    private final float cornerRadius;
    private final float paddingHorizontal;
    private final float paddingVertical;
    private final float marginHorizontal;
    private final float marginVertical;

    public PillSpan(Context context) {
        this(context, false);
    }

    public PillSpan(Context context, boolean isCounter) {
        float density = context.getResources().getDisplayMetrics().density;
        if (isCounter) {
            this.backgroundColor = ContextCompat.getColor(context, R.color.tag_counter_bg);
            this.textColor = ContextCompat.getColor(context, R.color.tag_counter_text);
            this.strokeColor = ContextCompat.getColor(context, R.color.tag_counter_stroke);
        } else {
            this.backgroundColor = ContextCompat.getColor(context, R.color.tag_pill_bg);
            this.textColor = ContextCompat.getColor(context, R.color.tag_pill_text);
            this.strokeColor = ContextCompat.getColor(context, R.color.tag_pill_stroke);
        }
        this.cornerRadius = 5 * density;
        this.paddingHorizontal = 7 * density;
        this.paddingVertical = 2.5f * density;
        this.marginHorizontal = 2 * density;
        this.marginVertical = 2.5f * density;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        if (fm != null) {
            Paint.FontMetricsInt pfm = paint.getFontMetricsInt();
            int extra = (int) Math.ceil(paddingVertical + marginVertical);
            fm.ascent = pfm.ascent - extra;
            fm.descent = pfm.descent + extra;
            fm.top = pfm.top - extra;
            fm.bottom = pfm.bottom + extra;
        }
        float textWidth = paint.measureText(text, start, end);
        return (int) (textWidth + paddingHorizontal * 2 + marginHorizontal * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        float textWidth = paint.measureText(text, start, end);
        float pillLeft = x + marginHorizontal;
        float pillRight = pillLeft + textWidth + paddingHorizontal * 2;

        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        float pillTop = y + fm.ascent - paddingVertical;
        float pillBottom = y + fm.descent + paddingVertical;

        RectF rect = new RectF(pillLeft, pillTop, pillRight, pillBottom);

        int origColor = paint.getColor();
        Paint.Style origStyle = paint.getStyle();

        // 1. Draw rounded pill background
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // 2. Draw border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(strokeColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // 3. Draw text inside pill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        canvas.drawText(text, start, end, pillLeft + paddingHorizontal, y, paint);

        // Restore paint
        paint.setColor(origColor);
        paint.setStyle(origStyle);
    }

    public static CharSequence formatAsPills(Context context, Set<String> tags) {
        if (tags.isEmpty()) {
            return "";
        }

        var validTags = new java.util.ArrayList<String>();
        for (String tag : tags) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                validTags.add(trimmed);
            }
        }
        if (validTags.isEmpty()) {
            return "";
        }

        final int MAX_INLINE_TAGS = 4;
        boolean hasOverflow = validTags.size() > MAX_INLINE_TAGS;
        int displayCount = hasOverflow ? 3 : validTags.size();

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = 0; i < displayCount; i++) {
            if (ssb.length() > 0) {
                ssb.append(" ");
            }
            ssb.append(validTags.get(i), new PillSpan(context, false), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (hasOverflow) {
            int remaining = validTags.size() - displayCount;
            String counterText = context.getString(R.string.tags_more, remaining);
            if (ssb.length() > 0) {
                ssb.append(" ");
            }
            ssb.append(counterText, new PillSpan(context, true), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ssb;
    }
}
