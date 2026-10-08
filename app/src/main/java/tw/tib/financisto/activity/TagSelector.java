package tw.tib.financisto.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import tw.tib.financisto.Application;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.StringUtil;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.view.PillSpan;

public class TagSelector<A extends AbstractActivity> {
    private static final String TAG = "TagSelector";

    private final A activity;
    private final DatabaseAdapter db;
    private final ActivityLayout x;
    private final boolean isShow;

    private final int actBtnId;
    private final int clrBtnId;
    private final int defaultValueResId;

    private View node;
    private TextView text;
    private AutoCompleteTextView autoCompleteFilter;
    private SimpleCursorAdapter filterAdapter;
    private final Set<String> selectedTags = new ObjectRBTreeSet<>();
    private Map<String, Tag> tagFromTitle;
    private boolean enabled = true;
    private boolean loaded = false;
    private boolean mainSearch;
    private boolean enableCreate;

    public TagSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, R.id.tags_add, R.id.tags_clear, R.string.select_tags, true);
    }

    public TagSelector(A activity, DatabaseAdapter db, ActivityLayout x, int actBtnId, int clearBtnId, int defaultValueResId, boolean enableCreate) {
        this.activity = activity;
        this.db = db;
        this.x = x;
        this.isShow = MyPreferences.isShowTags();
        this.mainSearch = (MyPreferences.getTagsSelectorType() == MyPreferences.EntitySelectorType.SEARCH);
        this.actBtnId = actBtnId;
        this.clrBtnId = clearBtnId;
        this.defaultValueResId = defaultValueResId;
        this.enableCreate = enableCreate;
    }

    public TextView createNode(LinearLayout layout) {
        if (!isShow) {
            return null;
        }

        Pair<TextView, AutoCompleteTextView> views;

        if (!mainSearch) {
            views = x.addListNodeWithButtonsAndFilter(
                    layout, R.layout.select_entry_with_2btn_and_filter, R.id.tags, actBtnId,
                    clrBtnId, R.string.tags, defaultValueResId, R.id.tags_filter_toggle);
        } else {
            views = x.addListNodeWithButtonsAndFilterSearchFirst(
                    layout, R.layout.select_entry_with_2btn_and_list_filter, R.id.tags, actBtnId,
                    clrBtnId, R.string.tags, defaultValueResId, R.id.tags_filter_toggle,
                    R.id.tags_show_list, R.id.tags_create, enableCreate);
        }

        text = views.first;
        autoCompleteFilter = views.second;
        node = (View) text.getTag();
        node.setEnabled(false);

//        float density = activity.getResources().getDisplayMetrics().density;
//        int minHeight = (int) (56 * density);
//        node.setMinimumHeight(minHeight);
//        View row = node.findViewById(R.id.list_node_row);
//        if (row != null) {
//            row.setMinimumHeight(minHeight);
//        }
//        View parent = (View) text.getParent();
//        if (parent != null) {
//            parent.setPadding(parent.getPaddingLeft(), (int) (4 * density), parent.getPaddingRight(), (int) (4 * density));
//        }
//        text.setPadding(0, (int) (2 * density), 0, (int) (2 * density));
//        text.setLineSpacing(3 * density, 1.0f);

        initAutoCompleteFilter(autoCompleteFilter);
        fetchEntities();

        return text;
    }

    private void initAutoCompleteFilter(final AutoCompleteTextView filterTxt) {
        filterAdapter = createFilterAdapter();
        filterTxt.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
        filterTxt.setThreshold(1);

        filterTxt.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                filterTxt.setAdapter(filterAdapter);
                filterTxt.selectAll();
            }
        });

        filterTxt.setOnItemClickListener((parent, view, position, id) -> {
            var c = (Cursor) parent.getItemAtPosition(position);
            @SuppressLint("Range") String tag = c.getString(c.getColumnIndex("e_title"));
            if (!TextUtils.isEmpty(tag)) {
                selectedTags.add(tag.trim());
                fillCheckedEntitiesInUI();
            }
            filterTxt.setText("");
            View toggleBtn = (View) filterTxt.getTag();
            if (toggleBtn != null) {
                toggleBtn.performClick();
            }

            List<Tag> selectedTagEntities = new ArrayList<>();
            for (String title : selectedTags) {
                selectedTagEntities.add(new Tag(title, true));
            }
            x.listener.onSelected(R.id.tags, selectedTagEntities);
        });
    }

    public void fetchEntities() {
        synchronized (this) {
            loaded = false;
        }
        Application.getExecutor().execute(() -> {
            var allTags = db.getAllTagByTitleMap();

            synchronized (TagSelector.this) {
                tagFromTitle = allTags;
                loaded = true;
            }

            activity.runOnUiThread(() -> {
                if (node != null && enabled) {
                    node.setEnabled(true);
                }
                fillCheckedEntitiesInUI();
            });
        });
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (node != null) {
            node.setEnabled(enabled);
        }
    }

    public void onClick(int id) {
        if (!enabled) return;

        if (id == R.id.tags) {
            if (mainSearch) {
                if (filterAdapter == null) initAutoCompleteFilter(autoCompleteFilter);
            } else {
                pickTags();
            }
        } else if (id == R.id.tags_add) {
            showAddTagDialog();
        } else if (id == R.id.tags_show_list) {
            pickTags();
        } else if (id == R.id.tags_filter_toggle) {
            if (filterAdapter == null) initAutoCompleteFilter(autoCompleteFilter);
        } else if (id == R.id.tags_create) {
            new AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.confirm_create_entity, autoCompleteFilter.getText()))
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> manualCreateNewEntityFromSearch())
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else if (id == R.id.tags_clear) {
            clearSelection();
        }
    }

    private void manualCreateNewEntityFromSearch() {
        String title = autoCompleteFilter.getText().toString();
        Tag e = db.findOrInsertEntityByTitle(Tag.class, title);

        View hideSearch = (View) autoCompleteFilter.getTag();
        hideSearch.performClick();

        fetchEntities();
        selectedTags.add(title);
        fillCheckedEntitiesInUI();
    }

    public void clearSelection() {
        selectedTags.clear();
        fillCheckedEntitiesInUI();
    }

    public void pickTags() {
        List<Tag> currentTags;
        synchronized (this) {
            currentTags = new ArrayList<>(tagFromTitle.values());
        }

        final CharSequence[] titles = new CharSequence[currentTags.size()];
        final boolean[] checked = new boolean[currentTags.size()];

        for (int i = 0; i < currentTags.size(); i++) {
            Tag t = currentTags.get(i);
            titles[i] = new SpannableStringBuilder().append(t.title, new PillSpan(this.activity, t.getColorInt()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            t.checked = checked[i] = selectedTags.contains(titles[i].toString());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.tags);

        if (currentTags.isEmpty()) {
            builder.setMessage(R.string.no_tags);
            builder.setPositiveButton(R.string.new_tag, (dialog, which) -> showAddTagDialog());
            builder.setNegativeButton(R.string.cancel, null);
        } else {
            builder.setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> {
                checked[which] = isChecked;
                currentTags.get(which).setChecked(isChecked);
            });
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                selectedTags.clear();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        selectedTags.add(titles[i].toString());
                    }
                }
                fillCheckedEntitiesInUI();
                x.listener.onSelected(R.id.tags, currentTags);
            });
            builder.setNeutralButton(R.string.new_tag, (dialog, which) -> {
                selectedTags.clear();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        selectedTags.add(titles[i].toString());
                    }
                }
                fillCheckedEntitiesInUI();
                showAddTagDialog();
            });
            builder.setNegativeButton(R.string.cancel, null);
        }
        builder.show();
    }

    public void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.new_tag);

        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(R.string.enter_tag_name);

        FrameLayout container = new FrameLayout(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        int margin = (int) (16 * activity.getResources().getDisplayMetrics().density);
//        params.leftMargin = margin;
//        params.rightMargin = margin;
//        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String tagName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(tagName)) {
                // Save to DB
                Tag t = db.findOrInsertEntityByTitle(Tag.class, tagName);
                selectedTags.add(t.title);
                fetchEntities();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public void fillCheckedEntitiesInUI() {
        if (text == null) return;
        if (!loaded) return;

        Log.d(TAG, "selectedTags=" + selectedTags);

        if (selectedTags.isEmpty()) {
            text.setText(R.string.select_tags);
            showHideMinusBtn(false);
        } else {
            text.setText(PillSpan.formatAsPills(activity, selectedTags, tagFromTitle));
            showHideMinusBtn(true);
        }
    }

    private void showHideMinusBtn(boolean show) {
        if (text != null) {
            ImageView minusBtn = (ImageView) text.getTag(R.id.bMinus);
            if (minusBtn != null) {
                minusBtn.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }

    public String getSelectedTags() {
        if (selectedTags.isEmpty()) {
            return null;
        }
        return "\n" + String.join("\n", selectedTags) + "\n";
    }

    public void setSelectedTags(String tagsString) {
        selectedTags.clear();
        if (!Utils.isEmpty(tagsString)) {
            for (String t : tagsString.split("\n")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    selectedTags.add(trimmed);
                }
            }
        }
        fillCheckedEntitiesInUI();
    }

    public void setSelectedTags(Set<String> tags) {
        selectedTags.clear();
        selectedTags.addAll(tags);
        fillCheckedEntitiesInUI();
    }

    protected SimpleCursorAdapter createFilterAdapter() {
        return new TransactionUtils.FilterSimpleCursorAdapter<>(activity, db, Tag.class);
    }

    public void onDestroy() {
        // cleanup if needed
    }
}
