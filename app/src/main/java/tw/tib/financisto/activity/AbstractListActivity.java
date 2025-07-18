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

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.utils.MenuItemInfo;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

public abstract class AbstractListActivity<D> extends ListActivity
		implements RefreshSupportedActivity, LoaderManager.LoaderCallbacks<D>
{
	private static final String TAG = "AbsListActivity";

	protected static final int MENU_VIEW = Menu.FIRST + 1;
	protected static final int MENU_EDIT = Menu.FIRST + 2;
	protected static final int MENU_DELETE = Menu.FIRST + 3;
	protected static final int MENU_ADD = Menu.FIRST + 4;

	private final int contentId;

	protected LayoutInflater inflater;
	protected ListAdapter adapter;
	protected DatabaseAdapter db;
	protected ImageButton bAdd;

	protected boolean enablePin = true;

	protected AbstractListActivity(int contentId) {
		this.contentId = contentId;
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(contentId);

		db = new DatabaseAdapter(this);
		db.open();

		this.inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		internalOnCreate(savedInstanceState);

		getLoaderManager().initLoader(0, null, this);

		getListView().setOnItemLongClickListener((parent, view, position, id) -> {
			PopupMenu popupMenu = new PopupMenu(AbstractListActivity.this, view);
			Menu menu = popupMenu.getMenu();
			List<MenuItemInfo> menus = createContextMenus(id);
			int i = 0;
			for (MenuItemInfo m : menus) {
				if (m.enabled) {
					menu.add(0, m.menuId, i++, m.titleId);
				}
			}
			popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), view, position, id));
			popupMenu.show();
			return true;
		});
	}

	protected abstract D loadInBackground();

	protected abstract ListAdapter createAdapter(D cursor);

	protected void internalOnCreate(Bundle savedInstanceState) {
		bAdd = findViewById(R.id.bAdd);
		bAdd.setOnClickListener(arg0 -> addItem());
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (enablePin) PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (enablePin) PinProtection.unlock(this);
	}

	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = new LinkedList<>();
		menus.add(new MenuItemInfo(MENU_VIEW, R.string.view));
		menus.add(new MenuItemInfo(MENU_EDIT, R.string.edit));
		menus.add(new MenuItemInfo(MENU_DELETE, R.string.delete));
		return menus;
	}

	public boolean onPopupItemSelected(int itemId, View view, int position, long id) {
		switch (itemId) {
			case MENU_VIEW: {
				viewItem(view, position, id);
				return true;
			}
			case MENU_EDIT: {
				editItem(view, position, id);
				return true;
			}
			case MENU_DELETE: {
				deleteItem(view, position, id);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		onItemClick(v, position, id);
	}

	protected void onItemClick(View v, int position, long id) {
		viewItem(v, position, id);
	}

	protected void addItem() {
	}

	protected abstract void deleteItem(View v, int position, long id);

	protected abstract void editItem(View v, int position, long id);

	protected abstract void viewItem(View v, int position, long id);

	public void recreateCursor() {
		Log.i(getClass().getSimpleName(), "Recreating cursor");
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void integrityCheck() {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			recreateCursor();
		}
	}

	// LoaderManager.LoaderCallbacks<Cursor>

	@SuppressLint("StaticFieldLeak")
	@Override
	public Loader<D> onCreateLoader(int id, Bundle args) {
		return new AsyncTaskLoader<>(this) {
			@Override
			protected void onStartLoading() {
				forceLoad();
			}

			@Override
			public void deliverResult(D data) {
				if (isStarted()) {
					super.deliverResult(data);
				}
			}

			@Override
			public D loadInBackground() {
				return AbstractListActivity.this.loadInBackground();
			}
		};
	}

	@Override
	public void onLoadFinished(@NonNull Loader<D> loader, D data) {
		// This will always be called from the process's main thread.
		adapter = createAdapter(data);
		long t1 = System.nanoTime();
		var listViewState = getListView().onSaveInstanceState();
		setListAdapter(adapter);
		long t2 = System.nanoTime();
		Log.d(TAG, "setListAdapter: " + (t2 - t1) / 1000 + " us");
		getListView().onRestoreInstanceState(listViewState);
		Log.d(TAG, "getListView().onRestoreInstanceState: " + (System.nanoTime() - t2) / 1000 + " us");
	}

	@Override
	public void onLoaderReset(@NonNull Loader<D> loader) {
	}
}
