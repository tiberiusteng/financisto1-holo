package tw.tib.financisto.activity;

import android.content.Context;
import android.widget.ImageButton;

import tw.tib.financisto.R;
import tw.tib.financisto.filter.WhereFilter;

class FilterState {

    static void updateFilterColor(Context context, WhereFilter filter, ImageButton button) {
        int color = filter.isEmpty() ? context.getResources().getColor(R.color.bottom_bar_tint) : context.getResources().getColor(R.color.holo_blue_dark);
        if (button != null) {
            button.setColorFilter(color);
        }
    }

}
