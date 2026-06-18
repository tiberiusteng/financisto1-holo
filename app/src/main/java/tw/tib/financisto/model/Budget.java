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
package tw.tib.financisto.model;

import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.Criterion;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.utils.RecurUtils;

import javax.persistence.*;

import java.util.LinkedList;
import java.util.Map;

@Entity
@Table(name = "budget")
public class Budget {
	
	@Id
	@Column(name = "_id")
	public long id = -1;

	@Column(name = "title")
	public String title;
	
	@Column(name = "category_id")
	public String categories;
	
	@Column(name = "project_id")
	public String projects;

    @Column(name = "currency_id")
    public long currencyId = -1;

    @JoinColumn(name = "budget_currency_id", required = false)
	public Currency currency;

    @JoinColumn(name = "budget_account_id", required = false)
    public Account account;

    @Column(name = "amount")
	public long amount;
	
	@Column(name = "include_subcategories")
	public boolean includeSubcategories;
	
	@Column(name = "expanded")
	public boolean expanded;

	@Column(name = "include_credit")
	public boolean includeCredit = true;

	@Column(name = "start_date")
	public long startDate;
	
	@Column(name = "end_date")
	public long endDate;
	
	@Column(name = "recur")
	public String recur;
	
	@Column(name = "recur_num")
	public long recurNum;

	@Column(name = "is_current")
	public boolean isCurrent;

	@Column(name = "parent_budget_id")
	public long parentBudgetId;
	
	@Column(name = "updated_on")
	public long updatedOn = System.currentTimeMillis();
	 
	@Column(name = "remote_key")
 	public String remoteKey ;		
	
	@Transient
	public String categoriesText = "";

	@Transient
	public String projectsText = "";
	
	@Transient
	public long spent = 0;

	@Transient
	public volatile boolean updated = false;

	public RecurUtils.Recur getRecur() {
		return RecurUtils.createFromExtraString(recur);
	}

	public static WhereFilter createWhereFilter(Budget b, Map<Long, Category> categories, Map<Long, Project> projects) {
		WhereFilter filter = new WhereFilter(b.title);

		// currency
		if (b.currency != null) {
			filter.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Long.toString(b.currency.id));
		} else if (b.account != null) {
			filter.eq(BlotterFilter.FROM_ACCOUNT_ID, Long.toString(b.account.id));
		}

		// categories & projects
		long[] ids = MyEntity.splitIds(b.categories);
		LinkedList<Criterion> categoryCriteria = new LinkedList<>();
		if (ids != null) {
			for (long id : ids) {
				if (b.includeSubcategories) {
					Category c = categories.get(id);
					if (c != null) {
						categoryCriteria.add(Criterion.btw(BlotterFilter.CATEGORY_LEFT, Integer.toString(c.left), Integer.toString(c.right)));
					}
				} else {
					categoryCriteria.add(Criterion.eq(BlotterFilter.CATEGORY_ID, Long.toString(id)));
				}
			}
		}
		Criterion categoryCriterion = null;
		if (!categoryCriteria.isEmpty()) {
			categoryCriterion = Criterion.or(categoryCriteria.toArray(new Criterion[0]));
		}

		ids = MyEntity.splitIds(b.projects);
		LinkedList<Criterion> projectCriteria = new LinkedList<>();
		if (ids != null) {
			for (long id : ids) {
				projectCriteria.add(Criterion.eq(BlotterFilter.PROJECT_ID, Long.toString(id)));
			}
		}
		Criterion projectCriterion = null;
		if (!projectCriteria.isEmpty()) {
			projectCriterion = Criterion.or(projectCriteria.toArray(new Criterion[0]));
		}

		if (categoryCriterion != null && projectCriterion != null) {
			if (b.expanded) {
				filter.put(Criterion.or(categoryCriterion, projectCriterion));
			}
			else {
				filter.put(Criterion.and(categoryCriterion, projectCriterion));
			}
		}
		else if (categoryCriterion != null) {
			filter.put(categoryCriterion);
		}
		else if (projectCriterion != null) {
			filter.put(projectCriterion);
		}

		// start date, end date
		if (b.startDate > 0 && b.endDate > 0) {
			filter.put(Criterion.and(
					Criterion.gte(BlotterFilter.DATETIME, Long.toString(b.startDate)),
					Criterion.lte(BlotterFilter.DATETIME, Long.toString(b.endDate))
			));
		} else if (b.startDate > 0) {
			filter.gte(BlotterFilter.DATETIME, Long.toString(b.startDate));
		} else if (b.endDate > 0) {
			filter.lte(BlotterFilter.DATETIME, Long.toString(b.endDate));
		}

		if (!b.includeCredit) {
			filter.lt(BlotterFilter.FROM_AMOUNT, "0");
		}

		return filter;
	}

    public Currency getBudgetCurrency() {
        return currency != null ? currency : (account != null ? account.currency : null);
    }
}
