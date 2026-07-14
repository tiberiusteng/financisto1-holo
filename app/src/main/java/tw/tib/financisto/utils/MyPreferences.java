/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Rodrigo Sousa - google docs backup
 * Abdsandryk Souza - report preferences
 ******************************************************************************/
package tw.tib.financisto.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.StyleRes;
import androidx.preference.PreferenceManager;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

import tw.tib.financisto.Application;
import tw.tib.financisto.R;
import tw.tib.financisto.export.ImportExportException;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.rates.ExchangeRateProvider;
import tw.tib.financisto.rates.ExchangeRateProviderFactory;

public class MyPreferences {
	private static final String TAG = "MyPreferences";

	private static final String DROPBOX_AUTH_TOKEN = "dropbox_auth_token";
	private static final String DROPBOX_AUTHORIZE = "dropbox_authorize";

	public enum AccountSortOrder {
		SORT_ORDER_ASC("sortOrder", true),
		SORT_ORDER_DESC("sortOrder", false),
		NAME("title", true),
		LAST_TRANSACTION_ASC("lastTransactionDate", true),
		LAST_TRANSACTION_DESC("lastTransactionDate", false);

		public final String property;
		public final boolean asc;

		AccountSortOrder(String property, boolean asc) {
			this.property = property;
			this.asc = asc;
		}
	}

	public enum LocationsSortOrder {
		FREQUENCY("count", false),
		TITLE("title", true);

		public final String property;
		public final boolean asc;

		LocationsSortOrder(String property, boolean asc) {
			this.property = property;
			this.asc = asc;
		}
	}

	public enum TemplatesSortOrder {
		DATE("datetime", false),
		NAME("template_name", true),
		ACCOUNT("from_account", true);

		public final String property;
		public final boolean asc;

		TemplatesSortOrder(String property, boolean asc) {
			this.property = property;
			this.asc = asc;
		}
	}

	public enum StartupScreen {
		ACCOUNTS("accounts"),
		BLOTTER("blotter"),
		BUDGETS("budgets"),
		REPORTS("reports");

		public final String tag;

		StartupScreen(String tag) {
			this.tag = tag;
		}
	}

	public enum AccountListDateType {
		LAST_TX("LAST_TX"),
		ACCOUNT_CREATION("ACCOUNT_CREATION"),
		ACCOUNT_UPDATE("ACCOUNT_UPDATE"),
		HIDDEN("HgeIDDEN");

		public final String tag;

		AccountListDateType(String tag) { this.tag = tag; }
	}

	public enum Theme {
		DARK("DARK"),
		LIGHT("LIGHT");

		public final String tag;

		Theme(String tag) { this.tag = tag; }
	}

	public enum FirstDayOfWeek {
		SYSTEM_DEFAULT("SYSTEM_DEFAULT"),
		SUNDAY("SUNDAY"),
		MONDAY("MONDAY");

		public final String tag;

		FirstDayOfWeek(String tag) { this.tag = tag; }
	}

	public enum EntitySelectorType {
		DROPDOWN("DROPDOWN"),
		SEARCH("SEARCH");

		public final String tag;

		EntitySelectorType(String tag) { this.tag = tag; }
	}

	public enum ReportAggregateUnit {
		WEEK("WEEK"),
		MONTH("MONTH"),
		YEAR("YEAR"),
		FISCAL_YEAR("FISCAL_YEAR");

		public final String tag;

		ReportAggregateUnit(String tag) { this.tag = tag; }
	}

	private static Method hasSystemFeatureMethod;

	static {
		// hack for 1.5/1.6 devices
		try {
			hasSystemFeatureMethod = PackageManager.class.getMethod("hasSystemFeature", String.class);
		} catch (NoSuchMethodException ex) {
			hasSystemFeatureMethod = null;
		}

	}

	public static boolean isSecureWindow() {
		return getBoolean("secure_window", true);
	}

	public static boolean isPinProtected() {
		return getBoolean("pin_protection", false);
	}

	public static boolean isPinProtectedNewTransaction() {
		return getBoolean("pin_protection_lock_transaction", true);
	}

	public static boolean isPinLockEnabled() {
		return isPinProtected() && getBoolean("pin_protection_lock", true);
	}

	public static boolean isPinLockUseFingerprint() {
		return isPinProtected() && getBoolean("pin_protection_use_fingerprint", false);
	}

