/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.activity;

import android.view.View;
import tw.tib.financisto.R;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.model.MyLocation;

import java.util.List;

public class LocationsListActivity extends MyEntityListActivity<MyLocation> {

	public LocationsListActivity() {
		super(MyLocation.class, R.string.no_locations);
	}

	@Override
	protected Class getEditActivityClass() {
		return LocationActivity.class;
	}

	@Override
	protected Criteria createBlotterCriteria(MyLocation location) {
		return Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(location.id));
	}

}
