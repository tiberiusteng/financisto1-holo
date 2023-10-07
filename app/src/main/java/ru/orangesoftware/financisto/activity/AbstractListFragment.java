package ru.orangesoftware.financisto.activity;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.util.LinkedList;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.financisto.utils.PinProtection;

abstract public class AbstractListFragment extends ListFragment
        implements RefreshSupportedActivity, LoaderManager.LoaderCallbacks<Cursor>
{
    protected static final int MENU_VIEW = Menu.FIRST + 1;
    protected static final int MENU_EDIT = Menu.FIRST + 2;
    protected static final int MENU_DELETE = Menu.FIRST + 3;
    protected static final int MENU_ADD = Menu.FIRST + 4;

    private final int contentId;

    protected LayoutInflater inflater;
    protected ListAdapter adapter;
    protected DatabaseAdapter db;
    protected ImageButton bAdd;
    protected Parcelable listViewState;

    protected boolean enablePin = true;

    protected AbstractListFragment(int contentId) {
        this.contentId = contentId;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        db = new DatabaseAdapter(getActivity());
        db.open();

        this.inflater = inflater;
        View view = inflater.inflate(contentId, container,false);

        return view;
    }

    protected abstract Cursor createCursor();

    protected abstract ListAdapter createAdapter(Cursor cursor);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(0, null, this);

        getListView().setOnItemLongClickListener((parent, v, position, id) -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            Menu menu = popupMenu.getMenu();
            List<MenuItemInfo> menus = createContextMenus(id);
            int i = 0;
            for (MenuItemInfo m : menus) {
                if (m.enabled) {
                    menu.add(0, m.menuId, i++, m.titleId);
                }
            }
            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), v, position, id));
            popupMenu.show();
            return true;
        });

        bAdd = view.findViewById(R.id.bAdd);
        if (bAdd != null) {
            bAdd.setOnClickListener(arg0 -> addItem());
        }
    }

    @Override
    public void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (enablePin) PinProtection.lock(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (enablePin) PinProtection.unlock(getContext());
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
    public void onListItemClick(ListView l, View v, int position, long id) {
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
        Log.i("AbstractListFragment", "Recreating cursor");
        listViewState = getListView().onSaveInstanceState();
        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    // RefreshSupportedActivity

    @Override
    public void integrityCheck() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }

    // LoaderManager.LoaderCallbacks<Cursor>

    @SuppressLint("StaticFieldLeak")
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                return createCursor();
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        adapter = createAdapter(data);
        long t1 = System.currentTimeMillis();
        setListAdapter(adapter);
        long t2 = System.currentTimeMillis();
        Log.d(this.getClass().getCanonicalName(), "setListAdapter: " + (t2 - t1));

        if (listViewState != null) {
            getListView().onRestoreInstanceState(listViewState);
            Log.d(this.getClass().getCanonicalName(), "getListView().onRestoreInstanceState: " + (System.currentTimeMillis() - t2));
            listViewState = null;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