	public static boolean isUseFingerprintFallbackToPinEnabled() {
		return isPinProtected() && getBoolean("pin_protection_use_fingerprint_fallback_to_pin", true);
	}

	public static int getLockTimeSeconds() {
		return isPinLockEnabled() ? 60 * Integer.parseInt(getString("pin_protection_lock_time", "5")) : 0;
	}

	public static String getPin() {
		return getString("pin", null);
	}

	public static AccountSortOrder getAccountSortOrder() {
		String sortOrder = getString("sort_accounts", AccountSortOrder.SORT_ORDER_ASC.name());
		return AccountSortOrder.valueOf(sortOrder);
	}

	public static boolean isBlurBalances() {
		return getBoolean("blur_balances", false);
	}

	public static LocationsSortOrder getLocationsSortOrder() {
		String sortOrder = getString("sort_locations", LocationsSortOrder.TITLE.name());
		try {
			return LocationsSortOrder.valueOf(sortOrder);
		} catch (IllegalArgumentException e) {
			return LocationsSortOrder.TITLE;
		}
	}

	public static TemplatesSortOrder getTemplatessSortOrder() {
		String sortOrder = getString("sort_templates", TemplatesSortOrder.DATE.name());
		return TemplatesSortOrder.valueOf(sortOrder);
	}

	public static long getLastAccount() {
		return getLong("last_account_id", -1);
	}

	public static void setLastAccount(long accountId) {
		edit().putLong("last_account_id", accountId).apply();
	}

	public static boolean isRememberAccount() {
		return getBoolean("remember_last_account", true);
	}

	public static boolean isRememberCategory() {
		return getBoolean("remember_last_category", false);
	}

	public static boolean isRememberLocation() {
		return getBoolean("remember_last_location", false);
	}

	public static boolean isRememberProject() {
		return getBoolean("remember_last_project", false);
	}

	private static EntitySelectorType getEntitySelectorType(String key) {
		String selectorType = getString(key, EntitySelectorType.SEARCH.name());
		try {
			return EntitySelectorType.valueOf(selectorType);
		} catch (IllegalArgumentException e) {
			return EntitySelectorType.SEARCH;
		}
	}

	public static boolean isShowAccountBalanceOnSelector() {
		return getBoolean("ntsl_show_account_balance_on_selector", false);
	}

	public static EntitySelectorType getPayeeSelectorType() {
		return getEntitySelectorType("payee_selector_type");
	}

	public static EntitySelectorType getProjectSelectorType() {
		return getEntitySelectorType("project_selector_type");
	}

	public static EntitySelectorType getLocationSelectorType() {
		return getEntitySelectorType("location_selector_type");
	}

	public static boolean isShowTakePicture() {
		return getBoolean("ntsl_show_picture", true);
	}

	public static boolean isShowCategoryInTransferScreen() {
		return getBoolean("ntsl_show_category_in_transfer", true);
	}

	public static boolean isShowPayee() {
		return getBoolean("ntsl_show_payee", true);
	}

	public static boolean isShowPayeeInTransfers() {
		return getBoolean("ntsl_show_payee_in_transfers", false);
	}

	public static boolean isShowCurrency() {
		return getBoolean("ntsl_show_currency", true);
	}

	public static boolean isEnterCurrencyDecimalPlaces() {
		return getBoolean("ntsl_enter_currency_decimal_places", true);
	}

	public static boolean isRoundUpAmount() {
		return getBoolean("ntsl_round_up_amount", true);
	}

	public static boolean isShowLocation() {
		return isLocationSupported() && getBoolean("ntsl_show_location", true);
	}

	public static int getLocationOrder() {
		return Integer.parseInt(getString("ntsl_show_location_order", "1"));
	}

	public static boolean isShowIsCCardPayment() {
		return getBoolean("ntsl_show_is_ccard_payment", true);
	}

	public static boolean isOpenCalculatorForTemplates() {
		return getBoolean("ntsl_open_calculator_for_template_transactions", true);
	}

	public static boolean isSetFocusOnAmountField() {
		return getBoolean("ntsl_set_focus_on_amount_field", false);
	}

	/**
	 * Get Google Drive backup folder registered on preferences
	 */
	public static String getGoogleDriveBackupFolder() {
		return getString("google_drive_backup_folder", null);
	}

	public static ReportAggregateUnit getReportAggregateUnit() {
		try {
			return ReportAggregateUnit.valueOf(getString("report_aggregate_unit", ReportAggregateUnit.MONTH.name()));
		}
		catch (Exception e) {
			return ReportAggregateUnit.MONTH;
		}
	}

