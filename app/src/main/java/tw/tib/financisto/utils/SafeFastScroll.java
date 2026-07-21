package tw.tib.financisto.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.AbsListView;

/**
 * Works around accidental jumps caused by ListView's built-in fast scroll:
 * the framework FastScroller keeps intercepting right-edge touches even after
 * the thumb has fully faded out, so touching near the right edge of an idle
 * list suddenly teleports it. Very easy to hit on long lists.
 *
 * Fix: toggle fast scroll dynamically. Keep it disabled while idle (the right
 * edge is not intercepted at all), enable it when scrolling starts (the thumb
 * becomes visible and grabbable), then disable it again shortly after
 * scrolling stops. Mirrors the androidx RecyclerView behavior where the thumb
 * is only grabbable while visible.
 */
public class SafeFastScroll {

    /** Delay before disabling fast scroll after scrolling stops (ms). The thumb's own fade-out is shorter, so this looks natural. */
    private static final long DISABLE_DELAY_MS = 1500;

    public static void attach(final AbsListView list) {
        list.setFastScrollEnabled(false);   // disabled while idle: no invisible interception
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable disable = () -> list.setFastScrollEnabled(false);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    handler.postDelayed(disable, DISABLE_DELAY_MS);
                } else {
                    handler.removeCallbacks(disable);
                    if (!view.isFastScrollEnabled()) {
                        view.setFastScrollEnabled(true);
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // While dragging the thumb the state may stay IDLE, but onScroll
                // keeps firing; treat it as activity and postpone disabling, so the
                // thumb does not vanish under the user's finger.
                if (view.isFastScrollEnabled()) {
                    handler.removeCallbacks(disable);
                    handler.postDelayed(disable, DISABLE_DELAY_MS);
                }
            }
        });
    }
}
