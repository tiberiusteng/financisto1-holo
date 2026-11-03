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

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

import greendroid.widget.QuickActionWidget;
import tw.tib.financisto.Application;
import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.db.DatabaseHelper.AccountColumns;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.dialog.DatePickerTwinDialog;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Attribute;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.SystemAttribute;
import tw.tib.financisto.model.Tag;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionAttribute;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.recur.NotificationOptions;
import tw.tib.financisto.recur.Recurrence;
import tw.tib.financisto.service.IntentTransactionProcessor;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PicturesUtil;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.view.AttributeView;
import tw.tib.financisto.view.AttributeViewFactory;
import tw.tib.financisto.widget.RateLayoutView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static tw.tib.financisto.activity.RequestPermission.isRequestingPermission;
import static tw.tib.financisto.model.Category.NO_CATEGORY_ID;
import static tw.tib.financisto.model.Category.SPLIT_CATEGORY_ID;
import static tw.tib.financisto.model.MyLocation.CURRENT_LOCATION_ID;
import static tw.tib.financisto.model.Project.NO_PROJECT_ID;
import static tw.tib.financisto.utils.Utils.text;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

public abstract class AbstractTransactionActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {
	public static final String TAG = "AbstractTxActivity";

	public static final String TRAN_ID_EXTRA = "tranId";
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	public static final String DUPLICATE_EXTRA = "isDuplicate";
	public static final String TEMPLATE_EXTRA = "isTemplate";
	public static final String DATETIME_EXTRA = "dateTimeExtra";
	public static final String NEW_FROM_TEMPLATE_EXTRA = "newFromTemplateExtra";
	public static final String TAGS_EXTRA = "tags";
	/** The form-independent entry carried by a switch between the transaction forms, see {@link FormSwitch}. */
	public static final String FORM_SWITCH_ORIGIN_EXTRA = "formSwitchOrigin";
	/** Id of the existing transaction being switched: saving updates it in place (same id) instead of adding one. */
	public static final String FORM_SWITCH_REPLACES_EXTRA = "formSwitchReplaces";

	private static final int RECURRENCE_REQUEST = 4003;
	private static final int NOTIFICATION_REQUEST = 4004;
	private static final int PICTURE_REQUEST = 4005;

	/** The origin this form was switched to with; {@code null} = not opened by a switch. */
	private FormSwitch.Origin switchOrigin;
	/** The form as read once loaded and its category applied (normalised): the baseline for "changed". */
	private FormSwitch.Origin switchBaseline;
	/** The existing transaction being switched ({@link #FORM_SWITCH_REPLACES_EXTRA}). */
	private long switchReplacesId = -1;
	private FormKindBar kindBar;

	private static final TransactionStatus[] statuses = TransactionStatus.values();

	protected RateLayoutView rateView;

	protected EditText templateName;
	protected TextView accountText;
	protected Cursor accountCursor;
	protected ListAdapter accountAdapter;

	protected Calendar dateTime;
	protected ImageButton status;
	protected Button dateText;
	protected Button timeText;

	protected EditText noteText;
	protected TagSelector<AbstractTransactionActivity> tagSelector;
	protected TextView recurText;
	protected TextView notificationText;

	private View pictureTopView;
	private View takePhotoButton;
	private View selectFromAlbumButton;
	private View removePictureButton;
	private ImageView pictureView;
	private TextView pictureDescView;

	private TextView editDisabled;

	private CheckBox ccardPayment;

	protected Account selectedAccount;

	protected String recurrence;
	protected String notificationOptions;

	protected boolean isDuplicate = false;
	private boolean isPrefilled = false;

	protected PayeeSelector<AbstractTransactionActivity> payeeSelector;
	protected ProjectSelector<AbstractTransactionActivity> projectSelector;
	protected LocationSelector<AbstractTransactionActivity> locationSelector;
	protected CategorySelector<AbstractTransactionActivity> categorySelector;

	protected boolean isRememberLastAccount;
	protected boolean isRememberLastCategory;
	protected boolean isRememberLastLocation;
	protected boolean isRememberLastProject;
	protected boolean isShowLocation;
	protected boolean isShowProject;
	protected boolean isShowNote;
	protected boolean isShowTags;
	protected boolean isShowTakePicture;
	protected boolean isShowIsCCardPayment;
	protected boolean isOpenCalculatorForTemplates;
	protected boolean isShowAccountBalanceOnSelector;
	protected boolean isTrackSplitEntityInChild;

	protected boolean isShowPayee = true;
//    protected AutoCompleteTextView payeeText;
//    protected SimpleCursorAdapter payeeAdapter;

	protected AttributeView deleteAfterExpired;

	protected DateFormat df;
	protected DateFormat tf;

	protected Utils u;

	private QuickActionWidget pickImageActionGrid;

	protected ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
	protected ActivityResultLauncher<Uri> takePicture;

	protected static final String NEW_PICTURE_URI = "newPictureUri";
	protected Uri newPictureUri;

	protected Transaction transaction = new Transaction();

	public AbstractTransactionActivity() {
	}

	protected abstract int getLayoutId();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		u = new Utils(this);

		if (MyPreferences.isSecureWindow()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
		}

		df = DateUtils.getLongDateFormat(this);
		tf = DateUtils.getTimeFormat(this);

		long t0 = System.currentTimeMillis();

