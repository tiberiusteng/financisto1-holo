/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.RelativeLayout.LayoutParams;
import tw.tib.financisto.BuildConfig;
import tw.tib.financisto.R;
import tw.tib.financisto.activity.RequestPermission;
import tw.tib.financisto.utils.PicturesUtil;
import tw.tib.financisto.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class NodeInflater {

	private final LayoutInflater inflater;

	public NodeInflater(LayoutInflater inflater) {
		this.inflater = inflater;
	}

	public View addDivider(LinearLayout layout) {
		View divider = inflater.inflate(R.layout.edit_divider, layout, false);
		layout.addView(divider);
		return divider;
	}

	public class Builder {
		protected final LinearLayout layout;
		protected final View v;

		private boolean divider = true;

		public Builder(LinearLayout layout, int layoutId) {
			this.layout = layout;
			this.v = inflater.inflate(layoutId, layout, false);
		}

		public Builder(LinearLayout layout, View v) {
			this.layout = layout;
			this.v = v;
		}

		public Builder withId(int id) {
			v.setId(id);
			return this;
		}

		public Builder withId(int id, OnClickListener listener) {
			v.setId(id);
			v.setOnClickListener(listener);
			return this;
		}

		public Builder withLabel(int labelId) {
			TextView labelView = v.findViewById(R.id.label);
			labelView.setText(labelId);
			return this;
		}

		public Builder withLabel(String label) {
			TextView labelView = v.findViewById(R.id.label);
			labelView.setText(label);
			return this;
		}

		public Builder withData(int labelId) {
			TextView labelView = v.findViewById(R.id.data);
			labelView.setText(labelId);
			return this;
		}

		public Builder withData(String label) {
			TextView labelView = v.findViewById(R.id.data);
			labelView.setText(label);
			return this;
		}

		public Builder withIcon(int iconId) {
			ImageView iconView = v.findViewById(R.id.icon);
			iconView.setImageResource(iconId);
			return this;
		}

		public Builder withNoDivider() {
			divider = false;
			return this;
		}

		public View create() {
			layout.addView(v);
			if (divider) {
				View dividerView = addDivider(layout);
				v.setTag(dividerView);
			}
			return v;
		}

	}

	public class EditBuilder extends Builder {

		public EditBuilder(LinearLayout layout, View view) {
			super(layout, R.layout.select_entry_edit);
			RelativeLayout relativeLayout = v.findViewById(R.id.layout);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.label);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.label);
			relativeLayout.addView(view, layoutParams);
		}

	}

	public class EditColorBuilder extends Builder {
		public EditColorBuilder(LinearLayout layout, View view) {
			super(layout, R.layout.select_entry_palette);
			RelativeLayout relativeLayout = v.findViewById(R.id.layout);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.label);
			layoutParams.addRule(RelativeLayout.BELOW, R.id.label);
			relativeLayout.addView(view, layoutParams);
		}

		public EditColorBuilder withPaletteButtonId(int buttonId, OnClickListener listener) {
			ImageButton button = v.findViewById(R.id.palette);
			button.setId(buttonId);
			button.setOnClickListener(listener);
			return this;
		}
	}

	public class ListBuilder extends Builder {

		public ListBuilder(LinearLayout layout, int layoutId) {
			super(layout, layoutId);
		}

		public ListBuilder withButtonId(int buttonId, OnClickListener listener) {
			ImageView plusImageView = v.findViewById(R.id.plus_minus);
			plusImageView.setId(buttonId);
			plusImageView.setOnClickListener(listener);
			return this;
		}

		public ListBuilder withClearButtonId(int buttonId, OnClickListener listener) {
			ImageView plusImageView = v.findViewById(R.id.bMinus);
			plusImageView.setId(buttonId);
			plusImageView.setOnClickListener(listener);
			return this;
		}

		public ListBuilder withAutoCompleteFilter(OnClickListener listener, int toggleId) {
			final AutoCompleteTextView autoCompleteTxt = v.findViewById(R.id.autocomplete_filter);
			autoCompleteTxt.setFocusableInTouchMode(true);

			ToggleButton toggleBtn = v.findViewById(R.id.filterToggle);
			toggleBtn.setId(toggleId);
			toggleBtn.setOnClickListener(v1 -> {
				listener.onClick(v1);
				boolean filterVisible = toggleBtn.isChecked();

				autoCompleteTxt.setVisibility(filterVisible ? VISIBLE : GONE);
				v.findViewById(R.id.list_node_row).setVisibility(filterVisible ? GONE : VISIBLE);
				if (filterVisible) {
					autoCompleteTxt.setText("");
					Utils.openSoftKeyboard(autoCompleteTxt, layout.getContext());
				} else {
					Utils.closeSoftKeyboard(autoCompleteTxt, layout.getContext());
				}
			});

			return this;
		}

		public ListBuilder withAutoCompleteFilterShowHideView(OnClickListener listener, int hideViewId, int showListId, int createEntityId, boolean showCreateEntity) {
			final AutoCompleteTextView autoCompleteTxt = v.findViewById(R.id.autocomplete_filter);
			autoCompleteTxt.setFocusableInTouchMode(true);

			View showListButton = v.findViewById(R.id.show_list);
			View clearButton = v.findViewById(R.id.hide_filter);
			View createEntityButton = v.findViewById(R.id.create_entity);

			showListButton.setId(showListId);
			showListButton.setOnClickListener(listener);

			createEntityButton.setId(createEntityId);
			createEntityButton.setOnClickListener(listener);

			v.setOnClickListener(v1 -> {
				Log.d("AutoComplete", "onClickListener" );
				listener.onClick(v1);

				autoCompleteTxt.setVisibility(VISIBLE);
				showListButton.setVisibility(GONE);
				clearButton.setVisibility(VISIBLE);
				if (showCreateEntity) {
					createEntityButton.setVisibility(VISIBLE);
				}
				v.findViewById(R.id.list_node_row).setVisibility(GONE);
				autoCompleteTxt.setText("");
				Utils.openSoftKeyboard(autoCompleteTxt, layout.getContext());
			});

			clearButton.setId(hideViewId);
			clearButton.setOnClickListener(v1 -> {
				listener.onClick(v1);

				autoCompleteTxt.setVisibility(GONE);
				showListButton.setVisibility(VISIBLE);
				clearButton.setVisibility(GONE);
				createEntityButton.setVisibility(GONE);
				v.findViewById(R.id.list_node_row).setVisibility(VISIBLE);
				Utils.closeSoftKeyboard(autoCompleteTxt, layout.getContext());
			});

			return this;
		}

		public ListBuilder withoutMoreButton() {
			v.findViewById(R.id.more).setVisibility(GONE);
			return this;
		}
	}

	public class CheckBoxBuilder extends Builder {

		public CheckBoxBuilder(LinearLayout layout) {
			super(layout, R.layout.select_entry_checkbox);
		}

		public CheckBoxBuilder withCheckbox(boolean checked) {
			CheckBox checkBox = v.findViewById(R.id.checkbox);
			checkBox.setChecked(checked);
			return this;
		}

	}

	public class PictureBuilder extends ListBuilder {

		public PictureBuilder(LinearLayout layout) {
			super(layout, R.layout.select_entry_picture);
		}

		@Override
		public ListBuilder withButtonId(int buttonId, OnClickListener listener) {
			ImageView plusImageView = v.findViewById(R.id.plus_minus);
			plusImageView.setVisibility(VISIBLE);
			return super.withButtonId(buttonId, listener);
		}

		public PictureBuilder withPicture(final Context context, String pictureFileName) {
			final ImageView imageView = v.findViewById(R.id.picture);
			imageView.setOnClickListener(arg0 -> {
				String fileName = (String) imageView.getTag(R.id.attached_picture);
				if (fileName != null) {
					Uri target = PicturesUtil.getPictureFileUri(context, fileName);
					Log.i("withPicture", "fileName: " + fileName + ", target: " + target);
					DocumentFile pictureFile = DocumentFile.fromSingleUri(context, target);
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_VIEW);
					intent.setDataAndType(target, pictureFile.getType());
					intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					context.startActivity(intent);
				}
			});
			PicturesUtil.showImage(context, imageView, v.findViewById(R.id.data), pictureFileName);
			return this;
		}

	}

}
