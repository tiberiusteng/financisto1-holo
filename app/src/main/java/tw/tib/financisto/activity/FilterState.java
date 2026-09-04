package tw.tib.financisto.activity;

import android.content.Context;
import android.widget.ImageButton;

import tw.tib.financisto.R;
import tw.tib.financisto.filter.WhereFilter;

class FilterState {

    static void updateFilterColor(Context context, WhereFilter filter, ImageButton button) {
        updateFilterColor(context, filter, button, false);
    }

    /**
     * @param treatAsUnfiltered the filter is not empty, but everything in it came from
     *                          navigation rather than from the user. The account blotter is
     *                          the case: it always carries that account's criterion, yet the
     *                          user has not picked anything, so the icon should not be lit
     *                          (a lit icon means "what you see is not everything").
     */
    static void updateFilterColor(Context context, WhereFilter filter, ImageButton button,
                                  boolean treatAsUnfiltered) {
        boolean unfiltered = treatAsUnfiltered || filter.isEmpty();
        int color = unfiltered ? context.getResources().getColor(R.color.bottom_bar_tint) : context.getResources().getColor(R.color.holo_blue_bright);
        if (button != null) {
            button.setColorFilter(color);
        }
    }

}