		setContentView(getLayoutId());

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transaction_base), (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
					| WindowInsetsCompat.Type.statusBars()
					| WindowInsetsCompat.Type.captionBar());
			var lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.topMargin = insets.top;
			lp.bottomMargin = insets.bottom;
			v.setLayoutParams(lp);
			return WindowInsetsCompat.CONSUMED;
		});

		isRememberLastAccount = MyPreferences.isRememberAccount();
		isRememberLastCategory = isRememberLastAccount && MyPreferences.isRememberCategory();
		isRememberLastLocation = isRememberLastCategory && MyPreferences.isRememberLocation();
		isRememberLastProject = isRememberLastCategory && MyPreferences.isRememberProject();
		isShowLocation = MyPreferences.isShowLocation();
		isShowProject = MyPreferences.isShowProject();
		isShowNote = MyPreferences.isShowNote();
		isShowTags = MyPreferences.isShowTags();
		isShowTakePicture = MyPreferences.isShowTakePicture();
		isShowIsCCardPayment = MyPreferences.isShowIsCCardPayment();
		isOpenCalculatorForTemplates = MyPreferences.isOpenCalculatorForTemplates();
		isShowAccountBalanceOnSelector = MyPreferences.isShowAccountBalanceOnSelector();
		isTrackSplitEntityInChild = MyPreferences.isTrackSplitEntityInChild();

		categorySelector = new CategorySelector<>(this, db, x);
		categorySelector.setListener(this);

		long accountId = -1;
		long transactionId = -1;
		boolean isNewFromTemplate = false;
		IntentTransactionProcessor.Prefill prefill = null;
		final Intent intent = getIntent();
		if (intent != null) {
			accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
			transactionId = intent.getLongExtra(TRAN_ID_EXTRA, -1);
			transaction.dateTime = intent.getLongExtra(DATETIME_EXTRA, System.currentTimeMillis());
			if (transactionId != -1) {
				transaction = db.getTransaction(transactionId);
				transaction.categoryAttributes = db.getAllAttributesForTransaction(transactionId);
				isDuplicate = intent.getBooleanExtra(DUPLICATE_EXTRA, false);
				isNewFromTemplate = intent.getBooleanExtra(NEW_FROM_TEMPLATE_EXTRA, false);
				if (isDuplicate) {
					transaction.id = -1;
					transaction.dateTime = System.currentTimeMillis();
				}
			} else if (isIntentPrefillSupported() && IntentTransactionProcessor.hasPrefillExtras(intent)) {
				// A new transaction pre-filled by another app (the same extras as the
				// NEW_TRANSACTION service), shown for review and saved only by the user.
				prefill = new IntentTransactionProcessor(db).createPrefillFromIntent(intent, defaultAccountId(accountId));
				transaction = prefill.transaction;
				isPrefilled = true;
				applyPayeeLastCategory(prefill);
				applyCategoryLastUsed(prefill);
			}
			if (intent.hasExtra(FORM_SWITCH_ORIGIN_EXTRA)) {
				// switched from another form: show the origin as this kind of form holds it
				switchOrigin = (FormSwitch.Origin) intent.getSerializableExtra(FORM_SWITCH_ORIGIN_EXTRA);
				switchReplacesId = intent.getLongExtra(FORM_SWITCH_REPLACES_EXTRA, -1);
				transaction = FormSwitch.project(switchOrigin, kindForIntent(intent),
						intent.getLongExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, 0));
			}
			transaction.isTemplate = intent.getIntExtra(TEMPLATE_EXTRA, transaction.isTemplate);
		}

		if (transaction.id == -1) {
			accountCursor = db.getAllActiveAccounts();
		} else {
			accountCursor = db.getAccountsForTransaction(transaction);
		}
		startManagingCursor(accountCursor);
		if (isShowAccountBalanceOnSelector) {
			accountAdapter = TransactionUtils.createAccountBalanceAdapter(this, accountCursor);
		}
		else {
			accountAdapter = TransactionUtils.createAccountAdapter(this, accountCursor);
		}

		dateTime = Calendar.getInstance();
		Date date = dateTime.getTime();

		status = findViewById(R.id.status);
		status.setOnClickListener(v -> {
			ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(AbstractTransactionActivity.this, statuses);
			x.selectPosition(AbstractTransactionActivity.this, R.id.status, R.string.transaction_status, adapter, transaction.status.ordinal());
		});

		dateText = findViewById(R.id.date);
		dateText.setText(df.format(date));
		dateText.setOnClickListener(arg0 -> {
			if (MyPreferences.isUseTwinDatePicker() && Build.VERSION.SDK_INT >= 22) {
				DatePickerTwinDialog dpd = DatePickerTwinDialog.newInstance(
						dateTime.get(Calendar.YEAR),
						dateTime.get(Calendar.MONTH),
						dateTime.get(Calendar.DAY_OF_MONTH),
						(view, year, monthOfYear, dayOfMonth) -> {
							dateTime.set(year, monthOfYear, dayOfMonth);
							dateText.setText(df.format(dateTime.getTime()));
						});
				dpd.show(getFragmentManager(), DatePickerTwinDialog.TAG);
			}
			else {
				DatePickerDialog dialog = new DatePickerDialog(AbstractTransactionActivity.this,
						(view, year, monthOfYear, dayOfMonth) -> {
							dateTime.set(year, monthOfYear, dayOfMonth);
							dateText.setText(df.format(dateTime.getTime()));
						},
						dateTime.get(Calendar.YEAR),
						dateTime.get(Calendar.MONTH),
						dateTime.get(Calendar.DAY_OF_MONTH)
				);
				dialog.show();
			}
		});

		timeText = findViewById(R.id.time);
		timeText.setText(tf.format(date));
		timeText.setOnClickListener(arg0 -> {
			boolean is24Format = DateUtils.is24HourFormat(AbstractTransactionActivity.this);
			TimePickerDialog dialog = new TimePickerDialog(AbstractTransactionActivity.this,
					(picker, hourOfDay, minute) -> {
						dateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
						dateTime.set(Calendar.MINUTE, minute);
						timeText.setText(tf.format(dateTime.getTime()));
					},
					dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE), is24Format
			);
			dialog.show();
		});

		internalOnCreate();

		LinearLayout layout = findViewById(R.id.list);

		editDisabled = findViewById(R.id.edit_disabled);
		this.templateName = new EditText(this);
		if (transaction.isTemplate()) {
			x.addEditNode(layout, R.string.template_name, templateName);
		}

		rateView = new RateLayoutView(this, x, layout);

		locationSelector = new LocationSelector<>(this, db, x);
		if (transaction.locationId != CURRENT_LOCATION_ID) {
			locationSelector.setIncludeEntityIds(transaction.locationId);
		}

		projectSelector = new ProjectSelector<>(this, db, x);
		if (transaction.projectId != NO_PROJECT_ID) {
			projectSelector.setIncludeEntityIds(transaction.projectId);
		}

		createListNodes(layout);
		categorySelector.createAttributesLayout(layout);
		createCommonNodes(layout);

		// ensure views are created before start loading
		// such that loading would never finish before view available
		fetchCategories();
		locationSelector.fetchEntities();
		projectSelector.fetchEntities();

		if (transaction.isScheduled()) {
			recurText = x.addListNode(layout, R.id.recurrence_pattern, R.string.recur, R.string.recur_interval_no_recur);
			notificationText = x.addListNode(layout, R.id.notification, R.string.notification, R.string.notification_options_default);
			Attribute sa = db.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			deleteAfterExpired = AttributeViewFactory.createViewForAttribute(this, sa);
			String value = transaction.getSystemAttribute(SystemAttribute.DELETE_AFTER_EXPIRED);
			deleteAfterExpired.inflateView(layout, value != null ? value : sa.defaultValue);
		}

		Button bSave = findViewById(R.id.bSave);
		bSave.setOnClickListener(arg0 -> saveAndFinish());

		// a form switched from an existing transaction is an edit too: this button cancels and
		// leaves the transaction as it was
		final boolean isEdit = transaction.id > 0 || switchReplacesId > 0;
		Button bSaveAndNew = findViewById(R.id.bSaveAndNew);
		if (isEdit) {
			bSaveAndNew.setText(R.string.cancel);
		}
		bSaveAndNew.setOnClickListener(arg0 -> {
			if (isEdit) {
				setResult(RESULT_CANCELED);
				finish();
			} else {
				if (saveAndFinish()) {
					intent.putExtra(DATETIME_EXTRA, transaction.dateTime);
					if (isPrefilled) {
						// after a pre-fill the next transaction starts empty; a screen opened with
						// TAGS only keeps them, as before
						IntentTransactionProcessor.removePrefillExtras(intent);
					}
					prepareIntentForNext(intent);
					startActivityForResult(intent, -1);
				}
			}
		});

		if (transactionId != -1 || switchOrigin != null) {
			isOpenCalculatorForTemplates &= isNewFromTemplate;
			editTransaction(transaction);
			if (switchOrigin != null) onFormSwitchLoaded(switchOrigin);
		} else if (prefill != null) {
			editPrefilledTransaction(prefill);
		} else {
			setDateTime(transaction.dateTime);
			categorySelector.selectCategory(NO_CATEGORY_ID);
			if (accountId != -1) {
				selectAccount(accountId);
			} else {
				long lastAccountId = MyPreferences.getLastAccount();
				if (isRememberLastAccount && lastAccountId != -1) {
					selectAccount(lastAccountId);
				}
			}
			if (!isRememberLastProject) {
				projectSelector.selectEntity(NO_PROJECT_ID);
			}
			if (!isRememberLastLocation) {
				locationSelector.selectEntity(CURRENT_LOCATION_ID);
			}
			if (transaction.isScheduled()) {
				selectStatus(TransactionStatus.PN);
			}
		}

		if (intent != null) {
			List<String> intentTags = IntentTransactionProcessor.extractTagsFromIntent(intent);
			if (!intentTags.isEmpty()) {
				for (String tag : intentTags) {
					db.findOrInsertEntityByTitle(Tag.class, tag);
				}
				String intentTagsStr = "\n" + String.join("\n", intentTags) + "\n";
				transaction.tags = intentTagsStr;
				if (isShowTags && tagSelector != null) {
					tagSelector.setSelectedTags(intentTagsStr);
					tagSelector.fetchEntities();
				}
			}
		}

		if (isShowTakePicture) {
			pickMedia =
					registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
						// Callback is invoked after the user selects a media item or closes the
						// photo picker.
						if (uri != null) {
							Log.d("PhotoPicker", "Selected URI: " + uri);
							Application.getExecutor().execute(() -> {
								String fileName = PicturesUtil.saveSelectedPicture(this, uri);
								if (fileName != null) {
									new Handler(Looper.getMainLooper()).post(() -> selectPicture(fileName));
								}
							});
						} else {
							Log.d("PhotoPicker", "No media selected");
						}
					});

			takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), ret -> {
				if (ret == false) return;
				PicturesUtil.backupPictureFile(this, newPictureUri);
				selectPicture(DocumentFile.fromSingleUri(this, newPictureUri).getName());
			});
		}

		installFormKindBar();
		if (switchOrigin != null) captureSwitchBaselineWhenSettled(0);

		long t1 = System.currentTimeMillis();
		Log.i("TransactionActivity", "onCreate " + (t1 - t0) + "ms");
	}

	protected void createPayeeNode(LinearLayout layout) {
		payeeSelector = new PayeeSelector<>(this, db, x);
		if (transaction.payeeId != Payee.EMPTY.id) {
			payeeSelector.setIncludeEntityIds(transaction.payeeId);
		}
		payeeSelector.createNode(layout);
		payeeSelector.fetchEntities();
	}

	protected void createTagsNode(LinearLayout layout) {
		tagSelector = new TagSelector<>(this, db, x);
		tagSelector.createNode(layout);
	}

	protected abstract void fetchCategories();

	private boolean saveAndFinish() {
		long id = save();
		if (id > 0) {
			Intent data = new Intent();
			data.putExtra(TransactionColumns._id.name(), id);
			setResult(RESULT_OK, data);
			finish();
			return true;
		}
		return false;
	}

	private long save() {
		if (onOKClicked()) {
			// An existing transaction switched to another kind: a transaction and a transfer differ only
			// in to_account_id, so it is updated in place and keeps its id; updateTransaction recalculates
			// the account totals and running balances from the old and the new version
			if (switchReplacesId > 0) {
				transaction.id = switchReplacesId;
			}
			boolean isNew = transaction.id == -1;
			long id = db.insertOrUpdate(transaction, getAttributes());
			if (isNew) {
				MyPreferences.setLastAccount(transaction.fromAccountId);
			}
			AccountWidget.updateWidgets(this);
			return id;
		}
		return -1;
	}

	// ---------------- switching between transaction, transfer and update balance ----------------

	/** Whether this form was opened by a switch from another form (it then carries the entry in {@link #transaction}). */
	protected boolean isOpenedBySwitch() {
		return switchOrigin != null;
	}

	/** Which kind of form this is. */
	protected FormSwitch.Kind formKind() {
		return FormSwitch.Kind.TRANSACTION;
	}

	/** The kind of form the intent opens (used early in onCreate, before subclass state exists). */
	private FormSwitch.Kind kindForIntent(Intent intent) {
		if (this instanceof TransferActivity) return FormSwitch.Kind.TRANSFER;
		return intent.hasExtra(TransactionActivity.CURRENT_BALANCE_EXTRA)
				? FormSwitch.Kind.BALANCE : FormSwitch.Kind.TRANSACTION;
	}

	/**
	 * The form as it is (no validation, no saving), in the shape it would be saved: a copy of
	 * {@link #transaction} updated from the UI, so fields the form does not show (project, payee
	 * hidden by preferences, ...) keep their values. Subclasses fill in the accounts and amounts.
	 */
	protected Transaction readFormForSwitch() {
		Transaction t = transaction.clone();
		t.splits = null;
		updateTransactionFromUI(t);
		HashMap<Long, String> attrs = new HashMap<>();
		for (TransactionAttribute a : categorySelector.getAttributes()) {
			attrs.put(a.attributeId, a.value);
		}
		t.categoryAttributes = attrs;
		return t;
	}

	/** Whether the form has split parts (no switching to a transfer). TransactionActivity overrides. */
	protected boolean hasSplitsForSwitch() {
		return false;
	}

	/** Adjustments after a switched-to form has loaded (such as the direction of a zero amount). */
	protected void onFormSwitchLoaded(FormSwitch.Origin origin) {
	}

	private Boolean transferInflowHint() {
		return switchOrigin != null ? Boolean.valueOf(Boolean.TRUE.equals(switchOrigin.inflow)) : null;
	}

	private FormSwitch.Origin readCanonical() {
		return FormSwitch.canonical(formKind(), readFormForSwitch(), rateView.isIncomeSelected(), transferInflowHint());
	}

	/** This form's origin: for a switched-to form, the origin with the fields changed here; otherwise the form as it is. */
	private FormSwitch.Origin currentOrigin() {
		if (switchOrigin == null) return readCanonical();
		// no baseline yet = the form is still loading, so nothing can have been edited
		if (switchBaseline == null) return switchOrigin.copy();
		return FormSwitch.merge(switchOrigin, switchBaseline, readCanonical());
	}

	/**
	 * Reads the baseline once the category is applied (the form adjusts the attribute fields,
	 * the split section and the income/expense button at that moment); waits up to about two seconds.
	 */
	private void captureSwitchBaselineWhenSettled(int attempt) {
		getWindow().getDecorView().postDelayed(() -> {
			if (isFinishing()) return;
			if (!categorySelector.isSettled() && attempt < 40) {
				captureSwitchBaselineWhenSettled(attempt + 1);
				return;
			}
			switchBaseline = readCanonical();
		}, attempt == 0 ? 0 : 50);
	}

	/** The existing transaction being edited, as stored (before saving); null for a new one. */
	private Transaction existingInDb;

	/**
	 * The "current balance" for the update balance form: the account total minus what the
	 * transaction being edited contributes to that account (the total for a new transaction).
	 * See {@link FormSwitch#balanceExcluding}.
	 */
	protected long balanceExcludingThis(Account a) {
		long id = transaction.id > 0 ? transaction.id : switchReplacesId;
		if (id > 0 && existingInDb == null) {
			existingInDb = db.getTransaction(id);
		}
		Transaction e = id > 0 ? existingInDb : null;
		if (e == null) return a.totalAmount;
		return FormSwitch.balanceExcluding(a.totalAmount, a.id,
				e.fromAccountId, e.toAccountId, e.fromAmount, e.toAmount);
	}

	/** The tabs in the title bar. Templates, schedules and split parts keep their own titles. */
	private void installFormKindBar() {
		if (transaction.isTemplateLike() || transaction.parentId > 0) return;
		FormKindBar bar = new FormKindBar(this, formKind(), this::onFormKindTapped);
		if (bar.install()) {
			kindBar = bar;
			refreshFormKindBar();
		}
	}

	/** Greys out the kinds that cannot be switched to. Called again when the category changes (split or not). */
	protected void refreshFormKindBar() {
		if (kindBar == null) return;
		boolean splits = hasSplitsForSwitch();
		boolean locked = isPreventEditing();
		for (FormSwitch.Kind k : FormSwitch.Kind.values()) {
			kindBar.setAvailable(k, !locked && FormSwitch.check(k, splits) == null);
		}
	}

	private void onFormKindTapped(FormSwitch.Kind target) {
		// unavailable tabs are greyed out and not tappable; this only double-checks
		if (target == formKind() || isPreventEditing()
				|| FormSwitch.check(target, hasSplitsForSwitch()) != null) {
			return;
		}
		launchFormSwitch(target, currentOrigin());
	}

	/** Opens the target form with the origin and replaces this one (the result goes to whoever opened the first form). */
	private void launchFormSwitch(FormSwitch.Kind target, FormSwitch.Origin origin) {
		Intent intent = new Intent(this,
				target == FormSwitch.Kind.TRANSFER ? TransferActivity.class : TransactionActivity.class);
		intent.putExtra(FORM_SWITCH_ORIGIN_EXTRA, origin);
		long replaces = transaction.id > 0 ? transaction.id : switchReplacesId;
		if (replaces > 0) intent.putExtra(FORM_SWITCH_REPLACES_EXTRA, replaces);
		if (target == FormSwitch.Kind.BALANCE) {
			// for an existing transaction the current balance excludes the transaction itself,
			// otherwise saving would count it twice
			Account a = origin.account > 0 ? db.getAccount(origin.account) : null;
			intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a != null ? balanceExcludingThis(a) : 0L);
			if (a != null) intent.putExtra(ACCOUNT_ID_EXTRA, a.id);
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		startActivity(intent);
		overridePendingTransition(0, 0);
		// close this form: switching back and forth never stacks forms, and without the animations
		// it looks like the form changed in place
		finish();
		overridePendingTransition(0, 0);
	}

	/**
	 * Prepares the intent before "Save & new" reopens this screen: drops what a switch brought
	 * (the next entry starts empty), and in update balance mode takes the balance just saved as the
	 * current balance (the old value would shift the difference by what was just saved).
	 */
	private void prepareIntentForNext(Intent intent) {
		intent.removeExtra(FORM_SWITCH_ORIGIN_EXTRA);
		intent.removeExtra(FORM_SWITCH_REPLACES_EXTRA);
		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		if (intent.hasExtra(TransactionActivity.CURRENT_BALANCE_EXTRA)) {
			Account a = db.getAccount(transaction.fromAccountId);
			if (a != null) {
				intent.putExtra(ACCOUNT_ID_EXTRA, a.id);
				intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
			}
		}
	}

	private List<TransactionAttribute> getAttributes() {
		List<TransactionAttribute> attributes = categorySelector.getAttributes();
		if (deleteAfterExpired != null) {
			TransactionAttribute ta = deleteAfterExpired.newTransactionAttribute();
			attributes.add(ta);
		}
		return attributes;
	}

	protected void internalOnCreate() {
	}

	@Override
	protected boolean shouldLock() {
		return MyPreferences.isPinProtectedNewTransaction();
	}

	protected void createCommonNodes(LinearLayout layout) {
		int locationOrder = MyPreferences.getLocationOrder();
		int noteOrder = MyPreferences.getNoteOrder();
		int projectOrder = MyPreferences.getProjectOrder();
		int tagsOrder = MyPreferences.getTagsOrder();
		for (int i = 0; i < 7; i++) {
			if (i == locationOrder) {
				locationSelector.createNode(layout);
			}
			if (i == noteOrder) {
				if (isShowNote) {
					//note
					noteText = new EditText(this);
					noteText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
					noteText.setTextColor(getResources().getColorStateList(android.R.color.primary_text_dark));
					noteText.setId(R.id.note);
					x.addEditNode(layout, R.string.note, noteText);
				}
			}
			if (i == projectOrder) {
				projectSelector.createNode(layout);
			}
			if (i == tagsOrder) {
				if (isShowTags) {
					createTagsNode(layout);
				}
			}
		}
		if (isShowTakePicture && transaction.isNotTemplateLike()) {
			pictureTopView = x.addPictureNodeMinus(this, layout, R.id.attach_picture, R.id.take_photo, R.id.select_from_album, R.id.delete_picture, R.string.attach_picture, R.string.new_picture);
			takePhotoButton = pictureTopView.findViewById(R.id.take_photo);
			selectFromAlbumButton = pictureTopView.findViewById(R.id.select_from_album);
			removePictureButton = pictureTopView.findViewById(R.id.delete_picture);
			pictureView = pictureTopView.findViewById(R.id.picture);
			pictureDescView = pictureTopView.findViewById(R.id.data);
			takePhotoButton.setVisibility(View.VISIBLE);
			selectFromAlbumButton.setVisibility(View.VISIBLE);
			removePictureButton.setVisibility(View.GONE);
		}
		if (isShowIsCCardPayment) {
			// checkbox to register if the transaction is a credit card payment.
			// this will be used to exclude from totals in bill preview
			ccardPayment = x.addCheckboxNode(layout, R.id.is_ccard_payment,
					R.string.is_ccard_payment, R.string.is_ccard_payment_summary, false);
		}
	}

	protected abstract void createListNodes(LinearLayout layout);

	protected abstract boolean onOKClicked();

	@Override
	protected void onClick(View v, int id) {
		if (isPreventEditing()) return;
		if (isShowPayee) payeeSelector.onClick(id);
		projectSelector.onClick(id);
		categorySelector.onClick(id);
		locationSelector.onClick(id);
		if (tagSelector != null) tagSelector.onClick(id);

		if (id == R.id.account) {
			x.select(this, R.id.account, R.string.account, accountCursor, accountAdapter,
					AccountColumns.ID, getSelectedAccountId());
		}
		else if (id == R.id.recurrence_pattern) {
			Intent intent = new Intent(this, RecurrenceActivity.class);
			intent.putExtra(RecurrenceActivity.RECURRENCE_PATTERN, recurrence);
			startActivityForResult(intent, RECURRENCE_REQUEST);
		}
		else if (id == R.id.notification) {
			if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
				(!isRequestingPermission(this, POST_NOTIFICATIONS)))
			{
				Intent intent = new Intent(this, NotificationOptionsActivity.class);
				intent.putExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS, notificationOptions);
				startActivityForResult(intent, NOTIFICATION_REQUEST);
			}
		}
		else if (id == R.id.select_from_album) {
			if (PicturesUtil.getPictureFolderUri(this) != null) {
				transaction.blobKey = null;
				pickMedia.launch(new PickVisualMediaRequest.Builder()
						.setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
						.build());
			}
		}
		else if (id ==R.id.take_photo) {
			if (PicturesUtil.getPictureFolderUri(this) != null) {
				transaction.blobKey = null;
				newPictureUri = PicturesUtil.createPictureFile(this);
				takePicture.launch(newPictureUri);
			}
		}
		else if (id == R.id.delete_picture) {
			removePicture();
		}
		else if (id == R.id.is_ccard_payment) {
			ccardPayment.setChecked(!ccardPayment.isChecked());
			transaction.isCCardPayment = ccardPayment.isChecked() ? 1 : 0;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		if (isShowPayee) payeeSelector.onSelectedPos(id, selectedPos);
		projectSelector.onSelectedPos(id, selectedPos);
		locationSelector.onSelectedPos(id, selectedPos);
		switch (id) {
			case R.id.status:
				selectStatus(statuses[selectedPos]);
				break;
		}
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		if (isShowPayee) payeeSelector.onSelectedId(id, selectedId);
		categorySelector.onSelectedId(id, selectedId);
		projectSelector.onSelectedId(id, selectedId);
		locationSelector.onSelectedId(id, selectedId);
		switch (id) {
			case R.id.account:
				selectAccount(selectedId);
				break;
		}
	}

	private void selectStatus(TransactionStatus transactionStatus) {
		transaction.status = transactionStatus;
		status.setImageResource(transactionStatus.iconId);
		updateCommonUIforPreventEditing();
	}

	protected Account selectAccount(long accountId) {
		return selectAccount(accountId, true);
	}

	abstract protected Account selectAccount(long accountId, boolean selectLast); /* {
		Account a = db.getAccount(accountId);
		if (a != null) {
			accountText.setText(a.title);
			rateView.selectCurrencyFrom(a.currency);
			selectedAccount = a;
		}
		categorySelector.setSelectedAccount(a);
		return a;
	} */

	protected long getSelectedAccountId() {
		return selectedAccount != null ? selectedAccount.id : -1;
	}

	@Override
	public void onCategorySelected(Category category, boolean selectLast) {
		addOrRemoveSplits();
		categorySelector.addAttributes(transaction);
		switchIncomeExpenseButton(category);
		if (selectLast && isRememberLastLocation) {
			locationSelector.selectEntity(category.lastLocationId);
		}
		if (selectLast && isRememberLastProject) {
			projectSelector.selectEntity(category.lastProjectId);
		}
		if (isTrackSplitEntityInChild) {
			if (transaction.payeeId == Payee.EMPTY.id && payeeSelector != null) {
				payeeSelector.setNodeVisible(!category.isSplit());
			}
			if (transaction.locationId == CURRENT_LOCATION_ID) {
				locationSelector.setNodeVisible(!category.isSplit());
			}
		}
		projectSelector.setNodeVisible(!category.isSplit());
		refreshFormKindBar();
	}

	protected void addOrRemoveSplits() {
	}

	protected void switchIncomeExpenseButton(Category category) {

	}

	private void setRecurrence(String recurrence) {
		this.recurrence = recurrence;
		if (recurrence == null) {
			recurText.setText(R.string.recur_interval_no_recur);
			dateText.setEnabled(true);
			timeText.setEnabled(true);
		} else {
			dateText.setEnabled(false);
			timeText.setEnabled(false);
			Recurrence r = Recurrence.parse(recurrence);
			recurText.setText(r.toInfoString(this));
		}
	}

	private void setNotification(String notificationOptions) {
		this.notificationOptions = notificationOptions;
		if (notificationOptions == null) {
			notificationText.setText(R.string.notification_options_default);
		} else {
			NotificationOptions o = NotificationOptions.parse(notificationOptions);
			notificationText.setText(o.toInfoString(this));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		projectSelector.onActivityResult(requestCode, resultCode, data);
		categorySelector.onActivityResult(requestCode, resultCode, data);
		locationSelector.onActivityResult(requestCode, resultCode, data);
		if (isShowPayee) {
			payeeSelector.onActivityResult(requestCode, resultCode, data);
		}
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case RECURRENCE_REQUEST:
					String recurrence = data.getStringExtra(RecurrenceActivity.RECURRENCE_PATTERN);
					setRecurrence(recurrence);
					break;
				case NOTIFICATION_REQUEST:
					String notificationOptions = data.getStringExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS);
					setNotification(notificationOptions);
					break;
				default:
					break;
			}
		} else {
			if (requestCode == PICTURE_REQUEST) {
				removePicture();
			}
		}
	}

	private void selectPicture(String pictureFileName) {
		if (pictureView == null) {
			return;
		}
		if (pictureFileName == null) {
			return;
		}
		PicturesUtil.showImage(this, pictureView, pictureDescView, pictureFileName);
		pictureView.setTag(R.id.attached_picture, pictureFileName);
		transaction.attachedPicture = pictureFileName;

		takePhotoButton.setVisibility(View.GONE);
		selectFromAlbumButton.setVisibility(View.GONE);
		removePictureButton.setVisibility(View.VISIBLE);
	}

	private void removePicture() {
		if (pictureView == null) {
			return;
		}
		// TODO: delete (potentially multiple) removed pictures or not-saved new pictures
		//  when saving and cancel creating
		// can't just delete here, since it should be only committed when explicitly pressed "Save"
		transaction.attachedPicture = null;
		transaction.blobKey = null;
		pictureView.setImageBitmap(null);
		pictureView.setTag(R.id.attached_picture, null);
		pictureDescView.setText(R.string.new_picture);
		takePhotoButton.setVisibility(View.VISIBLE);
		selectFromAlbumButton.setVisibility(View.VISIBLE);
		removePictureButton.setVisibility(View.GONE);
	}

	protected void setDateTime(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		dateText.setText(df.format(d));
		timeText.setText(tf.format(d));
	}

	protected abstract void editTransaction(Transaction transaction);

	/** Whether this screen fills a new transaction from IntentTransactionProcessor's extras. */
	protected boolean isIntentPrefillSupported() {
		return false;
	}

	/**
	 * The payee's last category when the intent names no category, as when the user picks the
	 * payee by hand (TransactionActivity.onSelectedId with "remember last category" on).
	 */
	private void applyPayeeLastCategory(IntentTransactionProcessor.Prefill prefill) {
		if (transaction.categoryId == NO_CATEGORY_ID && prefill.unknownCategory == null
				&& isRememberLastCategory && prefill.payeeLastCategoryId > 0) {
			transaction.categoryId = prefill.payeeLastCategoryId;
		}
	}

	/**
	 * The category's last project and location, as when the user picks the category by hand
	 * (onCategorySelected with selectLast and the "remember" preferences). editTransaction()
	 * selects the category the way an edit does, which leaves them out. Runs before the
	 * selectors are created, so an inactive project or location is still listed.
	 */
	private void applyCategoryLastUsed(IntentTransactionProcessor.Prefill prefill) {
		if (transaction.categoryId <= 0) return;
		Category category = db.getCategory(transaction.categoryId);
		if (category == null) return;
		if (isShowProject && isRememberLastProject && prefill.projectName == null) {
			transaction.projectId = category.lastProjectId;
		}
		if (isShowLocation && isRememberLastLocation) {
			transaction.locationId = category.lastLocationId;
		}
	}

	/**
	 * The account a new transaction starts with when the intent names none, as for any new
	 * transaction: the ACCOUNT_ID_EXTRA, else the last account with "remember last account" on.
	 * 0 for none. Known before the pre-fill is built, so the amount uses that account's decimals.
	 */
	private long defaultAccountId(long accountIdExtra) {
		if (accountIdExtra != -1) {
			return accountIdExtra;
		}
		long lastAccountId = MyPreferences.getLastAccount();
		return isRememberLastAccount && lastAccountId != -1 ? lastAccountId : 0;
	}

	private void editPrefilledTransaction(IntentTransactionProcessor.Prefill prefill) {
		// The payee and project names go into their search boxes, as if typed: the user can edit
		// them, and saving picks or creates them (checkSelectedEntities). A hidden field keeps
		// the payee or project found by the intent.
		boolean payeeAsSearch = isShowPayee && prefill.payeeName != null;
		boolean projectAsSearch = isShowProject && prefill.projectName != null;
		if (payeeAsSearch) transaction.payeeId = Payee.EMPTY.id;
		if (projectAsSearch) transaction.projectId = NO_PROJECT_ID;
		editTransaction(transaction);
		if (payeeAsSearch) payeeSelector.setSearchText(prefill.payeeName);
		if (projectAsSearch) projectSelector.setSearchText(prefill.projectName);

		// say which values were not found or not valid; those fields are left empty
		List<String> problems = new ArrayList<>();
		if (prefill.unknownAccount != null) {
			problems.add(getString(R.string.intent_prefill_account_not_found, prefill.unknownAccount));
		}
		if (prefill.unknownCategory != null) {
			problems.add(getString(R.string.intent_prefill_category_not_found, prefill.unknownCategory));
		}
		if (prefill.unknownCurrency != null) {
			problems.add(getString(R.string.intent_prefill_currency_not_found, prefill.unknownCurrency));
		}
		if (prefill.invalidAmount != null) {
			problems.add(getString(R.string.intent_prefill_invalid_amount, prefill.invalidAmount));
		}
		if (prefill.invalidOriginalAmount != null) {
			problems.add(getString(R.string.intent_prefill_invalid_amount, prefill.invalidOriginalAmount));
		}
		if (prefill.originalIncomplete) {
			problems.add(getString(R.string.intent_prefill_original_incomplete));
		}
		if (!problems.isEmpty()) {
			Toast.makeText(this, String.join("\n", problems), Toast.LENGTH_LONG).show();
		}
	}

	protected void updateUIforPreventEditing() {}

	protected boolean isPreventEditing() {
		return MyPreferences.isPreventEditClearedReconciledTransactions() &&
				(transaction.status == TransactionStatus.CL ||
				 transaction.status == TransactionStatus.RC);
	}

	protected void updateCommonUIforPreventEditing() {
		boolean enabled = !isPreventEditing();

		dateText.setEnabled(enabled);
		timeText.setEnabled(enabled);
		categorySelector.setEnabled(enabled);
		if (projectSelector != null) projectSelector.setEnabled(enabled);
		if (locationSelector != null) locationSelector.setEnabled(enabled);
		if (payeeSelector != null) payeeSelector.setEnabled(enabled);
		if (noteText != null) noteText.setEnabled(enabled);
		if (tagSelector != null) tagSelector.setEnabled(enabled);
		if (pictureTopView != null) pictureTopView.setEnabled(enabled);
		if (ccardPayment != null) ((View) ccardPayment.getTag()).setEnabled(enabled);
		rateView.setEnabled(enabled);

		if (enabled) {
			editDisabled.setVisibility(View.GONE);
		}
		else {
			editDisabled.setVisibility(View.VISIBLE);
		}

		updateUIforPreventEditing();
		refreshFormKindBar();
	}

	protected void commonEditTransaction(Transaction transaction) {
		selectStatus(transaction.status);
		categorySelector.selectCategory(transaction.categoryId, false);
		projectSelector.selectEntity(transaction.projectId);
		locationSelector.selectEntity(transaction.locationId);
		setDateTime(transaction.dateTime);
		if (isShowNote) {
			noteText.setText(transaction.note);
		}
		if (isShowTags && tagSelector != null) {
			tagSelector.setSelectedTags(transaction.tags);
		}
		if (transaction.isTemplate()) {
			templateName.setText(transaction.templateName);
		}
		if (transaction.isScheduled()) {
			setRecurrence(transaction.recurrence);
			setNotification(transaction.notificationOptions);
		}
		if (isShowTakePicture) {
			selectPicture(transaction.attachedPicture);
		}
		if (isShowIsCCardPayment) {
			setIsCCardPayment(transaction.isCCardPayment);
		}

		if (transaction.isCreatedFromTemlate() && isOpenCalculatorForTemplates && !isPreventEditing()) {
			rateView.openFromAmountCalculator();
		}

		updateCommonUIforPreventEditing();
	}

	private void setIsCCardPayment(int isCCardPaymentValue) {
		transaction.isCCardPayment = isCCardPaymentValue;
		ccardPayment.setChecked(isCCardPaymentValue == 1);
	}

	protected boolean checkSelectedEntities() {
		if (isShowPayee) {
			payeeSelector.autoCreateNewEntityFromSearch();
		}
		if (isShowLocation) {
			locationSelector.autoCreateNewEntityFromSearch();
		}
		if (isShowProject) {
			projectSelector.autoCreateNewEntityFromSearch();
		}
		return true;
	}

	protected void updateTransactionFromUI(Transaction transaction) {
		transaction.categoryId = categorySelector.getSelectedCategoryId();
		if (isShowProject) {
			transaction.projectId = projectSelector.getSelectedEntityId();
		}
		if (isShowLocation) {
			transaction.locationId = locationSelector.getSelectedEntityId();
		}
		if (transaction.isScheduled()) {
			DateUtils.zeroSeconds(dateTime);
		}
		transaction.dateTime = dateTime.getTime().getTime();
		if (isShowPayee) {
			transaction.payeeId = payeeSelector.getSelectedEntityId();
		}
		if (isShowNote) {
			transaction.note = text(noteText);
		}
		if (isShowTags && tagSelector != null) {
			transaction.tags = tagSelector.getSelectedTags();
		}
		if (transaction.isTemplate()) {
			transaction.templateName = text(templateName);
		}
		if (transaction.isScheduled()) {
			transaction.recurrence = recurrence;
			transaction.notificationOptions = notificationOptions;
		}
	}

	protected void selectPayee(long payeeId) {
		if (isShowPayee) {
			payeeSelector.selectEntity(payeeId);
		}
	}

	protected void selectLastCategoryForPayee(long id) {
		Payee p = db.get(Payee.class, id);
		if (p != null) {
			if (categorySelector.getSelectedCategoryId() != SPLIT_CATEGORY_ID) {
				categorySelector.selectCategory(p.lastCategoryId);
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (payeeSelector != null) payeeSelector.onDestroy();
		if (projectSelector != null) projectSelector.onDestroy();
		if (locationSelector != null) locationSelector.onDestroy();
		if (categorySelector != null) categorySelector.onDestroy();
		if (tagSelector != null) tagSelector.onDestroy();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (newPictureUri != null) {
			outState.putString(NEW_PICTURE_URI, newPictureUri.toString());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle saved) {
		super.onRestoreInstanceState(saved);
		String savedUri = saved.getString(NEW_PICTURE_URI);
		if (savedUri != null) newPictureUri = Uri.parse(savedUri);
	}
}
