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
package tw.tib.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import tw.tib.financisto.R;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.MultiChoiceItem;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.model.MyLocation;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.utils.ArrUtils;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.view.PillSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static tw.tib.financisto.activity.CategorySelector.SelectorType.FILTER;
import static tw.tib.financisto.filter.WhereFilter.Operation.BTW;
import static tw.tib.financisto.filter.WhereFilter.Operation.IN;

public abstract class FilterAbstractActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

	protected WhereFilter filter = WhereFilter.empty();

	protected ProjectSelector<FilterAbstractActivity> projectSelector;
	protected PayeeSelector<FilterAbstractActivity> payeeSelector;
	protected CategorySelector<FilterAbstractActivity> categorySelector;
	protected LocationSelector<FilterAbstractActivity> locationSelector;
	protected TagSelector<FilterAbstractActivity> tagSelector;

	protected TextView project;
	protected TextView payee;
	protected TextView location;
	protected TextView tagsOp;
	protected TextView tags;
	protected TextView categoryTxt;

	protected String noFilterValue;

	protected String[] tagsOpEntries;
	protected Map<String, Tag> tagFromTitle;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tagsOpEntries = getResources().getStringArray(R.array.tags_op_entries);
		tagFromTitle = db.getAllTagByTitleMap();
	}

	protected void initPayeeSelector(LinearLayout layout) {
		payeeSelector = new PayeeSelector<>(this, db, x, 0, R.id.payee_clear, R.string.no_filter);
		payeeSelector.setFetchAllEntities(true);
		payeeSelector.setEnableCreate(false);
		payeeSelector.initMultiSelect();
		payee = payeeSelector.createNode(layout);
	}

	protected void initProjectSelector(LinearLayout layout) {
		projectSelector = new ProjectSelector<>(this, db, x, 0, R.id.project_clear, R.string.no_filter);
		projectSelector.setFetchAllEntities(true);
		projectSelector.setEnableCreate(false);
		projectSelector.initMultiSelect();
		project = projectSelector.createNode(layout);
	}

	protected void initLocationSelector(LinearLayout layout) {
		locationSelector = new LocationSelector<>(this, db, x, 0, R.id.location_clear, R.string.current_location);
		locationSelector.setFetchAllEntities(true);
		locationSelector.setEnableCreate(false);
		locationSelector.initMultiSelect();
		location = locationSelector.createNode(layout);
	}

	protected void initTagSelector(LinearLayout layout) {
		tagSelector = new TagSelector<>(this, db, x, 0, R.id.tags_clear, R.string.no_filter, false);

		tagsOp = x.addFilterNodeMinus(layout, R.id.tags_op, R.id.tags_op_clear, R.string.tags_op, R.string.tags_op_default);
		tags = tagSelector.createNode(layout);
	}

	protected void initCategorySelector(LinearLayout layout) {
		categorySelector = new CategorySelector<>(this, db, x);
		categorySelector.setListener(this);
		categorySelector.initMultiSelect();
		categoryTxt = categorySelector.createNode(layout, FILTER);
	}

	protected void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
		hideMinusButton(textView);
	}

	protected void clearCategory() {
		clear(BlotterFilter.CATEGORY_LEFT, categoryTxt);
	}

	@Override
	protected void onClick(View v, int id) {
		if (id == R.id.category_filter_toggle ||
			id == R.id.category)
		{
			categorySelector.onClick(id);
		}
		else if (id == R.id.category_clear) {
			categorySelector.onClick(id);
			clearCategory();
		}
		else if (id == R.id.project) {
			Criterion c = filter.get(BlotterFilter.PROJECT_ID);
			if (c != null) projectSelector.updateCheckedEntities(c.getValues());
			projectSelector.onClick(id);
		}
		else if (id == R.id.project_clear) {
			clear(BlotterFilter.PROJECT_ID, project);
			projectSelector.onClick(id);
		}
		else if (id == R.id.project_filter_toggle ||
				id == R.id.project_show_list)
		{
			projectSelector.onClick(id);
		}
		else if (id == R.id.payee) {
			Criterion c = filter.get(BlotterFilter.PAYEE_ID);
			if (c != null) payeeSelector.updateCheckedEntities(c.getValues());
			payeeSelector.onClick(id);
		}
		else if (id == R.id.payee_clear) {
			clear(BlotterFilter.PAYEE_ID, payee);
			payeeSelector.onClick(id);
		}
		else if (id == R.id.payee_filter_toggle ||
				id == R.id.payee_show_list)
		{
			payeeSelector.onClick(id);
		}
		else if (id == R.id.location) {
			Criterion c = filter.get(BlotterFilter.LOCATION_ID);
			if (c != null) locationSelector.updateCheckedEntities(c.getValues());
			locationSelector.onClick(id);
		}
		else if (id == R.id.location_clear) {
			clear(BlotterFilter.LOCATION_ID, location);
			locationSelector.onClick(id);
		}
		else if (id == R.id.location_filter_toggle ||
				id == R.id.location_show_list)
		{
			locationSelector.onClick(id);
		}
		else if (id == R.id.tags_op) {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tagsOpEntries);
			Criterion c = filter.get(BlotterFilter.TAGS_OP);
			int selectedPos = c != null ? c.getIntValue() : 0;
			x.selectPosition(this, R.id.tags_op, R.string.tags_op, adapter, selectedPos);
		} else if (id == R.id.tags_op_clear) {
			onSelectedPos(R.id.tags_op, 0);
		} else if (id == R.id.tags || id == R.id.tags_show_list || id == R.id.tags_filter_toggle) {
			tagSelector.onClick(id);
		} else if (id == R.id.tags_clear) {
			clear(BlotterFilter.TAGS, tags);
			tagSelector.onClick(id);
		}

	}

	@Override
	public void onSelectedId(final int id, final long selectedId) {
		switch (id) {
			case R.id.project:
				projectSelector.onSelectedId(id, selectedId);
				filter.put(Criterion.in(BlotterFilter.PROJECT_ID, projectSelector.getCheckedIds()));
				updateProjectFromFilter();
				break;
			case R.id.payee:
				payeeSelector.onSelectedId(id, selectedId);
				if (selectedId == 0) {
					filter.put(Criterion.isNull(BlotterFilter.PAYEE_ID));
				} else {
					filter.put(Criterion.in(BlotterFilter.PAYEE_ID, payeeSelector.getCheckedIds()));
				}
				updatePayeeFromFilter();
				break;
			case R.id.location:
				locationSelector.onSelectedId(id, selectedId);
				filter.put(Criterion.in(BlotterFilter.LOCATION_ID, locationSelector.getCheckedIds()));
				updateLocationFromFilter();
				break;
			case R.id.category:
				categorySelector.onSelectedId(id, selectedId, false);
//                filter.put(Criterion.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
//                updateCategoryFromFilter();
				break;
		}
	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		if (id == R.id.category) {
			if (ArrUtils.isEmpty(categorySelector.getCheckedCategoryLeafs())) {
				clearCategory();
			} else {
				filter.put(Criterion.btw(BlotterFilter.CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
				updateCategoryFromFilter();
			}
		}
		else if (id == R.id.project) {
			if (ArrUtils.isEmpty(projectSelector.getCheckedIds())) {
				clear(BlotterFilter.PROJECT_ID, project);
			} else {
				filter.put(Criterion.in(BlotterFilter.PROJECT_ID, projectSelector.getCheckedIds()));
				updateProjectFromFilter();
			}
		}
		else if (id == R.id.payee) {
			if (ArrUtils.isEmpty(payeeSelector.getCheckedIds())) {
				clear(BlotterFilter.PAYEE_ID, payee);
			} else {
				filter.put(Criterion.in(BlotterFilter.PAYEE_ID, payeeSelector.getCheckedIds()));
				updatePayeeFromFilter();
			}
		}
		else if (id == R.id.location) {
			if (ArrUtils.isEmpty(locationSelector.getCheckedIds())) {
				clear(BlotterFilter.LOCATION_ID, location);
			} else {
				filter.put(Criterion.in(BlotterFilter.LOCATION_ID, locationSelector.getCheckedIds()));
				updateLocationFromFilter();
			}
		}
		else if (id == R.id.tags) {
			var selectedTags = new ArrayList<String>();
			for (var item : items) {
				if (item.isChecked()) {
					selectedTags.add(((Tag) item).title);
				}
			}
			if (!selectedTags.isEmpty()) {
				Criterion[] children = new Criterion[selectedTags.size()];
				for (int i = 0; i < selectedTags.size(); i++) {
					String tag = selectedTags.get(i);
					if (tag.equals(getString(R.string.no_tags))) {
						children[i] = new Criterion(BlotterFilter.TAGS, WhereFilter.Operation.ISNULL);
					} else {
						children[i] = new Criterion(BlotterFilter.TAGS, WhereFilter.Operation.LIKE, "%\n" + tag + "\n%");
					}
				}
				Criterion c = filter.get(BlotterFilter.TAGS_OP);
				if (c != null && c.getIntValue() == 1) {
					filter.put(Criterion.or(children));
				} else {
					filter.put(Criterion.and(children));
				}
			} else {
				clear(BlotterFilter.TAGS, tags);
			}
			updateTagsFromFilter();
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) { // todo.mb: not used in case of multi-select, so remove then
		if (id == R.id.project) {
			projectSelector.onSelectedPos(id, selectedPos);
			filter.put(Criterion.eq(BlotterFilter.PROJECT_ID, String.valueOf(projectSelector.getSelectedEntityId())));
			updateProjectFromFilter();
		}
		else if (id == R.id.payee) {
			payeeSelector.onSelectedPos(id, selectedPos);
			filter.put(Criterion.eq(BlotterFilter.PAYEE_ID, String.valueOf(payeeSelector.getSelectedEntityId())));
			updatePayeeFromFilter();
		}
		else if (id == R.id.location) {
			locationSelector.onSelectedPos(id, selectedPos);
			filter.put(Criterion.in(BlotterFilter.LOCATION_ID, String.valueOf(locationSelector.getSelectedEntityId())));
			updateLocationFromFilter();
		}
		else if (id == R.id.tags_op) {
			if (selectedPos == 0) {
				filter.remove(BlotterFilter.TAGS_OP);
				Criterion c = filter.get(BlotterFilter.TAGS);
				if (c != null) {
					filter.put(Criterion.and(c.getChildren()));
				}
			} else {
				filter.put(Criterion.tag(BlotterFilter.TAGS_OP, String.valueOf(selectedPos)));
				Criterion c = filter.get(BlotterFilter.TAGS);
				if (c != null) {
					filter.put(Criterion.or(c.getChildren()));
				}
			}
			updateTagsOpFromFilter();
		}
	}

	protected void updateCategoryFromFilter() {
		Criterion c = filter.get(BlotterFilter.CATEGORY_LEFT);
		if (c != null) {
			if (c.operation != BTW) { // todo.mb: only for backward compatibility, just remove in next releases
				Log.i(getClass().getSimpleName(), "Found category filter with deprecated op: " + c.operation);
				filter.remove(BlotterFilter.CATEGORY_LEFT);
				return;
			}

			List<String> checkedLeftIds = getLeftCategoryNodesFromFilter(c);
			List<Long> catIds = db.getCategoryIdsByLeftIds(checkedLeftIds);

			categorySelector.updateCheckedEntities(catIds);
			categorySelector.fillCategoryInUI();
		}
	}

	private List<String> getLeftCategoryNodesFromFilter(Criterion catCriterion) {
		List<String> res = new LinkedList<>();
		for (int i = 0; i < catCriterion.getValues().length; i += 2) {
			res.add(catCriterion.getValues()[i]);
		}
		return res;
	}

	protected <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
		if (filterView == null) return;
		Criterion c = filter.get(filterCriteriaName);
		if (c != null && !c.isNull()) {
			String filterText = noFilterValue;
			if (c.operation == IN) {
				getSelectedTitles(c, filterCriteriaName, (selectedTitles) -> {
					if (!TextUtils.isEmpty(selectedTitles)) {
						filterView.setText(selectedTitles);
						showMinusButton(filterView);
					}
				});
			} else {
				long entityId = c.getLongValue1();
				T e = db.get(entityClass, entityId);
				if (e != null) filterText = e.title;
			}
			if (!TextUtils.isEmpty(filterText)) {
				filterView.setText(filterText);
				showMinusButton(filterView);
			}
		} else {
			filterView.setText(R.string.no_filter);
			hideMinusButton(filterView);
		}
	}

	protected void updateTagsOpFromFilter() {
		Criterion c = filter.get(BlotterFilter.TAGS_OP);
		if (c != null) {
			int selected = c.getIntValue();
			tagsOp.setText(tagsOpEntries[selected]);
			showMinusButton(tagsOp);
		} else {
			tagsOp.setText(tagsOpEntries[0]);
			hideMinusButton(tagsOp);
		}
	}

	protected void updateTagsFromFilter() {
		tagSelector.setSelectedTags(getSelectedTagsFromFilter());
	}

	private Set<String> getSelectedTagsFromFilter() {
		var list = new ObjectOpenHashSet<String>();
		Criterion c = filter.get(BlotterFilter.TAGS);
		if (c != null) {
			extractTagsFromCriterion(c, list);
		}
		return list;
	}

	private void extractTagsFromCriterion(Criterion c, Collection<String> list) {
		if (c.getChildren() != null && c.getChildren().length > 0) {
			for (Criterion child : c.getChildren()) {
				extractTagsFromCriterion(child, list);
			}
		}
		else if (c.isNull()) {
			list.add(getString(R.string.no_tags));
		}
		else if (c.getValues() != null) {
			for (String v : c.getValues()) {
				if (v != null) {
					if (v.startsWith("%\n") && v.endsWith("\n%") && v.length() >= 4) {
						v = v.substring(2, v.length() - 2);
					}
					v = v.trim();
					if (!v.isEmpty() && !list.contains(v)) {
						list.add(v);
					}
				}
			}
		}
	}

	private void showTagsFilterDialog() {
		var allTagsSet = new TreeSet<String>();

		allTagsSet.add(getString(R.string.no_tags));

		allTagsSet.addAll(db.getAllUniqueTags());
		allTagsSet.addAll(getSelectedTagsFromFilter());
		if (allTagsSet.isEmpty()) {
			Toast.makeText(this, R.string.no_tags, Toast.LENGTH_SHORT).show();
			return;
		}

		var items = new ArrayList<BlotterFilterActivity.TagMultiChoiceItem>();
		var selected = new TreeSet<String>();
		selected.addAll(getSelectedTagsFromFilter());

		for (String tag : allTagsSet) {
			var item = new BlotterFilterActivity.TagMultiChoiceItem(tag);
			if (selected.contains(tag)) {
				item.setChecked(true);
			}
			items.add(item);
		}
		x.selectMultiChoice(this, R.id.tags, R.string.tags, items);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case R.id.category_pick:
			case R.id.category_add:
				categorySelector.onActivityResult(requestCode, resultCode, data);
				break;
		}
	}

	@Override
	public void onCategorySelected(Category cat, boolean selectLast) {
		clearCategory();
		filter.remove(BlotterFilter.CATEGORY_ID);
		if (categorySelector.isMultiSelect()) {
			String categories[] = categorySelector.getCheckedCategoryLeafs();
			if (categories.length > 0) {
				filter.put(Criterion.btw(BlotterFilter.CATEGORY_LEFT, categories));
			}
			else {
				clearCategory();
			}
		} else {
			if (cat.id > 0) {
				filter.put(Criterion.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
			} else {
				clearCategory();
			}
		}
		updateCategoryFromFilter();
	}

	protected void updateProjectFromFilter() {
		if (projectSelector.isShow()) updateEntityFromFilter(BlotterFilter.PROJECT_ID, Project.class, project);
	}

	protected void updatePayeeFromFilter() {
		if (payeeSelector.isShow()) updateEntityFromFilter(BlotterFilter.PAYEE_ID, Payee.class, payee);
	}

	protected void updateLocationFromFilter() {
		if (locationSelector.isShow()) updateEntityFromFilter(BlotterFilter.LOCATION_ID, MyLocation.class, location);
	}

	protected void getSelectedTitles(Criterion c, String filterCriteriaName, Consumer<String> callback) {
		if (filterCriteriaName.equals(BlotterFilter.PROJECT_ID)) {
			projectSelector.getCheckedTitles((checkedTitles) -> {
				projectSelector.updateCheckedEntities(c.getValues());
				callback.accept(checkedTitles);
			});
			return;
		} else if (filterCriteriaName.equals(BlotterFilter.PAYEE_ID)) {
			payeeSelector.getCheckedTitles((checkedTitles) -> {
				payeeSelector.updateCheckedEntities(c.getValues());
				callback.accept(checkedTitles);
			});
			return;
		} else if (filterCriteriaName.equals(BlotterFilter.LOCATION_ID)) {
			locationSelector.getCheckedTitles((checkedTitles) -> {
				locationSelector.updateCheckedEntities(c.getValues());
				callback.accept(checkedTitles);
			});
			return;
		}
		throw new UnsupportedOperationException(filterCriteriaName + ": titles not implemented");
	}

	protected void showMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.VISIBLE);
	}

	protected void hideMinusButton(TextView textView) {
		ImageView v = findMinusButton(textView);
		v.setVisibility(View.GONE);
	}

	protected ImageView findMinusButton(TextView textView) {
		return (ImageView) textView.getTag(R.id.bMinus);
	}

	@Override
	protected void onDestroy() {
		if (payeeSelector != null) payeeSelector.onDestroy();
		if (projectSelector != null) projectSelector.onDestroy();
		if (categorySelector != null) categorySelector.onDestroy();
		if (locationSelector != null) locationSelector.onDestroy();
		if (tagSelector != null) tagSelector.onDestroy();
		super.onDestroy();
	}
}
