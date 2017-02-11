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
package ru.orangesoftware.financisto.activity;

import java.util.List;

import android.view.View;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.LocationListAdapter;
import ru.orangesoftware.financisto.model.MyLocation;
import ru.orangesoftware.financisto.utils.AddressGeocoder;
import ru.orangesoftware.financisto.utils.MenuItemInfo;
import ru.orangesoftware.orb.EntityManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

public class LocationsListActivity extends AbstractListActivity {
	
	private static final int MENU_RESOLVE = MENU_ADD+1;
	private static final int NEW_LOCATION_REQUEST = 1;
	private static final int EDIT_LOCATION_REQUEST = 2;
	
	public LocationsListActivity() {
		super(R.layout.location_list);
	}
	
	@Override
	protected List<MenuItemInfo> createContextMenus(long id) {
		List<MenuItemInfo> menus = super.createContextMenus(id);
		menus.add(0, new MenuItemInfo(MENU_RESOLVE, R.string.resolve_address));
		return menus;
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new LocationListAdapter(db, this, cursor);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item)) {
			return true;
		}
		if (item.getItemId() == MENU_RESOLVE) {
			AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			resolveAddress(mi.position, mi.id);
		}
		return false;
	}

	private void resolveAddress(int position, long id) {
		cursor.moveToPosition(position);
		MyLocation location = EntityManager.loadFromCursor(cursor, MyLocation.class);
		startGeocode(location);
	}

	@Override
	protected Cursor createCursor() {
		return em.getAllLocations(false);
	}
	
	@Override
	protected void addItem() {
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		em.deleteLocation(id);
		cursor.requery();
	}

	@Override
	public void editItem(View v, int position, long id) {
	}

	@Override
	protected String getContextMenuHeaderTitle(int position) {
		return getString(R.string.location);
	}

	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}

    private void startGeocode(MyLocation location) {
        new GeocoderTask(location).execute(location.latitude, location.longitude);
    }

    private class GeocoderTask extends AsyncTask<Double, Void, String> {
        
    	private final AddressGeocoder geocoder;
        private final MyLocation location;

        private GeocoderTask(MyLocation location) {
        	this.geocoder = new AddressGeocoder(LocationsListActivity.this);
            this.location = location;
        }

        @Override
        protected void onPreExecute() {
        	Log.d("Geocoder", "About to enter from onPreExecute");
            // Show progress spinner and disable buttons
            setProgressBarIndeterminateVisibility(true);
            //setActionEnabled(false);
            Log.d("Geocoder", "About to exit from onPreExecute");
        }

        @Override
        protected String doInBackground(Double... args) {
        	Log.d("Geocoder", "About to enter from doInBackground");
            // Reverse geocode using location
            return geocoder.resolveAddressFromLocation(args[0], args[1]);
        }

        @Override
        protected void onPostExecute(String found) {
        	Log.d("Geocoder", "About to enter from onPostExecute");
            setProgressBarIndeterminateVisibility(false);
            // Update GUI with resolved string
            if (found != null) {
				Toast t = Toast.makeText(LocationsListActivity.this, found, Toast.LENGTH_LONG);
				t.show();
				location.resolvedAddress = found;
				em.saveLocation(location);
				recreateCursor();
            } else if (geocoder.lastException != null) {
				Toast t = Toast.makeText(LocationsListActivity.this, R.string.service_is_not_available, Toast.LENGTH_LONG);
				t.show();            	
            }
            Log.d("Geocoder", "About to exit from onPostExecute");
        }
    }

}