	/**
	 * Gets the string representing reference currency registered on preferences to display chart reports.
	 *
	 * @return The string representing the currency registered as a reference to display chart reports or null if not configured yet.
	 */
	public static String getReferenceCurrencyTitle() {
		return getString("report_reference_currency", "");
	}

	/**
	 * Gets the reference currency registered on preferences to display chart reports.
	 *
	 * @return The currency registered as a reference to display chart reports or null if not configured yet.
	 */
	public static Currency getReferenceCurrency() {
		Collection<Currency> currencies = CurrencyCache.getAllCurrencies();
		Currency cur = null;
		try {
			String refCurrency = getString("report_reference_currency", null);
			if (currencies != null && !currencies.isEmpty()) {
				for (Currency currency : currencies) {
					if (currency.title.equals(refCurrency)) cur = currency;
				}
			}
		} catch (Exception e) {
			return null;
		}
		return cur;
	}

	/**
	 * Gets the period of reference (number of Months to display the 2D report) registered on preferences.
	 *
	 * @return The number of months registered as a period of reference to display chart reports or 0 if not configured yet.
	 */
	public static int getPeriodOfReference() {
		return Integer.parseInt(getString("report_reference_period", "0"));
	}

	/**
	 * Gets the reference month.
	 *
	 * @return The reference month that represents the end of the report period.
	 */
	public static int getReferenceMonth() {
		return Integer.parseInt(getString("report_reference_month", "0"));
	}

	/**
	 * Gets the flag that indicates if the sub categories will be available individually in 2D report or not.
	 *
	 * @return True if the sub categories shall be displayed in the Report 2D list of categories, false otherwise.
	 */
	public static boolean includeSubCategoriesInReport() {
		return getBoolean("report_include_sub_categories", true);
	}

	/**
	 * Gets the flag that indicates if the list of filter ids will include No Filter (no category, no project or current location) or not.
	 *
	 * @return True if no category, no project and current location shall be displayed in 2D Reports, false otherwise.
	 */
	public static boolean includeNoFilterInReport() {
		return getBoolean("report_include_no_filter", true);
	}

	/**
	 * Get the flag that indicates if the category monthly result will consider the result of its sub categories or not.
	 *
	 * @return True if the category result shall include the result of its categories, false otherwise.
	 */
	public static boolean addSubCategoriesToSum() {
		return getBoolean("report_add_sub_categories_result", false);
	}

	/**
	 * Gets the flag that indicates if the statistics calculation will consider null values or not.
	 *
	 * @return True if the null values shall impact the statistics, false otherwise.
	 */
	public static boolean considerNullResultsInReport() {
		return getBoolean("report_consider_null_results", true);
	}

	public static boolean isShowNote() {
		return getBoolean("ntsl_show_note", true);
	}

	public static int getNoteOrder() {
		return Integer.parseInt(getString("ntsl_show_note_order", "3"));
	}

	public static boolean isShowProject() {
		return getBoolean("ntsl_show_project", true);
	}

	public static int getProjectOrder() {
		return Integer.parseInt(getString("ntsl_show_project_order", "4"));
	}

	public static boolean isUseTwinDatePicker() {
		return getBoolean("ntsl_use_twin_date_picker", false);

	}

	public static boolean isUseFixedLayout() {
		return getBoolean("ntsl_use_fixed_layout", true);
	}

	public static boolean isWidgetEnabled() {
		return getBoolean("enable_widget", true);
	}

	public static boolean isTreatTransferToCCardAsPayment() {
		return getBoolean("treat_transfer_to_ccard_as_payment", true);
	}

	public static boolean isRestoreMissedScheduledTransactions() {
		return getBoolean("restore_missed_scheduled_transactions", true);
	}

	public static boolean isShowRunningBalance() {
		return getBoolean("show_running_balance", true);
	}

	public static boolean isColorizeBlotterItem() {
		return getBoolean("colorize_blotter_item", true);
	}

	public static boolean isShowProjectInBlotter() {
		return getBoolean("show_project_in_blotter", true);
	}

	public static boolean isResetCopiedTransactionStatus() {
		return getBoolean("reset_copied_transaction_status", true);
	}

	public static TransactionStatus getCopiedTransactionStatus() {
		return TransactionStatus.valueOf(getString("reset_copied_transaction_status_to", "UR"));
	}

