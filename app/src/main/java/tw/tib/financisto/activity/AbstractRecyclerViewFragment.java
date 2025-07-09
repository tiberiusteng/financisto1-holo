package tw.tib.financisto.activity;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
//import android.content.res.Resources;
import android.database.Cursor;
//import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.utils.MenuItemInfo;
import tw.tib.financisto.utils.PinProtection;
//import tw.tib.financisto.widget.RecyclerViewFastScroller;

abstract public class AbstractRecyclerViewFragment<VH extends RecyclerView.ViewHolder> extends Fragment
        implements RefreshSupportedActivity, LoaderManager.LoaderCallbacks<Cursor>
{
    private static final String TAG = "AbstRecyclerFragment";

    protected static final int MENU_VIEW = Menu.FIRST + 1;
    protected static final int MENU_EDIT = Menu.FIRST + 2;
    protected static final int MENU_DELETE = Menu.FIRST + 3;
    protected static final int MENU_ADD = Menu.FIRST + 4;

    private final int contentId;

    protected LayoutInflater inflater;
    protected RecyclerView recyclerView;
    protected TextView empty;
    protected RecyclerView.Adapter<VH> adapter;
    protected DatabaseAdapter db;
    protected ImageButton bAdd;

    protected boolean enablePin = true;

    protected AbstractRecyclerViewFragment(int contentId) {
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

        recyclerView = view.findViewById(android.R.id.list);
        empty = view.findViewById(android.R.id.empty);

        return view;
    }

    protected abstract Cursor createCursor();

    protected abstract RecyclerView.Adapter<VH> createAdapter(Cursor cursor);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(0, null, this);

        var layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

//        // Custom FastScroller that the scrolling thumb has a minimum size
//        Resources resources = getContext().getResources();
//        new RecyclerViewFastScroller(recyclerView,
//                (StateListDrawable) resources.getDrawable(R.drawable.thumb_drawable),
//                resources.getDrawable(R.drawable.line_drawable),
//                (StateListDrawable) resources.getDrawable(R.drawable.thumb_drawable),
//                resources.getDrawable(R.drawable.line_drawable),
//                resources.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
//                resources.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
//                resources.getDimensionPixelOffset(R.dimen.fastscroll_margin));

//        getListView().setOnItemLongClickListener((parent, v, position, id) -> {
//            PopupMenu popupMenu = new PopupMenu(getActivity(), v);
//            Menu menu = popupMenu.getMenu();
//            List<MenuItemInfo> menus = createContextMenus(id);
//            int i = 0;
//            for (MenuItemInfo m : menus) {
//                if (m.enabled) {
//                    menu.add(0, m.menuId, i++, m.titleId);
//                }
//            }
//            popupMenu.setOnMenuItemClickListener(item -> onPopupItemSelected(item.getItemId(), v, position, id));
//            popupMenu.show();
//            return true;
//        });

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

    protected void onItemClick(View v, int position, long id) {
        viewItem(v, position, id);
    }

    protected void addItem() {
    }
    protected abstract void deleteItem(View v, int position, long id);

    protected abstract void editItem(View v, int position, long id);

    protected abstract void viewItem(View v, int position, long id);

    public void recreateCursor() {
        Log.i(TAG, "Recreating cursor");
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
        long t1 = System.nanoTime();
        recyclerView.setAdapter(adapter);
        long t2 = System.nanoTime();

        if (data.getCount() == 0) {
            empty.setVisibility(View.VISIBLE);
        }
        else {
            empty.setVisibility(View.GONE);
        }
        Log.d(TAG, "setListAdapter: " + (t2 - t1) / 1000 + " us");
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    }
}
