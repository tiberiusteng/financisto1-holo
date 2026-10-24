package tw.tib.financisto.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import tw.tib.financisto.R;

/**
 * The Transaction | Transfer | Update balance tabs in the title bar of the transaction forms.
 * Styled like the main screen's tab row: dark grey bar, tabs filling the row evenly, the selected
 * one in white with an accent underline, the others grey.
 * Only the view and the taps; whether a switch is possible and what it carries is decided by
 * {@link AbstractTransactionActivity}.
 */
final class FormKindBar {

	interface Listener {
		void onKindTapped(FormSwitch.Kind kind);
	}

	// same colours as the main screen's TabLayout (colorPrimary / colorAccent of the AppCompat dark theme)
	private static final int BAR = 0xFF212121;
	private static final int ACCENT = 0xFF80CBC4;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFF808080;
	private static final int TEXT_DISABLED = 0xFF4A4A4A;
	private static final int[] TITLES = {R.string.transaction, R.string.transfer, R.string.update_balance};

	private final Activity activity;
	private final FormSwitch.Kind current;
	private final LinearLayout row;
	private final TextView[] tabs = new TextView[FormSwitch.Kind.values().length];

	FormKindBar(Activity activity, FormSwitch.Kind current, Listener listener) {
		this.activity = activity;
		this.current = current;
		row = new LinearLayout(activity);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		for (FormSwitch.Kind kind : FormSwitch.Kind.values()) {
			TextView v = new TextView(activity);
			v.setText(TITLES[kind.ordinal()]);
			v.setSingleLine(true);
			v.setGravity(Gravity.CENTER);
			v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			v.setPadding(dp(14), dp(12), dp(14), dp(12));
			boolean selected = kind == current;
			v.setTextColor(selected ? TEXT : TEXT_DIM);
			if (selected) {
				v.setBackground(underline());
			} else {
				v.setBackground(selectableBackground());
				v.setOnClickListener(x -> listener.onKindTapped(kind));
			}
			row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
			tabs[kind.ordinal()] = v;
		}
	}

	/** Selection mark: a 2dp accent line along the bottom, as wide as the tab (as on the main screen). */
	private Drawable underline() {
		LayerDrawable d = new LayerDrawable(new Drawable[]{new ColorDrawable(ACCENT)});
		d.setLayerGravity(0, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL);
		d.setLayerHeight(0, dp(2));
		return d;
	}

	private Drawable selectableBackground() {
		TypedValue tv = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
		return activity.getDrawable(tv.resourceId);
	}

	/** A kind that cannot be switched to is greyed out and not tappable (no message: the grey says it). */
	void setAvailable(FormSwitch.Kind kind, boolean available) {
		if (kind == current) return;
		TextView v = tabs[kind.ordinal()];
		v.setTextColor(available ? TEXT_DIM : TEXT_DISABLED);
		v.setEnabled(available);
		v.setClickable(available);
	}

	/**
	 * Puts the tabs into the title bar in place of the title and icon, on the main screen's dark
	 * grey. Replacing the background also removes the Holo bar's blue bottom line, which would
	 * otherwise run under the selected tab's underline. Does nothing without an action bar.
	 */
	boolean install() {
		ActionBar actionBar = activity.getActionBar();
		if (actionBar == null) return false;
		actionBar.setBackgroundDrawable(new ColorDrawable(BAR));
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(row, new ActionBar.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		paintStatusBar();
		return true;
	}

	/**
	 * Paints the status bar the same grey, so the bar runs from the top of the screen down to the
	 * tab underline as on the main screen. The Holo theme of the transaction forms does not let the
	 * app draw system bar backgrounds (the status bar stays black), so the flag has to be set first.
	 */
	private void paintStatusBar() {
		Window w = activity.getWindow();
		w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		w.setStatusBarColor(BAR);
	}

	private int dp(int v) {
		return Math.round(v * activity.getResources().getDisplayMetrics().density);
	}
}