	public static boolean isResetCopiedForeignTransactionStatus() {
		return getBoolean("reset_copied_foreign_transaction_status", false);
	}

	public static TransactionStatus getCopiedForeignTransactionStatus() {
		return TransactionStatus.valueOf(getString("reset_copied_foreign_transaction_status_to", "PN"));
	}

	public static boolean isUpdateCopiedTransactionProject() {
		return getBoolean("update_copied_transaction_project", false);
	}

	public static boolean isColorizeWeekendDate() {
		return getBoolean("colorize_weekend_date", true);
	}

	public static boolean isBlotterShowTimeOfDay() {
		return getBoolean("blotter_show_time_of_day", true);
	}

	public static boolean isPreventEditClearedReconciledTransactions() {
		return getBoolean("prevent_edit_cleared_reconciled", false);
	}

	public static boolean isQuickMenuEnabledForSplit() {
		return getBoolean("quick_menu_split_transactions", true);
	}

	public static boolean isTrackSplitEntityInChild() {
		return getBoolean("split_entity_in_child", false);
	}

	private static final String DEFAULT = "default";

	public static Context switchLocale(Context context) {
		return switchLocale(context, getString("ui_language", DEFAULT));
	}

	public static Context switchLocale(Context context, String locale) {
		if (DEFAULT.equals(locale)) {
			return context;
		} else {
			String[] a = locale.split("-");
			String language = a[0];
			String country = a.length > 1 ? a[1] : null;
			Locale newLocale = country != null ? new Locale(language, country) : new Locale(language);
			return switchLocale(context, newLocale);
		}
	}

	private static Context switchLocale(Context context, Locale locale) {
		Locale.setDefault(locale);
		Resources res = context.getResources();
		Configuration config = new Configuration(res.getConfiguration());
		config.setLocale(locale);
		context = context.createConfigurationContext(config);
		Log.i("MyPreferences", "Switching locale to " + config.locale.getDisplayName());
		return context;
	}

	public static int getFirstDayOfWeek() {
		FirstDayOfWeek fw;
		try {
			fw = FirstDayOfWeek.valueOf(getString("first_day_of_week", FirstDayOfWeek.SYSTEM_DEFAULT.name()));
		} catch (IllegalArgumentException e) {
			fw = FirstDayOfWeek.SYSTEM_DEFAULT;
		}
		switch (fw) {
			case SUNDAY -> {
				return Calendar.SUNDAY;
			}
			case MONDAY -> {
				return Calendar.MONDAY;
			}
			default -> {
				return Calendar.getInstance().getFirstDayOfWeek();
			}
		}
	}

	public static int getFiscalYearStart() {
		return getInt("fiscal_year_start", 301);
	}

	public static void setFiscalYearStart(int month, int date) {
		edit().putInt("fiscal_year_start", month * 100 + date).apply();
	}

	public static boolean isLocationSupported() {
		return isFeatureSupported(PackageManager.FEATURE_LOCATION);
	}

	public static boolean isAutoBackupEnabled() {
		return getBoolean("auto_backup_enabled", false);
	}

	public static boolean isBackupNewlines() {
		return getBoolean("backup_newlines", false);
	}

	public static int getAutoBackupTime() {
		return getInt("auto_backup_time", 600);
	}

	public static boolean isCollapseBlotterButtons() {
		return getBoolean("collapse_blotter_buttons", false);
	}

	private static boolean isFeatureSupported(String feature) {
		if (hasSystemFeatureMethod != null) {
			PackageManager pm = Application.getInstance().getPackageManager();
			try {
				return (Boolean) hasSystemFeatureMethod.invoke(pm, feature);
			} catch (Exception e) {
				Log.w("Financisto", "Some problems executing PackageManager.hasSystemFeature(" + feature + ")", e);
				return false;
			}
		}
		Log.i("Financisto", "It's an old device - no PackageManager.hasSystemFeature");
		return true;
	}

	public static boolean shouldRebuildRunningBalance() {
		return getOneTimeFlag("should_rebuild_running_balance");
	}

	public static boolean shouldUpdateHomeCurrency() {
		return getOneTimeFlag("should_update_home_currency");
	}

	public static boolean shouldUpdateAccountsLastTransactionDate() {
		return getOneTimeFlag("should_update_accounts_last_transaction_date");
	}

	public static boolean shouldUpdateSplitParentAccountId() {
		return getOneTimeFlag("should_update_split_parent_account_id");
	}

