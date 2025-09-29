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
import tw.tib.financisto.R;
import tw.tib.financisto.datetime.DateUtils;
import tw.tib.financisto.db.DatabaseHelper.AccountColumns;
import tw.tib.financisto.db.DatabaseHelper.TransactionColumns;
import tw.tib.financisto.dialog.DatePickerTwinDialog;
import tw.tib.financisto.model.*;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Attribute;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.SystemAttribute;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionAttribute;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.recur.NotificationOptions;
import tw.tib.financisto.recur.Recurrence;
import tw.tib.financisto.utils.EnumUtils;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PicturesUtil;
import tw.tib.financisto.utils.TransactionUtils;
import tw.tib.financisto.utils.Utils;
import tw.tib.financisto.view.AttributeView;
import tw.tib.financisto.view.AttributeViewFactory;
import tw.tib.financisto.widget.RateLayoutView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import static tw.tib.financisto.activity.RequestPermission.isRequestingPermission;
import static tw.tib.financisto.model.Category.NO_CATEGORY_ID;
import static tw.tib.financisto.model.MyLocation.CURRENT_LOCATION_ID;
import static tw.tib.financisto.model.Project.NO_PROJECT_ID;
import static tw.tib.financisto.utils.Utils.text;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class AbstractTransactionActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

	public static final String TRAN_ID_EXTRA = "tranId";
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	public static final String DUPLICATE_EXTRA = "isDuplicate";
	public static final String TEMPLATE_EXTRA = "isTemplate";
	public static final String DATETIME_EXTRA = "dateTimeExtra";
	public static final String NEW_FROM_TEMPLATE_EXTRA = "newFromTemplateExtra";

	private static final int RECURRENCE_REQUEST = 4003;
	private static final int NOTIFICATION_REQUEST = 4004;
	private static final int PICTURE_REQUEST = 4005;

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
	protected TextView recurText;
	protected TextView notificationText;

	private ImageView pictureView;
	private TextView pictureDescView;

	private CheckBox ccardPayment;

	protected Account selectedAccount;

	protected String recurrence;
	protected String notificationOptions;

	protected boolean isDuplicate = false;

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
	protected boolean isShowTakePicture;
	protected boolean isShowIsCCardPayment;
	protected boolean isOpenCalculatorForTemplates;
	protected boolean isShowAccountBalanceOnSelector;

	protected boolean isShowPayee = true;
