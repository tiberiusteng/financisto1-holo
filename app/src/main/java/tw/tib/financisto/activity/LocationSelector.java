/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.activity;

import tw.tib.financisto.R;
import tw.tib.financisto.db.MyEntityManager;
import tw.tib.financisto.model.MyLocation;
import tw.tib.financisto.utils.MyPreferences;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public class LocationSelector<A extends AbstractActivity> extends MyEntitySelector<MyLocation, A> {

    public LocationSelector(A activity, MyEntityManager em, ActivityLayout x) {
        this(activity, em, x, R.id.location_add, R.id.location_clear, R.string.current_location);
    }

    public LocationSelector(A activity, MyEntityManager em, ActivityLayout x, int actBtnId, int clearBtnId, int emptyId) {
        super(MyLocation.class, activity, em, x, MyPreferences.isShowLocation(activity),
                R.id.location, actBtnId, clearBtnId, R.string.location, emptyId,
                R.id.location_filter_toggle, R.id.location_show_list, R.id.location_create);
        setUseSearchAsPrimary(true);
    }

    @Override
    protected Class getEditActivityClass() {
        return LocationActivity.class;
    }

}