	private static boolean getOneTimeFlag(String name) {
		boolean result = getBoolean(name, true);
		if (result) {
			edit().putBoolean(name, false).apply();
		}
		return result;
	}

	public static String getDatabaseBackupFolder() {
		return getString("database_backup_folder", Application.getInstance().getExternalFilesDir(Export.BACKUP_DIRECTORY_NAME).getAbsolutePath());
	}

	public static void setDatabaseBackupFolder(String databaseBackupFolder) {
		edit().putString("database_backup_folder", databaseBackupFolder).apply();
	}

	public static String[] getReportPreferences() {
		String[] preferences = new String[8];
		preferences[0] = getReferenceCurrencyTitle();
		preferences[1] = Integer.toString(getPeriodOfReference());
		preferences[2] = Integer.toString(getReferenceMonth());
		preferences[3] = Boolean.toString(considerNullResultsInReport());
		preferences[4] = Boolean.toString(includeNoFilterInReport());
		preferences[5] = Boolean.toString(includeSubCategoriesInReport());
		preferences[6] = Boolean.toString(addSubCategoriesToSum());
		preferences[7] = getReportAggregateUnit().name();
		return preferences;
	}

	public static boolean isQuickMenuEnabledForAccount() {
		return getBoolean("quick_menu_account_enabled", true);
	}

	public static boolean isQuickMenuEnabledForTransaction() {
		return getBoolean("quick_menu_transaction_enabled", true);
	}

	public static boolean isQuickMenuShowAdditionalTransactionStatus() {
		return getBoolean("quick_menu_transaction_additional_status", false);
	}

	public static boolean isQuickMenuShowDuplicateKeepTime() {
		return getBoolean("quick_menu_transaction_duplicate_keep_time", false);
	}

	public static boolean isDuplicateKeepTimeInYesterday() {
		return getBoolean("quick_menu_transaction_duplicate_keep_time_in_yesterday", false);
	}

	public static boolean isQuickMenuShowDuplicateKeepDateTime() {
		return getBoolean("quick_menu_transaction_duplicate_keep_date_time", false);
	}

	public static String getDropboxAuthToken() {
		return getString(DROPBOX_AUTH_TOKEN, null);
	}

	public static void storeDropboxKeys(String sessionToken) {
		edit().putString(DROPBOX_AUTH_TOKEN, sessionToken)
			.putBoolean(DROPBOX_AUTHORIZE, true)
			.apply();
	}

	public static void removeDropboxKeys() {
		edit().remove(DROPBOX_AUTH_TOKEN)
			.remove(DROPBOX_AUTHORIZE)
			.apply();
	}

	public static boolean isDropboxAuthorized() {
		return getBoolean(DROPBOX_AUTHORIZE, false);
	}

	public static boolean isDropboxUploadBackups() {
		return isDropboxAuthorized() && getBoolean("dropbox_upload_backup", false);
	}

	public static boolean isDropboxUploadAutoBackups() {
		return isDropboxAuthorized() && getBoolean("dropbox_upload_autobackup", false);
	}

	public static boolean isDropboxUploadPictures() {
		return isDropboxAuthorized() && getBoolean("dropbox_upload_pictures", false);
	}

	public static boolean isDropboxDownloadPictures() {
		return isDropboxAuthorized() && getBoolean("dropbox_download_pictures", false);
	}

	public static boolean isUseHierarchicalCategorySelector() {
		return getBoolean("use_hierarchical_category_selector", true);
	}

	public static boolean isShowRecentlyUsedCategory() {
		return getBoolean("show_recently_used_category", true);
	}

	public static boolean isAutoSelectChildCategory() {
		return getBoolean("hierarchical_category_selector_select_child_immediately", true);
	}

	public static boolean isSeparateIncomeExpense() {
		return getBoolean("hierarchical_category_selector_income_expense", false);
	}

	public static AccountListDateType getAccountListDateType() {
		String accountListDateType = getString("account_list_date_type", AccountListDateType.LAST_TX.name());
		return AccountListDateType.valueOf(accountListDateType);
	}

	public static boolean isHideClosedAccounts() {
		return getBoolean("hide_closed_accounts", false);
	}

	public static boolean isPinHapticFeedbackEnabled() {
		return getBoolean("pin_protection_haptic_feedback", true);
	}

	public static boolean isShowMenuButtonOnAccountsScreen() {
		return getBoolean("show_menu_button_on_accounts_screen", true);
	}

