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
package tw.tib.financisto.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseHelper.AccountColumns;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.utils.MyPreferences;

import static tw.tib.financisto.activity.CategorySelector.SelectorType.TRANSFER;

public class TransferActivity extends AbstractTransactionActivity {

	public static final String AMOUNT_EXTRA = "amount";

	private TextView accountFromText;
	private TextView accountFromBalanceText;
	private TextView accountFromLimitText;

	private TextView accountToText;
	private TextView accountToBalanceText;
	private TextView accountToLimitText;

	private long selectedAccountFromId = -1;
	private long selectedAccountToId = -1;

	private boolean isShowCategoryInTransfer;

	public TransferActivity() {
	}

	@Override
	protected void internalOnCreate() {
		super.internalOnCreate();
		if (transaction.isTemplateLike()) {
			setTitle(transaction.isTemplate() ? R.string.transfer_template : R.string.transfer_schedule);
			if (transaction.isTemplate()) {
				dateText.setEnabled(false);
				timeText.setEnabled(false);
			}
		}
	}

	@Override
	protected  void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(AMOUNT_EXTRA)) {
				long amount = intent.getLongExtra(AMOUNT_EXTRA, 0);
				rateView.setFromAmount(amount);
			}
		}
	}

	protected void fetchCategories() {
		categorySelector.fetchCategories(false);
		categorySelector.doNotShowSplitCategory();
	}

	protected int getLayoutId() {
		return MyPreferences.isUseFixedLayout(this) ? R.layout.transfer_fixed : R.layout.transfer_free;
	}

	@Override
	protected void createListNodes(LinearLayout layout) {
		if (isShowAccountBalanceOnSelector) {
			accountFromText = x.addListNodeAccount(layout, R.id.account_from, R.string.account_from, R.string.select_account);
			View v = ((View) accountFromText.getTag());
			accountFromBalanceText = v.findViewById(R.id.balance);
			accountFromBalanceText.setVisibility(View.INVISIBLE);
			accountFromLimitText = v.findViewById(R.id.limit);
			accountFromLimitText.setVisibility(View.GONE);

			accountToText = x.addListNodeAccount(layout, R.id.account_to, R.string.account_to, R.string.select_account);
			v = ((View) accountToText.getTag());
			accountToBalanceText = v.findViewById(R.id.balance);
			accountToBalanceText.setVisibility(View.INVISIBLE);
			accountToLimitText = v.findViewById(R.id.limit);
			accountToLimitText.setVisibility(View.GONE);
		}
		else {
			accountFromText = x.addListNode(layout, R.id.account_from, R.string.account_from, R.string.select_account);
			accountToText = x.addListNode(layout, R.id.account_to, R.string.account_to, R.string.select_account);
		}
		// payee
		isShowPayee = MyPreferences.isShowPayeeInTransfers(this);
		if (isShowPayee) {
			createPayeeNode(layout);
		}
		// category
		isShowCategoryInTransfer = MyPreferences.isShowCategoryInTransferScreen(this);
		if (isShowCategoryInTransfer) {
			categorySelector.createNode(layout, TRANSFER);
		} else {
			categorySelector.createDummyNode();
		}
		// amounts
		rateView.createTransferUI();
	}

	@Override
	protected void editTransaction(Transaction transaction) {
		if (transaction.fromAccountId > 0) {
			Account fromAccount = db.getAccount(transaction.fromAccountId);
			selectAccount(fromAccount, accountFromText, accountFromBalanceText, accountFromLimitText, false);
			rateView.selectCurrencyFrom(fromAccount.currency);
			rateView.setFromAmount(transaction.fromAmount);
			selectedAccountFromId = transaction.fromAccountId;
		}
		commonEditTransaction(transaction);
		if (transaction.toAccountId > 0) {
			Account toAccount = db.getAccount(transaction.toAccountId);
			selectAccount(toAccount, accountToText, accountToBalanceText, accountToLimitText, false);
			rateView.selectCurrencyTo(toAccount.currency);
			rateView.setToAmount(transaction.toAmount);
			selectedAccountToId = transaction.toAccountId;
		}
		selectPayee(transaction.payeeId);
	}

	@Override
	protected boolean onOKClicked() {
		if (selectedAccountFromId == -1) {
			Toast.makeText(this, R.string.select_from_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (selectedAccountToId == -1) {
			Toast.makeText(this, R.string.select_to_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (selectedAccountFromId == selectedAccountToId) {
			Toast.makeText(this, R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (checkSelectedEntities()) {
			updateTransferFromUI();
			return true;
		}
		return false;
	}

	private void updateTransferFromUI() {
		updateTransactionFromUI(transaction);
		transaction.fromAccountId = selectedAccountFromId;
		transaction.toAccountId = selectedAccountToId;
		transaction.fromAmount = rateView.getFromAmount();
		transaction.toAmount = rateView.getToAmount();
	}

	@Override
	protected void onClick(View v, int id) {
		super.onClick(v, id);
		if (id == R.id.account_from) {
			x.select(this, R.id.account_from, R.string.account, accountCursor, accountAdapter,
					AccountColumns.ID, selectedAccountFromId);
		} else if (id == R.id.account_to) {
			x.select(this, R.id.account_to, R.string.account, accountCursor, accountAdapter,
					AccountColumns.ID, selectedAccountToId);
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		super.onSelectedPos(id, selectedPos);
		if (id == R.id.payee) {
			if (isShowPayee && isRememberLastCategory) {
				selectLastCategoryForPayee(payeeSelector.getSelectedEntityId());
			}
		}
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		super.onSelectedId(id, selectedId);
		if (id == R.id.account_from) {
			selectFromAccount(selectedId);
		} else if (id == R.id.account_to) {
			selectToAccount(selectedId);
		} else if (id == R.id.payee) {
			if (isRememberLastCategory) {
				selectLastCategoryForPayee(selectedId);
			}
		}
	}

	private void selectFromAccount(long selectedId) {
		selectAccount(selectedId, true);
	}

	private void selectToAccount(long selectedId) {
		Account account = db.getAccount(selectedId);
		if (account != null) {
			selectAccount(account, accountToText, accountToBalanceText, accountToLimitText, false);
			selectedAccountToId = selectedId;
			rateView.selectCurrencyTo(account.currency);
		}
	}

	@Override
	protected Account selectAccount(long accountId, boolean selectLast) {
		Account account = db.getAccount(accountId);
		if (account != null) {
			selectAccount(account, accountFromText, accountFromBalanceText, accountFromLimitText, selectLast);
			selectedAccountFromId = accountId;
			rateView.selectCurrencyFrom(account.currency);
		}
		return account;
	}

	protected void selectAccount(Account account, TextView accountText, TextView accountBalanceText, TextView accountLimitText, boolean selectLast) {
		u.setAccountTitleBalance(account, accountText, accountBalanceText, accountLimitText);
		if (selectLast) {
			if (isRememberLastAccount) {
				selectToAccount(account.lastAccountId);
			}
			if (!isShowPayee && isShowCategoryInTransfer && isRememberLastCategory) {
				categorySelector.selectCategory(account.lastCategoryId);
			}
		}
	}

}
