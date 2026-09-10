package tw.tib.financisto.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import tw.tib.financisto.Application;
import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.view.PillSpan;

public class TagSelector<A extends AbstractActivity> {

    private final A activity;
    private final DatabaseAdapter db;
    private final ActivityLayout x;
    private final boolean isShow;

    private View node;
    private TextView text;
    private AutoCompleteTextView autoCompleteFilter;
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private List<Tag> entities = new ArrayList<>();
    private boolean enabled = true;
    private boolean loaded = false;

    public TagSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this.activity = activity;
        this.db = db;
        this.x = x;
        this.isShow = MyPreferences.isShowTags();
    }

    public TextView createNode(LinearLayout layout) {
        if (!isShow) {
            return null;
        }

        Pair<TextView, AutoCompleteTextView> views = x.addListNodeWithButtonsAndFilter(
                layout,
                R.layout.select_entry_with_2btn_and_filter,
                R.id.tags,
                R.id.tags_add,
                R.id.tags_clear,
                R.string.tags,
                R.string.select_tags,
                R.id.tags_filter_toggle
        );

        text = views.first;
        autoCompleteFilter = views.second;
        node = (View) text.getTag();
        node.setEnabled(false);

        float density = activity.getResources().getDisplayMetrics().density;
        int minHeight = (int) (56 * density);
        node.setMinimumHeight(minHeight);
        View row = node.findViewById(R.id.list_node_row);
        if (row != null) {
            row.setMinimumHeight(minHeight);
        }
        View parent = (View) text.getParent();
        if (parent != null) {
            parent.setPadding(parent.getPaddingLeft(), (int) (4 * density), parent.getPaddingRight(), (int) (4 * density));
        }
        text.setPadding(0, (int) (2 * density), 0, (int) (2 * density));
        text.setLineSpacing(3 * density, 1.0f);

        initAutoCompleteFilter(autoCompleteFilter);
        fetchEntities();

        return text;
    }

    private void initAutoCompleteFilter(final AutoCompleteTextView filterTxt) {
        if (filterTxt == null) return;
        filterTxt.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
        filterTxt.setThreshold(1);

        filterTxt.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                List<String> tags = db.getAllUniqueTags();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                        android.R.layout.simple_dropdown_item_1line, tags);
                filterTxt.setAdapter(adapter);
                filterTxt.selectAll();
            }
        });

        filterTxt.setOnItemClickListener((parent, view, position, id) -> {
            String tag = (String) parent.getItemAtPosition(position);
            if (!TextUtils.isEmpty(tag)) {
                selectedTags.add(tag.trim());
                fillCheckedEntitiesInUI();
            }
            filterTxt.setText("");
            View toggleBtn = (View) filterTxt.getTag();
            if (toggleBtn != null) {
                toggleBtn.performClick();
            }
        });
    }

    public void fetchEntities() {
        synchronized (this) {
            loaded = false;
        }
        Application.getExecutor().execute(() -> {
            List<Tag> list = db.getAllEntitiesList(Tag.class, false, true);
            // Also ensure any tags from transactions exist in list
            List<String> uniqueTagNames = db.getAllUniqueTags();
            Set<String> known = new LinkedHashSet<>();
            for (Tag t : list) {
                known.add(t.title.toLowerCase());
            }
            for (String name : uniqueTagNames) {
                if (!known.contains(name.toLowerCase())) {
                    Tag t = new Tag(name);
                    list.add(t);
                    known.add(name.toLowerCase());
                }
            }

            synchronized (TagSelector.this) {
                entities = list;
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
            pickTags();
        } else if (id == R.id.tags_add) {
            showAddTagDialog();
        } else if (id == R.id.tags_clear) {
            clearSelection();
        }
    }

    public void clearSelection() {
        selectedTags.clear();
        fillCheckedEntitiesInUI();
    }

    public void pickTags() {
        List<Tag> currentTags;
        synchronized (this) {
            currentTags = new ArrayList<>(entities);
        }

        final String[] titles = new String[currentTags.size()];
        final boolean[] checked = new boolean[currentTags.size()];

        for (int i = 0; i < currentTags.size(); i++) {
            titles[i] = currentTags.get(i).title;
            checked[i] = containsIgnoreCase(selectedTags, titles[i]);
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
            });
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                selectedTags.clear();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        selectedTags.add(titles[i]);
                    }
                }
                fillCheckedEntitiesInUI();
            });
            builder.setNeutralButton(R.string.new_tag, (dialog, which) -> {
                selectedTags.clear();
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        selectedTags.add(titles[i]);
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
        int margin = (int) (16 * activity.getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
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

        if (selectedTags.isEmpty()) {
            text.setText(R.string.select_tags);
            showHideMinusBtn(false);
        } else {
            text.setText(PillSpan.formatAsPills(activity, selectedTags));
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
        return String.join("\n", selectedTags);
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

    private static boolean containsIgnoreCase(Set<String> set, String target) {
        for (String s : set) {
            if (s.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public void onDestroy() {
        // cleanup if needed
    }
}