	public static boolean isShowTransferCurrentBalance() {
		return getBoolean("show_transfer_current_balance", false);
	}

	public static StartupScreen getStartupScreen() {
		String screen = getString("startup_screen", StartupScreen.ACCOUNTS.name());
		return StartupScreen.valueOf(screen);
	}

	public static ExchangeRateProvider createExchangeRatesProvider(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		ExchangeRateProviderFactory factory = getExchangeRateProviderFactory();
		return factory.createProvider(sharedPreferences, context);
	}

	private static ExchangeRateProviderFactory getExchangeRateProviderFactory() {
		String provider = getString("exchange_rate_provider", ExchangeRateProviderFactory.freeCurrency.name());
		ExchangeRateProviderFactory r;
		try {
			r = ExchangeRateProviderFactory.valueOf(provider);
		} catch (IllegalArgumentException e) {
			return ExchangeRateProviderFactory.freeCurrency;
		}
		return r;
	}

	public static boolean isOpenExchangeRatesProviderSelected() {
		return getExchangeRateProviderFactory() == ExchangeRateProviderFactory.openexchangerates;
	}

	private static boolean getBoolean(String name, boolean defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		return sharedPreferences.getBoolean(name, defaultValue);
	}

	private static String getString(String name, String defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		return sharedPreferences.getString(name, defaultValue);
	}

	private static long getLong(String name, long defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		return sharedPreferences.getLong(name, defaultValue);
	}

	private static int getInt(String name, int defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		return sharedPreferences.getInt(name, defaultValue);
	}

	private static SharedPreferences.Editor edit() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.getInstance());
		return sharedPreferences.edit();
	}

	public static String getGoogleDriveAccount() {
		return getString("google_drive_backup_account", null);
	}

	public static boolean isGoogleDriveUploadBackups() {
		return getBoolean("google_drive_upload_backup", false);
	}

	public static boolean isGoogleDriveUploadAutoBackups() {
		return getBoolean("google_drive_upload_autobackup", false);
	}

	public static boolean isGoogleDriveUploadPictures() {
		return getBoolean("google_drive_upload_pictures", false);
	}

	public static boolean isGoogleDriveDownloadPictures() {
		return getBoolean("google_drive_download_pictures", false);
	}

	public static TransactionStatus getSmsTransactionStatus() {
		return TransactionStatus.valueOf(getString("sms_transaction_status", "PN"));
	}

	public static boolean shouldSaveSmsToTransactionNote() {
		return getBoolean("sms_transaction_note", true);
	}

	public static long getLastAutobackupCheck() {
		return getLong("last_autobackup_check", 0);
	}

	public static void updateLastAutobackupCheck() {
		edit().putLong("last_autobackup_check", System.currentTimeMillis()).apply();
	}

	public static boolean isAutoBackupReminderEnabled() {
		return getBoolean("auto_backup_reminder_enabled", true);
	}

	public static boolean isAutoBackupWarningEnabled() {
		return getBoolean("auto_backup_warning_enabled", true);
	}

	public static void notifyAutobackupFailed(Exception e) {
		edit()
				.putBoolean("auto_backup_failed_notify", isAutoBackupWarningEnabled())
				.putString("auto_backup_failed_error", messageForException(e))
				.putLong("auto_backup_failed_timestamp", System.currentTimeMillis())
				.apply();
	}

	private static String messageForException(Exception e) {
		if (e instanceof ImportExportException importExportException) {
            String message = Application.getInstance().getString(importExportException.errorResId);
			if (e.getCause() != null) {
				message += " - " + e.getCause().getMessage();
			}
			return message;
		} else {
			return e.getMessage();
		}
	}

	public static void notifyAutobackupSucceeded() {
		edit().putBoolean("auto_backup_failed_notify", false).apply();
	}

	public static AutobackupStatus getAutobackupStatus() {
		return new AutobackupStatus(
				getBoolean("auto_backup_failed_notify", false),
				getString("auto_backup_failed_error", null),
				getLong("auto_backup_failed_timestamp", 0)
		);
	}

	public static class AutobackupStatus {
		public final boolean notify;
		public final String errorMessage;
		public final long timestamp;

		private AutobackupStatus(boolean notify, String errorMessage, long timestamp) {
			this.notify = notify;
			this.errorMessage = errorMessage;
			this.timestamp = timestamp;
		}
	}

}