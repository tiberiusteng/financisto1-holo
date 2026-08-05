package tw.tib.financisto.activity;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.AllOf.allOf;

import android.widget.ListView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tw.tib.financisto.R;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SmokeTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void SmokeTest() {
        // dismiss what's new
        onView(withText("OK")).perform(click());

        // change some prefs
        onView(withId(R.id.menu_tab)).perform(click());
        onView(withText(R.string.preferences)).perform(click());
        onView(withText(R.string.transaction_screen)).perform(click());
        onView(withText(R.string.split_entity_in_child)).perform(click());
        pressBack();
        pressBack();
        onView(withId(R.id.account_list_tab)).perform(click());

        // create new account
        onView(withId(R.id.bAdd)).perform(click());
        onView(withId(R.id.title)).perform(typeText("Cash"));
        onView(withId(R.id.currency_add)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("TWD (New Taiwan dollar)")))
                .inAdapterView(isAssignableFrom(ListView.class)).perform(click());
        onView(withText("OK")).perform(click());
        onView(withText("OK")).perform(click());

        // enter cash blotter
        onView(allOf(withText("Cash"), withId(R.id.center))).perform(click());
        onView(withText("Blotter")).perform(click());

        // create new transaction
        // Bookstore, Salary, +29500
        {
            onView(withId(R.id.bAdd)).perform(click());

            // -- create payee
            onView(withId(R.id.payee)).perform(click());
            onView(allOf(withId(R.id.autocomplete_filter), withParent(withId(R.id.payee)))).perform(typeText("Bookstore"));
            onView(withId(R.id.payee_create)).perform(click());
            onView(withText("YES")).perform(click());

            // -- create category
            onView(withId(R.id.category_add)).perform(click());
            onView(withId(R.id.primary)).perform(typeText("Salary"));
            onView(withId(R.id.toggle)).perform(click());
            onView(withId(R.id.bOK)).perform(click());

            // -- amount
            onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input_from)))).perform(typeText("29500"));

            // -- note
            onView(withId(R.id.note)).perform(typeText("Part-time"));

            onView(withId(R.id.bSave)).perform(click());
        }

        // create new transaction
        // City Bus, Transit, -15
        {
            onView(withId(R.id.bAdd)).perform(click());

            // -- create payee
            onView(withId(R.id.payee)).perform(click());
            onView(allOf(withId(R.id.autocomplete_filter), withParent(withId(R.id.payee)))).perform(typeText("City Bus"));
            onView(withId(R.id.payee_create)).perform(click());
            onView(withText("YES")).perform(click());

            // -- create category
            onView(withId(R.id.category_add)).perform(click());
            onView(withId(R.id.primary)).perform(typeText("Transit"));
            onView(withId(R.id.bOK)).perform(click());

            // -- amount
            onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input_from)))).perform(typeText("15"));

            // -- note
            onView(withId(R.id.note)).perform(typeText("Go home"));

            onView(withId(R.id.bSave)).perform(click());
        }

        // create new transaction
        // 7-11, [Split...]
        // Food,
        {
            onView(withId(R.id.bAdd)).perform(click());

            // -- create payee
            onView(withId(R.id.payee)).perform(click());
            onView(allOf(withId(R.id.autocomplete_filter), withParent(withId(R.id.payee)))).perform(typeText("7-11"));
            onView(withId(R.id.payee_create)).perform(click());
            onView(withText("YES")).perform(click());

            // -- split
            onView(allOf(withId(R.id.label), isDescendantOfA(withId(R.id.category)))).perform(click());
            onView(withText(R.string.split)).perform(click());

            // -- amount
            onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input_from)))).perform(typeText("94"));

            // split 1, Food, -39, Hot dog
            {
                onView(withId(R.id.add_split)).perform(click());
                onView(withId(R.id.category_add)).perform(click());
                onView(withId(R.id.primary)).perform(typeText("Food"));
                onView(withId(R.id.bOK)).perform(click());
                // -- amount
                onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input)))).perform(replaceText("45"));
                // -- note
                onView(withId(R.id.note)).perform(typeText("Big Bite"));
                onView(withId(R.id.bSave)).perform(click());
            }

            // split 2, Drink, -49, Tea Latte Royal Blend
            {
                onView(withId(R.id.add_split)).perform(click());
                onView(withId(R.id.category_add)).perform(click());
                onView(withText(R.string.select_category)).perform(click());
                onView(withText("Food")).perform(click());
                onView(withId(R.id.primary)).perform(typeText("Drink"));
                onView(withId(R.id.bOK)).perform(click());
                // -- amount
                onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input)))).perform(replaceText("49"));
                // -- note
                onView(withId(R.id.note)).perform(typeText("Tea Latte Royal Blend"));
                onView(withId(R.id.bSave)).perform(click());
            }

            onView(withId(R.id.bSave)).perform(click());
            pressBack();
        }

        // Budget
        {
            onView(withId(R.id.budgets_tab)).perform(click());
            onView(withId(R.id.bAdd)).perform(click());
            onView(withId(R.id.title)).perform(typeText("Food"));
            onView(withText(R.string.select_account)).perform(click());
            onView(withText("All TWD Accounts")).perform(click());
            onView(withText(R.string.no_categories)).perform(click());
            onView(withText("Food")).perform(click());
            onView(withText("OK")).perform(click());
            onView(withText(R.string.budget_type_saving)).perform(click());
            onView(allOf(withId(R.id.primary), isDescendantOfA(withId(R.id.amount_input)))).perform(scrollTo(), replaceText("15000"));
            onView(withId(R.id.bOK)).perform(click());
        }

        // Report
        {
            // Bar chart reports
            onView(withId(R.id.reports_tab)).perform(click());
            onView(withText(R.string.report_by_period)).perform(click());
            onView(withId(R.id.bFilter)).perform(click());
            onView(withId(R.id.bOK)).perform(click());
            onView(withId(R.id.bToggle)).perform(click()).perform(click()).perform(click()).perform(click());
            pressBack();
            onView(withText(R.string.report_by_category)).perform(click());
            pressBack();
            onView(withText(R.string.report_by_payee)).perform(click());
            pressBack();
            // 2D report
            onView(withText(R.string.report_by_category_by_period)).perform(click());
            // -- dismiss unconfigured notification
            onView(withText("OK")).perform(click());
            // -- perform initial configuration
            onView(withId(R.id.bt_preferences)).perform(click());
            onView(withText(R.string.report_reference_currency)).perform(click());
            onView(withText(startsWith("TWD"))).perform(click());
            onView(withText(R.string.report_reference_period)).perform(click());
            onView(withText("1 year")).perform(click());
            onView(withText(R.string.report_reference_month)).perform(click());
            onView(withText("Current month")).perform(click());
            onView(withText(R.string.report_aggregate_unit)).perform(click());
            onView(withText("Month")).perform(click());
            pressBack();
            onView(withId(R.id.bt_filter_next)).perform(click()).perform(click()).perform(click());
            pressBack();
        }
    }
}
