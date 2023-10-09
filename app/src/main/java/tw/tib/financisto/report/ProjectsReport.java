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
package tw.tib.financisto.report;

import android.content.Context;

import tw.tib.financisto.activity.SplitsBlotterActivity;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Currency;

import static tw.tib.financisto.db.DatabaseHelper.V_REPORT_PROJECTS;

import androidx.appcompat.app.AppCompatActivity;

public class ProjectsReport extends Report {

	public ProjectsReport(Context context, Currency currency) {
		super(ReportType.BY_PROJECT, context, currency);
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
        cleanupFilter(filter);
		return queryReport(db, V_REPORT_PROJECTS, filter);
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(id));
	}

    @Override
    protected Class<? extends AppCompatActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }

}
