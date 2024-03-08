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
package tw.tib.financisto.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.datetime.Period;
import tw.tib.financisto.filter.Criteria;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.DatabaseHelper_;
import tw.tib.financisto.model.*;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.RecurUtils;
import tw.tib.financisto.utils.StringUtil;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Budget;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.MyEntity;
import tw.tib.financisto.model.MyLocation;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.Project;
import tw.tib.financisto.model.SystemAttribute;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionAttributeInfo;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.orb.EntityManager;
import tw.tib.orb.Expression;
import tw.tib.orb.Expressions;
import tw.tib.orb.Query;

import java.util.*;

public abstract class MyEntityManager extends EntityManager {

	protected final Context context;

	public MyEntityManager(Context context) {
		super(DatabaseHelper_.getInstance_(context));
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	public <T extends MyEntity> Cursor filterActiveEntities(Class<T> clazz, String titleLike, long... includeEntityIds) {
		return queryEntities(clazz, StringUtil.emptyIfNull(titleLike), false, true, includeEntityIds);
	}

	public <T extends MyEntity> Cursor queryEntities(Class<T> clazz, String titleLike, boolean include0, boolean onlyActive, long... includeEntityIds) {
		Query<T> q = createQuery(clazz);
		Expression include0Ex = include0 ? Expressions.gte("id", 0) : Expressions.gt("id", 0);
		Expression whereEx = include0Ex;
		if (onlyActive) {
			int count = 0;
			if (includeEntityIds != null) count = includeEntityIds.length;
			if (count > 0) {
				Expression[] ee = new Expression[count + 1];
				for (int i = 0; i < count; i++) {
					ee[i] = Expressions.eq("id", includeEntityIds[i]);
				}
				ee[count] = Expressions.eq("isActive", 1);
				whereEx = Expressions.and(include0Ex, Expressions.or(ee));
			}
			else {
				whereEx = Expressions.and(include0Ex, Expressions.eq("isActive", 1));
			}
		}
		if (!StringUtil.isEmpty(titleLike)) {
			titleLike = "%" + titleLike.replace(" ", "%") + "%";
			whereEx = Expressions.and(whereEx, Expressions.or(
					Expressions.like("title", "%" + titleLike + "%"),
					Expressions.like("title", "%" + StringUtil.capitalize(titleLike) + "%")
			));
		}
		q.where(whereEx).asc("title");
		return q.execute();
	}

	public  <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0, boolean onlyActive, long... includeEntityIds) {
		return getAllEntitiesList(clazz, include0, onlyActive, null, includeEntityIds);
	}

	public  <T extends MyEntity> ArrayList<T> getAllEntitiesList(Class<T> clazz, boolean include0, boolean onlyActive, String filter, long... includeEntityIds) {
		try (Cursor c = queryEntities(clazz, filter, include0, onlyActive, includeEntityIds)) {
			T e0 = null;
			ArrayList<T> list = new ArrayList<>();
			while (c.moveToNext()) {
				T e = EntityManager.loadFromCursor(c, clazz);
				if (e.id == 0) {
					e0 = e;
				} else {
					list.add(e);
				}
			}
			if (e0 != null) {
				list.add(0, e0);
			}
			return list;
		}
	}

	/* ===============================================
	 * LOCATION
	 * =============================================== */

	public Cursor getAllLocations(boolean includeCurrentLocation) {
		Query<MyLocation> q = createQuery(MyLocation.class);
		if (!includeCurrentLocation) {
			q.where(Expressions.gt("id", 0));
		}
		MyPreferences.LocationsSortOrder sortOrder = MyPreferences.getLocationsSortOrder(context);
		if (sortOrder.asc) {
			q.asc(sortOrder.property);
		} else {
			q.desc(sortOrder.property);
		}
		if (sortOrder != MyPreferences.LocationsSortOrder.TITLE) {
			q.asc(MyPreferences.LocationsSortOrder.TITLE.property);
		}
		return q.execute();
	}

	public List<MyLocation> getAllLocationsList(boolean includeNoLocation) {
		try (Cursor c = getAllLocations(includeNoLocation)) {
			MyLocation e0 = null;
			ArrayList<MyLocation> list = new ArrayList<>();
			while (c.moveToNext()) {
				MyLocation e = EntityManager.loadFromCursor(c, MyLocation.class);
				if (e.id == 0) {
					e0 = e;
				} else {
					list.add(e);
				}
			}
			if (e0 != null) {
				list.add(0, e0);
			}
			return list;
		}
	}

