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
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.utils.MyPreferences;

public class PayeeSelector<A extends AbstractActivity> extends MyEntitySelector<Payee, A> {

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x) {
        this(activity, em, x, R.id.payee_add, R.id.payee_clear, R.string.no_payee);
    }

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x, int actBtnId, int clearBtnId, int emptyId) {
        super(Payee.class, activity, em, x, MyPreferences.isShowPayee(activity),
                R.id.payee, actBtnId, clearBtnId, R.string.payee, emptyId, R.id.payee_filter_toggle, R.id.payee_show_list, R.id.payee_create);
        if (MyPreferences.getPayeeSelectorType(activity) == MyPreferences.EntitySelectorType.SEARCH) {
            setUseSearchAsPrimary(true);
        }
    }

    @Override
    protected Class getEditActivityClass() {
        return PayeeActivity.class;
    }

}
