package ru.orangesoftware.financisto.activity;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ImageButton;
import android.widget.ListView;
import com.jayway.android.robotium.solo.Solo;
import ru.orangesoftware.financisto.R;

public class BasicRegressionTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;

    public BasicRegressionTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void test_should_add_new_currency() throws InterruptedException {
        solo.sleep(5000);

        bringUpCurrenciesList();
        checkCountInTheList(0);

        addNewCurrencyFromThePreDefinedList();
        checkCountInTheList(1);
    }

    private void bringUpCurrenciesList() {
        solo.sendKey(Solo.MENU);
        solo.clickOnText("Entities");
        solo.clickOnText("Currencies");
        solo.sleep(1000);
    }

    private void addNewCurrencyFromThePreDefinedList() {
        ImageButton addButton = (ImageButton) solo.getView(R.id.bAdd);
        solo.clickOnView(addButton);
        solo.clickOnText("ALL");
        solo.clickOnButton("OK");
        solo.sleep(1000);
    }

    private void checkCountInTheList(int count) {
        ListView currenciesListView = solo.getCurrentListViews().get(0);
        assertEquals(count, currenciesListView.getAdapter().getCount());
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }
}
