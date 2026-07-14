package tw.tib.financisto.activity;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import tw.tib.financisto.R;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.model.Payee;
import tw.tib.financisto.model.TransactionAttribute;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.widget.AmountInput;
import tw.tib.financisto.widget.AmountInput_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitTransactionActivity extends AbstractSplitActivity implements CategorySelector.CategorySelectorListener {

    private TextView amountTitle;
    private AmountInput amountInput;

    private PayeeSelector<SplitTransactionActivity> payeeSelector;
    private CategorySelector<SplitTransactionActivity> categorySelector;

    private boolean isTrackSplitEntityInChild;

    public SplitTransactionActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected void createUI(LinearLayout layout) {
        isTrackSplitEntityInChild = MyPreferences.isTrackSplitEntityInChild();

        if (MyPreferences.isShowPayee() && isTrackSplitEntityInChild) {
            payeeSelector = new PayeeSelector<>(this, db, x);
            if (split.payeeId != Payee.EMPTY.id) {
                payeeSelector.setIncludeEntityIds(split.payeeId);
            }
            payeeSelector.createNode(layout);
        }

        categorySelector = new CategorySelector<>(this, db, x);
        categorySelector.createNode(layout, CategorySelector.SelectorType.SPLIT);

        amountInput = AmountInput_.build(this);
        amountInput.setOwner(this);
        amountInput.setOnAmountChangedListener((oldAmount, newAmount) -> setUnsplitAmount(split.unsplitAmount - newAmount));
        View v = x.addEditNode(layout, R.string.amount, amountInput);
        amountTitle = v.findViewById(R.id.label);
        categorySelector.createAttributesLayout(layout);
    }

    @Override
    protected void updateUIforPreventEditing() {
        boolean enabled = !isPreventEditing();
        if (payeeSelector != null) payeeSelector.setEnabled(enabled);
        categorySelector.setEnabled(enabled);
        amountInput.setEnabled(enabled);
    }

    @Override
    protected void fetchData() {
        if (payeeSelector != null) payeeSelector.fetchEntities();
        categorySelector.setListener(this);
        categorySelector.doNotShowSplitCategory();
        categorySelector.fetchCategories(false);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        if (payeeSelector != null) payeeSelector.selectEntity(split.payeeId);
        categorySelector.selectCategory(split.categoryId);
        setAmount(split.fromAmount);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        if (payeeSelector != null) {
            payeeSelector.autoCreateNewEntityFromSearch();
            split.payeeId = payeeSelector.getSelectedEntityId();
        }
        split.fromAmount = amountInput.getAmount();
        split.categoryAttributes = getAttributes();
        return true;
    }

    private Map<Long, String> getAttributes() {
        List<TransactionAttribute> attributeList = categorySelector.getAttributes();
        Map<Long, String> attributes = new HashMap<>();
        for (TransactionAttribute ta : attributeList) {
            attributes.put(ta.attributeId, ta.value);
        }
        return attributes;
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        if (category.isIncome()) {
            amountInput.setIncome();
        } else {
            amountInput.setExpense();
        }
        split.categoryId = category.id;
        categorySelector.addAttributes(split);
    }

    private void setAmount(long amount) {
        amountInput.setAmount(amount);
        Currency c = getCurrency();
        amountInput.setCurrency(c);
        amountTitle.setText(getString(R.string.amount)+" ("+c.name+")");
    }

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        categorySelector.onClick(id);
        if (payeeSelector != null) payeeSelector.onClick(id);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        categorySelector.onSelectedId(id, selectedId);
        if (payeeSelector != null) payeeSelector.onSelectedId(id, selectedId);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        super.onSelectedPos(id, selectedPos);
        if (payeeSelector != null) payeeSelector.onSelectedPos(id, selectedPos);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        if (categorySelector != null) categorySelector.onDestroy();
        super.onDestroy();
    }
}
