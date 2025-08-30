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
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.utils.RecurUtils;
import tw.tib.financisto.utils.Utils;

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
		LinkedList<Criteria> categoryCriterion = new LinkedList<>();
		if (ids != null) {
			for (long id : ids) {
				if (b.includeSubcategories) {
					Category c = categories.get(id);
					if (c != null) {
						categoryCriterion.add(Criteria.btw(BlotterFilter.CATEGORY_LEFT, Integer.toString(c.left), Integer.toString(c.right)));
					}
				} else {
					categoryCriterion.add(Criteria.eq(BlotterFilter.CATEGORY_ID, Long.toString(id)));
				}
			}
		}
		Criteria categoryCriteria = null;
		if (!categoryCriterion.isEmpty()) {
			categoryCriteria = Criteria.or(categoryCriterion.toArray(new Criteria[0]));
		}

		ids = MyEntity.splitIds(b.projects);
		LinkedList<Criteria> projectCriterion = new LinkedList<>();
		if (ids != null) {
			for (long id : ids) {
				projectCriterion.add(Criteria.eq(BlotterFilter.PROJECT_ID, Long.toString(id)));
			}
		}
		Criteria projectCriteria = null;
		if (!projectCriterion.isEmpty()) {
			projectCriteria = Criteria.or(projectCriterion.toArray(new Criteria[0]));
		}

		if (categoryCriteria != null && projectCriteria != null) {
			if (b.expanded) {
				filter.put(Criteria.or(categoryCriteria, projectCriteria));
			}
			else {
				filter.put(Criteria.and(categoryCriteria, projectCriteria));
			}
		}
		else if (categoryCriteria != null) {
			filter.put(categoryCriteria);
		}
		else if (projectCriteria != null) {
			filter.put(projectCriteria);
		}

		// start date, end date
		if (b.startDate > 0 && b.endDate > 0) {
			filter.put(Criteria.and(
					Criteria.gte(BlotterFilter.DATETIME, Long.toString(b.startDate)),
					Criteria.lte(BlotterFilter.DATETIME, Long.toString(b.endDate))
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