//    protected AutoCompleteTextView payeeText;
//    protected SimpleCursorAdapter payeeAdapter;

	protected AttributeView deleteAfterExpired;

	protected DateFormat df;
	protected DateFormat tf;

	protected Utils u;

	private QuickActionWidget pickImageActionGrid;

	ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

	protected Transaction transaction = new Transaction();

	public AbstractTransactionActivity() {
	}

	protected abstract int getLayoutId();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		u = new Utils(this);

		if (MyPreferences.isSecureWindow(this)) {
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

		isRememberLastAccount = MyPreferences.isRememberAccount(this);
		isRememberLastCategory = isRememberLastAccount && MyPreferences.isRememberCategory(this);
		isRememberLastLocation = isRememberLastCategory && MyPreferences.isRememberLocation(this);
		isRememberLastProject = isRememberLastCategory && MyPreferences.isRememberProject(this);
		isShowLocation = MyPreferences.isShowLocation(this);
		isShowProject = MyPreferences.isShowProject(this);
		isShowNote = MyPreferences.isShowNote(this);
		isShowTakePicture = MyPreferences.isShowTakePicture(this);
		isShowIsCCardPayment = MyPreferences.isShowIsCCardPayment(this);
		isOpenCalculatorForTemplates = MyPreferences.isOpenCalculatorForTemplates(this);
		isShowAccountBalanceOnSelector = MyPreferences.isShowAccountBalanceOnSelector(this);

		categorySelector = new CategorySelector<>(this, db, x);
		categorySelector.setListener(this);
		fetchCategories();

		long accountId = -1;
		long transactionId = -1;
		boolean isNewFromTemplate = false;
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
			if (MyPreferences.isUseTwinDatePicker(this) && Build.VERSION.SDK_INT >= 22) {
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

		this.templateName = new EditText(this);
		if (transaction.isTemplate()) {
			x.addEditNode(layout, R.string.template_name, templateName);
		}

		rateView = new RateLayoutView(this, x, layout);

		locationSelector = new LocationSelector<>(this, db, x);
		if (transaction.locationId != CURRENT_LOCATION_ID) {
			locationSelector.setIncludeEntityIds(transaction.locationId);
		}
		locationSelector.fetchEntities();

		projectSelector = new ProjectSelector<>(this, db, x);
		if (transaction.projectId != NO_PROJECT_ID) {
			projectSelector.setIncludeEntityIds(transaction.projectId);
		}
		projectSelector.fetchEntities();

		createListNodes(layout);
		categorySelector.createAttributesLayout(layout);
		createCommonNodes(layout);

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

		final boolean isEdit = transaction.id > 0;
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
					startActivityForResult(intent, -1);
				}
			}
		});

		if (transactionId != -1) {
			isOpenCalculatorForTemplates &= isNewFromTemplate;
			editTransaction(transaction);
		} else {
			setDateTime(transaction.dateTime);
			categorySelector.selectCategory(NO_CATEGORY_ID);
			if (accountId != -1) {
				selectAccount(accountId);
			} else {
				long lastAccountId = MyPreferences.getLastAccount(this);
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

		if (isShowTakePicture) {
			pickMedia =
					registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
						// Callback is invoked after the user selects a media item or closes the
						// photo picker.
						if (uri != null) {
							Log.d("PhotoPicker", "Selected URI: " + uri);
							var executor = Executors.newSingleThreadExecutor();
							var handler = new Handler(Looper.getMainLooper());
							executor.execute(() -> {
								String fileName = PicturesUtil.saveSelectedPicture(this, uri);
								if (fileName != null) {
									handler.post(() -> selectPicture(fileName));
								}
							});
						} else {
							Log.d("PhotoPicker", "No media selected");
						}
					});
		}

		long t1 = System.currentTimeMillis();
		Log.i("TransactionActivity", "onCreate " + (t1 - t0) + "ms");
	}

	protected void createPayeeNode(LinearLayout layout) {
		payeeSelector = new PayeeSelector<>(this, db, x);
		if (transaction.payeeId != Payee.EMPTY.id) {
			payeeSelector.setIncludeEntityIds(transaction.payeeId);
		}
		payeeSelector.fetchEntities();
		payeeSelector.createNode(layout);
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
			boolean isNew = transaction.id == -1;
			long id = db.insertOrUpdate(transaction, getAttributes());
			if (isNew) {
				MyPreferences.setLastAccount(this, transaction.fromAccountId);
			}
			AccountWidget.updateWidgets(this);
			return id;
		}
		return -1;
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
		return MyPreferences.isPinProtectedNewTransaction(this);
	}

	protected void createCommonNodes(LinearLayout layout) {
		int locationOrder = MyPreferences.getLocationOrder(this);
		int noteOrder = MyPreferences.getNoteOrder(this);
		int projectOrder = MyPreferences.getProjectOrder(this);
		for (int i = 0; i < 6; i++) {
			if (i == locationOrder) {
				locationSelector.createNode(layout);
			}
			if (i == noteOrder) {
				if (isShowNote) {
					//note
					noteText = new EditText(this);
					noteText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
					x.addEditNode(layout, R.string.note, noteText);
				}
			}
			if (i == projectOrder) {
				projectSelector.createNode(layout);
			}
		}
		if (isShowTakePicture && transaction.isNotTemplateLike()) {
			View v = x.addPictureNodeMinus(this, layout, R.id.attach_picture, R.id.delete_picture, R.string.attach_picture, R.string.new_picture);
			pictureView = v.findViewById(R.id.picture);
			pictureDescView = v.findViewById(R.id.data);
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
		if (isShowPayee) payeeSelector.onClick(id);
		projectSelector.onClick(id);
		categorySelector.onClick(id);
		locationSelector.onClick(id);
		switch (id) {
			case R.id.account:
				x.select(this, R.id.account, R.string.account, accountCursor, accountAdapter,
						AccountColumns.ID, getSelectedAccountId());
				break;
			case R.id.recurrence_pattern: {
				Intent intent = new Intent(this, RecurrenceActivity.class);
				intent.putExtra(RecurrenceActivity.RECURRENCE_PATTERN, recurrence);
				startActivityForResult(intent, RECURRENCE_REQUEST);
				break;
			}
			case R.id.notification: {
				if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) ||
					(!isRequestingPermission(this, POST_NOTIFICATIONS)))
				{
					Intent intent = new Intent(this, NotificationOptionsActivity.class);
					intent.putExtra(NotificationOptionsActivity.NOTIFICATION_OPTIONS, notificationOptions);
					startActivityForResult(intent, NOTIFICATION_REQUEST);
				}
				break;
			}
			case R.id.attach_picture: {
				if (PicturesUtil.getPictureFolderUri(this) != null) {
					transaction.blobKey = null;
					pickMedia.launch(new PickVisualMediaRequest.Builder()
							.setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
							.build());
				}
				break;
			}
			case R.id.delete_picture: {
				removePicture();
				break;
			}
			case R.id.is_ccard_payment: {
				ccardPayment.setChecked(!ccardPayment.isChecked());
				transaction.isCCardPayment = ccardPayment.isChecked() ? 1 : 0;
			}
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
		projectSelector.setNodeVisible(!category.isSplit());
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
	}

	private void removePicture() {
		if (pictureView == null) {
			return;
		}
		transaction.attachedPicture = null;
		transaction.blobKey = null;
		pictureView.setImageBitmap(null);
		pictureView.setTag(R.id.attached_picture, null);
		pictureDescView.setText(R.string.new_picture);
	}

	protected void setDateTime(long date) {
		Date d = new Date(date);
		dateTime.setTime(d);
		dateText.setText(df.format(d));
		timeText.setText(tf.format(d));
	}

	protected abstract void editTransaction(Transaction transaction);

	protected void commonEditTransaction(Transaction transaction) {
		selectStatus(transaction.status);
		categorySelector.selectCategory(transaction.categoryId, false);
		projectSelector.selectEntity(transaction.projectId);
		locationSelector.selectEntity(transaction.locationId);
		setDateTime(transaction.dateTime);
		if (isShowNote) {
			noteText.setText(transaction.note);
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

		if (transaction.isCreatedFromTemlate() && isOpenCalculatorForTemplates) {
			rateView.openFromAmountCalculator();
		}
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
		transaction.projectId = projectSelector.getSelectedEntityId();
		transaction.locationId = locationSelector.getSelectedEntityId();
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
			categorySelector.selectCategory(p.lastCategoryId);
		}
	}

	@Override
	protected void onDestroy() {
		if (payeeSelector != null) payeeSelector.onDestroy();
		if (projectSelector != null) projectSelector.onDestroy();
		if (locationSelector != null) locationSelector.onDestroy();
		if (categorySelector != null) categorySelector.onDestroy();
		super.onDestroy();
	}
}
