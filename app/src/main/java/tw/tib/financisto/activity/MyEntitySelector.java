/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import androidx.core.util.Pair;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;
import tw.tib.financisto.R;
import tw.tib.financisto.adapter.MyEntityAdapter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.model.MultiChoiceItem;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.utils.ArrUtils;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.utils.Utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public abstract class MyEntitySelector<T extends MyEntity, A extends AbstractActivity> {
    private static final String TAG = "MyEntitySelector";

    protected final Class<T> entityClass;
    protected final A activity;
    protected final MyEntityManager em;
    private final ActivityLayout x;
    private final boolean isShow;
    private final int layoutId, actBtnId, clearBtnId, labelResId, defaultValueResId, filterToggleId, showListId, createEntityId;

    private View node;
    private TextView text;
    private AutoCompleteTextView autoCompleteFilter;
    private SimpleCursorAdapter filterAdapter;
    private List<T> entities = Collections.emptyList();
    private ListAdapter adapter;
    private boolean includeZero;
    private boolean multiSelect;
    private boolean useSearchAsPrimary;

    private long selectedEntityId = 0;

    long[] includeEntityIds;
    boolean fetchAllEntities = false;
    boolean enableCreate = true;

    public MyEntitySelector(Class<T> entityClass,
                            A activity,
                            MyEntityManager em,
                            ActivityLayout x,
                            boolean isShow,
                            int layoutId,
                            int actBtnId,
                            int clearBtnId,
                            int labelResId,
                            int defaultValueResId,
                            int filterToggleId,
                            int showListId,
                            int createEntityId) {
        this.entityClass = entityClass;
        this.activity = activity;
        this.em = em;
        this.x = x;
        this.isShow = isShow;
        this.layoutId = layoutId;
        this.actBtnId = actBtnId;
        this.clearBtnId = clearBtnId;
        this.labelResId = labelResId;
        this.defaultValueResId = defaultValueResId;
        this.filterToggleId = filterToggleId;
        this.showListId = showListId;
        this.createEntityId = createEntityId;
    }

    protected abstract Class getEditActivityClass();

    public List<T> getEntities() {
        return entities;
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
    }

    public void setIncludeEntityIds(long... includeEntityIds) {
        this.includeEntityIds = includeEntityIds;
    }

    public void setFetchAllEntities(boolean fetchAllEntities) {
        this.fetchAllEntities = fetchAllEntities;
    }

    public void setEnableCreate(boolean enableCreate) {
        this.enableCreate = enableCreate;
    }

    public void setIncludeZero(boolean includeZero) {
        this.includeZero = includeZero;
    }

    public void fetchEntities() {
        entities = fetchEntities(em);
        if (!multiSelect) {
            adapter = createAdapter(activity, entities);
        }
    }

    protected List<T> fetchEntities(MyEntityManager em) {
        if (fetchAllEntities) {
            return em.getAllEntitiesList(entityClass, this.includeZero || !isMultiSelect(), false);
        }
        else {
            return em.getAllEntitiesList(entityClass, this.includeZero || !isMultiSelect(), true, includeEntityIds);
        }
    }

    protected ListAdapter createAdapter(Activity activity, List<T> entities) {
        return new MyEntityAdapter<>(activity, android.R.layout.simple_list_item_activated_1, android.R.id.text1, entities);
    }

    protected SimpleCursorAdapter createFilterAdapter() {
        return new TransactionUtils.FilterSimpleCursorAdapter<>(activity, em, entityClass, includeEntityIds);
    }

    public TextView createNode(LinearLayout layout) {
        if (useSearchAsPrimary) {
            return createNode(layout, R.layout.select_entry_with_2btn_and_list_filter);
        } else {
            return createNode(layout, R.layout.select_entry_with_2btn_and_filter);
        }
    }

    public TextView createNode(LinearLayout layout, int nodeLayoutId) {
        if (isShow) {
            final Pair<TextView, AutoCompleteTextView> views;
            if (useSearchAsPrimary) {
                views = x.addListNodeWithButtonsAndFilterSearchFirst(layout, nodeLayoutId, layoutId,
                        actBtnId, clearBtnId, labelResId, defaultValueResId,
                        filterToggleId, showListId, createEntityId, enableCreate);
            } else {
                views = x.addListNodeWithButtonsAndFilter(layout, nodeLayoutId, layoutId, actBtnId,
                        clearBtnId, labelResId, defaultValueResId, filterToggleId);
            }
            text = views.first;
            autoCompleteFilter = views.second;
            node = (View) text.getTag();
        }
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
            activity.onSelectedId(layoutId, id);
            View toggleBtn = (View) filterTxt.getTag();
            toggleBtn.performClick();
        });
    }

    public void onClick(int id) {
        if (id == layoutId) {
            Log.d(TAG, "onClick Layout");
            if (useSearchAsPrimary) {
                if (filterAdapter == null) initAutoCompleteFilter(autoCompleteFilter);
            } else {
                pickEntity();
            }
        } else if (id == actBtnId) {
            Intent intent = new Intent(activity, getEditActivityClass());
            activity.startActivityForResult(intent, actBtnId);
        } else if (id == filterToggleId) {
            if (filterAdapter == null) initAutoCompleteFilter(autoCompleteFilter);
        } else if (id == showListId) {
            pickEntity();
        } else if (id == createEntityId) {
            new AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.confirm_create_entity, autoCompleteFilter.getText()))
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> manualCreateNewEntityFromSearch())
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else if (id == clearBtnId) {
            clearSelection();
        }
    }

    private void clearSelection() {
        if (text != null) {
            text.setText(defaultValueResId);
        }
        selectedEntityId = 0;
        for (MyEntity e : getEntities()) e.setChecked(false);
        showHideMinusBtn(false);
    }

    private void showHideMinusBtn(boolean show) {
        if (text != null) {
            ImageView minusBtn = (ImageView) text.getTag(R.id.bMinus);
            if (minusBtn != null) minusBtn.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void pickEntity() {
        if (multiSelect) {
            x.selectMultiChoice(activity, layoutId, labelResId, entities);
        } else {
            int selectedEntityPos = MyEntity.indexOf(entities, selectedEntityId);
            x.selectPosition(activity, layoutId, labelResId, adapter, selectedEntityPos);
        }
    }

    public void onSelectedPos(int id, int selectedPos) {
        if (id == layoutId) onEntitySelected(selectedPos);
    }

    public void onSelectedId(int id, long selectedId) {
        if (id != layoutId) return;

        selectEntity(selectedId);
    }

    public void onSelected(int id, List<? extends MultiChoiceItem> ignore) {
        if (id == layoutId) fillCheckedEntitiesInUI();
    }

    public void fillCheckedEntitiesInUI() {
        String selectedProjects = getCheckedTitles();
        if (Utils.isEmpty(selectedProjects)) {
            clearSelection();
        } else {
            text.setText(selectedProjects);
            showHideMinusBtn(true);
        }
    }


    public String getCheckedTitles() {
        return getCheckedTitles(entities);
    }

    public static String getCheckedTitles(List<? extends MyEntity> list) {
        StringBuilder sb = new StringBuilder();
        for (MyEntity e : list) {
            if (e.checked) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(e.title);
            }
        }
        return sb.toString();
    }

    public String[] getCheckedIds() {
        return getCheckedIds(entities);
    }

    public static String[] getCheckedIds(List<? extends MyEntity> list) {
        List<String> res = new LinkedList<>();
        for (MyEntity e : list) {
            if (e.checked) {
                res.add(String.valueOf(e.id));
            }
        }
        return ArrUtils.strListToArr(res);
    }

    public String getCheckedIdsAsStr() {
        return getCheckedIdsAsStr(this.entities);
    }

    public static String getCheckedIdsAsStr(List<? extends MyEntity> list) {
        StringBuilder sb = new StringBuilder();
        for (MyEntity e : list) {
            if (e.checked) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(e.id);
            }
        }
        return sb.toString();
    }

    private void onEntitySelected(int selectedPos) {
        T e = entities.get(selectedPos);
        selectEntity(e);
    }

    public void selectEntity(long entityId) {
        if (isShow) {
            if (multiSelect) {
                updateCheckedEntities("" + entityId);
                fillCheckedEntitiesInUI();
            } else {
                T e = MyEntity.find(entities, entityId);
                selectEntity(e);
            }
        }
    }

    private void selectEntity(T e) {
        if (isShow) {
            if (e == null) {
                clearSelection();
            } else {
                text.setText(e.title);
                selectedEntityId = e.id;
                showHideMinusBtn(e.id > 0);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == actBtnId) {
            onNewEntity(data);
        }
    }

    private void manualCreateNewEntityFromSearch() {
        String title = autoCompleteFilter.getText().toString();
        T e = em.findOrInsertEntityByTitle(entityClass, title);

        View hideSearch = (View) autoCompleteFilter.getTag();
        hideSearch.performClick();

        fetchEntities();
        selectEntity(e);
    }

    protected void autoCreateNewEntityFromSearch() {
        if (autoCompleteFilter != null
                && autoCompleteFilter.getVisibility() == View.VISIBLE
                && selectedEntityId == 0)
        {
            String title = autoCompleteFilter.getText().toString();
            T e = em.findOrInsertEntityByTitle(entityClass, title);
            selectEntity(e);
        }
    }

    private void onNewEntity(Intent data) {
        fetchEntities();
        long entityId = data.getLongExtra(DatabaseHelper.EntityColumns.ID, -1);
        if (entityId != -1) {
            selectEntity(entityId);
        }
    }

    public void setNodeVisible(boolean visible) {
        if (isShow) {
            AbstractActivity.setVisibility(node, visible ? View.VISIBLE : View.GONE);
        }
    }

    public long getSelectedEntityId() {
        return node == null || node.getVisibility() == View.GONE ? 0 : selectedEntityId;
    }

    public boolean isShow() {
        return isShow;
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void initMultiSelect() {
        this.multiSelect = true;
        fetchEntities();
    }

    public void setUseSearchAsPrimary(boolean val) {
        this.useSearchAsPrimary = val;
    }

    public boolean isUseSearchAsPrimary() {
        return useSearchAsPrimary;
    }

    public static <T extends MyEntity> List<T> merge(List<T> oldList, List<T> newList) {
        for (T newT : newList) {
            for (Iterator<T> i = oldList.iterator(); i.hasNext(); ) {
                T oldT = i.next();
                if (newT.id == oldT.id) {
                    newT.checked = oldT.checked;
                    i.remove();
                    break;
                }
            }
        }
        return newList;
    }

    public void updateCheckedEntities(String checkedCommaIds) {
        updateCheckedEntities(this.entities, checkedCommaIds);
    }

    public static void updateCheckedEntities(List<? extends MyEntity> list, String checkedCommaIds) {
        if (!Utils.isEmpty(checkedCommaIds)) {
            updateCheckedEntities(list, checkedCommaIds.split(","));
        }

    }

    public void updateCheckedEntities(String[] checkedIds) {
        updateCheckedEntities(this.entities, checkedIds);
    }


    public static void updateCheckedEntities(List<? extends MyEntity> list, String[] checkedIds) {
        for (String s : checkedIds) {
            long id = Long.parseLong(s);
            for (MyEntity e : list) {
                if (e.id == id) {
                    e.checked = true;
                    break;
                }
            }
        }
    }

    public void onDestroy() {
        if (filterAdapter != null) {
            filterAdapter.changeCursor(null);
            filterAdapter = null;
        }
    }
}