	public Map<Long, MyLocation> getAllLocationsByIdMap(boolean includeNoLocation) {
		List<MyLocation> locations = getAllLocationsList(includeNoLocation);
		Map<Long, MyLocation> map = new HashMap<>();
		for (MyLocation location : locations) {
			map.put(location.id, location);
		}
		return map;
	}

	public void deleteLocation(long id) {
		SQLiteDatabase db = db();
		db.beginTransaction();
		try {
			delete(MyLocation.class, id);
			ContentValues values = new ContentValues();
			values.put("location_id", 0);
			db.update("transactions", values, "location_id=?", new String[]{String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public long saveLocation(MyLocation location) {
		return saveOrUpdate(location);
	}

	/* ===============================================
	 * TRANSACTION INFO
	 * =============================================== */

	public TransactionInfo getTransactionInfo(long transactionId) {
		return get(TransactionInfo.class, transactionId);
	}

	public List<TransactionAttributeInfo> getAttributesForTransaction(long transactionId) {
		Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class).asc("name");
		q.where(Expressions.and(
				Expressions.eq("transactionId", transactionId),
				Expressions.gte("attributeId", 0)
		));
		try (Cursor c = q.execute()) {
			List<TransactionAttributeInfo> list = new LinkedList<>();
			while (c.moveToNext()) {
				TransactionAttributeInfo ti = loadFromCursor(c, TransactionAttributeInfo.class);
				list.add(ti);
			}
			return list;
		}

	}

	public TransactionAttributeInfo getSystemAttributeForTransaction(SystemAttribute sa, long transactionId) {
		Query<TransactionAttributeInfo> q = createQuery(TransactionAttributeInfo.class);
		q.where(Expressions.and(
				Expressions.eq("transactionId", transactionId),
				Expressions.eq("attributeId", sa.id)
		));
		try (Cursor c = q.execute()) {
			if (c.moveToFirst()) {
				return loadFromCursor(c, TransactionAttributeInfo.class);
			}
			return null;
		}
	}

	/* ===============================================
	 * ACCOUNT
	 * =============================================== */
	public Cursor getAccountByNumber(String numberEnding) {
		Query<Account> q = createQuery(Account.class);
		q.where(Expressions.like(DatabaseHelper.AccountColumns.NUMBER, "%" + numberEnding));
		return q.execute();
	}

	public Account getAccount(long id) {
		return get(Account.class, id);
	}

	public Cursor getAccountsForTransaction(Transaction t) {
		return getAllAccounts(true, null, t.fromAccountId, t.toAccountId);
	}

	public Cursor getAllActiveAccounts() {
		return getAllAccounts(true, null);
	}

	public Cursor getAllActiveAccountsWithFilter(String filter) {
		return getAllAccounts(true, filter);
	}

	public Cursor getAllAccounts() {
		return getAllAccounts(false, null);
	}

	public Cursor getAllAccountsWithFilter(String filter) {
		return getAllAccounts(false, filter);
	}

	private Cursor getAllAccounts(boolean isActiveOnly, String filter, long... includeAccounts) {
		MyPreferences.AccountSortOrder sortOrder = MyPreferences.getAccountSortOrder(context);
		Query<AccountForSearch> q = createQuery(AccountForSearch.class);
		ArrayList<Expression> e = new ArrayList<>();

		if (filter != null && !filter.isEmpty()) {
			e.add(Expressions.or(
					Expressions.like("title", String.format("%%%s%%", filter)),
					Expressions.like("issuer", String.format("%%%s%%", filter)),
					Expressions.like("number", String.format("%%%s%%", filter)),
					Expressions.like("note", String.format("%%%s%%", filter)),
					Expressions.like("currencyName", String.format("%%%s%%", filter))
			));
		}
		if (isActiveOnly) {
			int count = includeAccounts.length;
			if (count > 0) {
				Expression[] ee = new Expression[count + 1];
				for (int i = 0; i < count; i++) {
					ee[i] = Expressions.eq("id", includeAccounts[i]);
				}
				ee[count] = Expressions.eq("isActive", 1);
				e.add(Expressions.or(ee));
			} else {
				e.add(Expressions.eq("isActive", 1));
			}
		}
		if (e.size() > 0) {
			q.where(Expressions.and(e.toArray(new Expression[0])));
		}
		q.desc("isActive");
		if (sortOrder.asc) {
			q.asc(sortOrder.property);
		} else {
			q.desc(sortOrder.property);
		}
		return q.asc("title").execute();
	}

	public long saveAccount(Account account) {
		return saveOrUpdate(account);
	}

	public List<Account> getAllAccountsList() {
		return getAllAccountsListWithFilter(null, true);
	}

	public List<Account> getAllAccountsListWithClosed() {
		return getAllAccountsListWithFilter(null, false);
	}

	public List<Account> getAllAccountsListWithFilter(String filter, boolean canHideClosed) {
		List<Account> list = new ArrayList<>();
		Cursor c;
		if (canHideClosed && MyPreferences.isHideClosedAccounts(context)) {
			c = getAllActiveAccountsWithFilter(filter);
		} else {
			c = getAllAccountsWithFilter(filter);
		}
		try (c) {
			while (c.moveToNext()) {
				Account a = EntityManager.loadFromCursor(c, Account.class);
				list.add(a);
			}
		}
		return list;
	}

	public Map<Long, Account> getAllAccountsMap() {
		Map<Long, Account> accountsMap = new HashMap<>();
		List<Account> list = getAllAccountsListWithClosed();
		for (Account account : list) {
			accountsMap.put(account.id, account);
		}
		return accountsMap;
	}

	/* ===============================================
	 * CURRENCY
	 * =============================================== */

	private static final String UPDATE_DEFAULT_FLAG = "update currency set is_default=0";

	public long saveOrUpdate(tw.tib.financisto.model.Currency currency) {
		SQLiteDatabase db = db();
		db.beginTransaction();
		try {
			if (currency.isDefault) {
				db.execSQL(UPDATE_DEFAULT_FLAG);
			}
			long id = super.saveOrUpdate(currency);
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}

	public int deleteCurrency(long id) {
		String sid = String.valueOf(id);
		tw.tib.financisto.model.Currency c = load(tw.tib.financisto.model.Currency.class, id);
		return db().delete(DatabaseHelper.CURRENCY_TABLE, "_id=? AND NOT EXISTS (SELECT 1 FROM " + DatabaseHelper.ACCOUNT_TABLE + " WHERE " + DatabaseHelper.AccountColumns.CURRENCY_ID + "=?)",
				new String[]{sid, sid});
	}

	public Cursor getAllCurrencies(String sortBy) {
		Query<tw.tib.financisto.model.Currency> q = createQuery(tw.tib.financisto.model.Currency.class);
		return q.desc("isDefault").asc(sortBy).execute();
	}

	public List<tw.tib.financisto.model.Currency> getAllCurrenciesList() {
		return getAllCurrenciesList("name");
	}

	public List<tw.tib.financisto.model.Currency> getAllCurrenciesList(String sortBy) {
		Query<tw.tib.financisto.model.Currency> q = createQuery(tw.tib.financisto.model.Currency.class);
		return q.desc("isDefault").asc(sortBy).list();
	}

	public Map<String, tw.tib.financisto.model.Currency> getAllCurrenciesByTtitleMap() {
		return entitiesAsTitleMap(getAllCurrenciesList("name"));
	}

	/* ===============================================
	 * TRANSACTIONS
	 * =============================================== */

//	public Cursor getBlotter(WhereFilter blotterFilter) {
//		long t0 = System.currentTimeMillis();
//		try {
//			Query<TransactionInfo> q = createQuery(TransactionInfo.class);
//			if (!blotterFilter.isEmpty()) {
//				q.where(blotterFilter.toWhereExpression());
//			}
//			q.desc("dateTime");
//			return q.list();
//		} finally {
//			Log.d("BLOTTER", "getBlotter executed in "+(System.currentTimeMillis()-t0)+"ms");
//		}
//	}
//
//	public Cursor getTransactions(WhereFilter blotterFilter) {
//		return null;
//	}

//	public Cursor getAllProjects(boolean includeNoProject) {
//		Query<Project> q = createQuery(Project.class);
//		if (!includeNoProject) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}

	public Project getProject(long id) {
		return get(Project.class, id);
	}

	public ArrayList<Project> getAllProjectsList(boolean includeNoProject) {
		return getAllEntitiesList(Project.class, includeNoProject, false);
	}

	public ArrayList<Project> getActiveProjectsList(boolean includeNoProject, long... includeProjectIds) {
		return getAllEntitiesList(Project.class, includeNoProject, true, includeProjectIds);
	}

	public Map<String, Project> getAllProjectsByTitleMap(boolean includeNoProject) {
		return entitiesAsTitleMap(getAllProjectsList(includeNoProject));
	}

	public Map<Long, Project> getAllProjectsByIdMap(boolean includeNoProject) {
		return entitiesAsIdMap(getAllProjectsList(includeNoProject));
	}

//	public Category getCategoryByLeft(long left) {
//		Query<Category> q = createQuery(Category.class);
//		q.where(Expressions.eq("left", left));
//		return q.uniqueResult();
//	}
//
//	public Cursor getAllCategories(boolean includeNoCategory) {
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		if (!includeNoCategory) {
//			q.where(Expressions.neq("id", 0));
//		}
//		return q.list();
//	}
//
//	public Cursor getAllCategoriesWithoutSubtree(long id) {
//		Category c = load(Category.class, id);
//		Query<CategoryInfo> q = createQuery(CategoryInfo.class);
//		q.where(Expressions.not(Expressions.and(
//				Expressions.gte("left", c.left),
//				Expressions.lte("right", c.right)
//		)));
//		return q.list();
//	}

	public long insertBudget(Budget budget) {
		SQLiteDatabase db = db();
		budget.remoteKey = null;

		db.beginTransaction();
		try {
			if (budget.id > 0) {
				deleteBudget(budget.id);
			}
			long id = 0;
			RecurUtils.Recur recur = RecurUtils.createFromExtraString(budget.recur);
			Period[] periods = RecurUtils.periods(recur);
			for (int i = 0; i < periods.length; i++) {
				Period p = periods[i];
				budget.id = -1;
				budget.parentBudgetId = id;
				budget.recurNum = i;
				budget.startDate = p.start;
				budget.endDate = p.end;
				long bid = super.saveOrUpdate(budget);
				if (i == 0) {
					id = bid;
				}
			}
			db.setTransactionSuccessful();
			return id;
		} finally {
			db.endTransaction();
		}
	}

	public void deleteBudget(long id) {
		SQLiteDatabase db = db();
		db.delete(DatabaseHelper.BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
		db.delete(DatabaseHelper.BUDGET_TABLE, "parent_budget_id=?", new String[]{String.valueOf(id)});
	}

	public void deleteBudgetOneEntry(long id) {
		db().delete(DatabaseHelper.BUDGET_TABLE, "_id=?", new String[]{String.valueOf(id)});
	}

	public ArrayList<Budget> getAllBudgets(WhereFilter filter) {
		Query<Budget> q = createQuery(Budget.class);
		Criteria c = filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			long start = c.getLongValue1();
			long end = c.getLongValue2();
			q.where(Expressions.and(Expressions.lte("startDate", end), Expressions.gte("endDate", start)));
		}
		q.asc("title");
		try (Cursor cursor = q.execute()) {
			ArrayList<Budget> list = new ArrayList<>();
			while (cursor.moveToNext()) {
				Budget b = MyEntityManager.loadFromCursor(cursor, Budget.class);
				list.add(b);
			}
			return list;
		}
	}

	public void deleteProject(long id) {
		SQLiteDatabase db = db();
		db.beginTransaction();
		try {
			delete(Project.class, id);
			ContentValues values = new ContentValues();
			values.put("project_id", 0);
			db.update("transactions", values, "project_id=?", new String[]{String.valueOf(id)});
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public ArrayList<TransactionInfo> getAllScheduledTransactions() {
		Query<TransactionInfo> q = createQuery(TransactionInfo.class);
		q.where(Expressions.and(
				Expressions.eq("isTemplate", 2),
				Expressions.eq("parentId", 0)));
		return (ArrayList<TransactionInfo>) q.list();
	}

	public Category getCategory(long id) {
		return get(Category.class, id);
	}

	public ArrayList<Category> getAllCategoriesList(boolean includeNoCategory) {
		return getAllEntitiesList(Category.class, includeNoCategory, false);
	}

	public <T extends MyEntity> T findOrInsertEntityByTitle(Class<T> entityClass, String title) {
		if (Utils.isEmpty(title)) {
			return newEntity(entityClass);
		} else {
			T e = findEntityByTitle(entityClass, title);
			if (e == null) {
				e = newEntity(entityClass);
				e.title = title;
				e.id = saveOrUpdate(e);
			}
			return e;
		}
	}

	private <T extends MyEntity> T newEntity(Class<T> entityClass) {
		try {
			return entityClass.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public <T extends MyEntity> T findEntityByTitle(Class<T> entityClass, String title) {
		Query<T> q = createQuery(entityClass);
		q.where(Expressions.eq("title", title));
		return q.uniqueResult();
	}

	@SuppressLint("Range")
	public <T extends MyEntity> long getEntityIdByTitle(Class<T> entityClass, String title) {
		Query<T> q = createQuery(entityClass);
		q.where(Expressions.eq("title", title));
		try (Cursor c = q.execute()) {
			if (c.moveToFirst()) {
				return c.getLong(c.getColumnIndex("e__id"));
			}
			else {
				return 0;
			}
		}
	}

	public Payee getPayee(String payee) {
		Query<Payee> q = createQuery(Payee.class);
		q.where(Expressions.eq("title", payee));
		return q.uniqueResult();
	}

	public <T extends MyEntity> Cursor getAllEntities(Class<T> entityClass) {
		return queryEntities(entityClass, null, false, false);
	}

	public List<Payee> getAllPayeeList() {
		return getAllEntitiesList(Payee.class, true, false);
	}

	public Map<String, Payee> getAllPayeeByTitleMap() {
		return entitiesAsTitleMap(getAllPayeeList());
	}

	public Map<Long, Payee> getAllPayeeByIdMap() {
		return entitiesAsIdMap(getAllPayeeList());
	}

	public Cursor getAllPayeesLike(String constraint) {
		return filterAllEntities(Payee.class, constraint);
	}

	public <T extends MyEntity> Cursor filterAllEntities(Class<T> entityClass, String titleFilter) {
		return queryEntities(entityClass, StringUtil.emptyIfNull(titleFilter), false, false);
	}

	public List<Transaction> getSplitsForTransaction(long transactionId) {
		Query<Transaction> q = createQuery(Transaction.class);
		q.where(Expressions.eq("parentId", transactionId));
		return q.list();
	}

	public List<TransactionInfo> getSplitsInfoForTransaction(long transactionId) {
		Query<TransactionInfo> q = createQuery(TransactionInfo.class);
		q.where(Expressions.eq("parentId", transactionId));
		return q.list();
	}

	public List<TransactionInfo> getTransactionsForAccount(long accountId) {
		Query<TransactionInfo> q = createQuery(TransactionInfo.class);
		q.where(Expressions.and(
				Expressions.eq("fromAccount.id", accountId),
				Expressions.eq("parentId", 0)
		));
		q.desc("dateTime");
		return q.list();
	}

	void reInsertEntity(MyEntity e) {
		if (get(e.getClass(), e.id) == null) {
			reInsert(e);
		}
	}

	public tw.tib.financisto.model.Currency getHomeCurrency() {
		Query<tw.tib.financisto.model.Currency> q = createQuery(tw.tib.financisto.model.Currency.class);
		q.where(Expressions.eq("isDefault", "1")); //uh-oh
		tw.tib.financisto.model.Currency homeCurrency = q.uniqueResult();
		if (homeCurrency == null) {
			homeCurrency = Currency.EMPTY;
		}
		return homeCurrency;
	}

	private static <T extends MyEntity> Map<String, T> entitiesAsTitleMap(List<T> entities) {
		Map<String, T> map = new HashMap<>();
		for (T e : entities) {
			map.put(e.title, e);
		}
		return map;
	}

	private static <T extends MyEntity> Map<Long, T> entitiesAsIdMap(List<T> entities) {
		Map<Long, T> map = new HashMap<>();
		for (T e : entities) {
			map.put(e.id, e);
		}
		return map;
	}

}
