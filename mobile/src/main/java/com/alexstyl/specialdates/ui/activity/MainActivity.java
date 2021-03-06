package com.alexstyl.specialdates.ui.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alexstyl.specialdates.ExternalNavigator;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.analytics.Action;
import com.alexstyl.specialdates.analytics.Analytics;
import com.alexstyl.specialdates.analytics.AnalyticsProvider;
import com.alexstyl.specialdates.analytics.Screen;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.search.SearchHintCreator;
import com.alexstyl.specialdates.support.AskForSupport;
import com.alexstyl.specialdates.theming.ThemeMonitor;
import com.alexstyl.specialdates.theming.ThemingPreferences;
import com.alexstyl.specialdates.ui.ViewFader;
import com.alexstyl.specialdates.ui.base.ThemedActivity;
import com.alexstyl.specialdates.upcoming.DatePickerDialogFragment;
import com.alexstyl.specialdates.upcoming.view.ExposedSearchToolbar;
import com.alexstyl.specialdates.util.Notifier;
import com.novoda.notils.caster.Views;
import com.novoda.notils.meta.AndroidUtils;

import static android.view.View.OnClickListener;

/*
 * The activity was first launched with MainActivity being in package.ui.activity
  * For that reason, it needs to stay here so that we don't remove ourselves from the user's desktop
 */
public class MainActivity extends ThemedActivity implements DatePickerDialogFragment.OnDateSetListener {

    private Notifier notifier;
    private AskForSupport askForSupport;
    private ThemeMonitor themeMonitor;

    private MainNavigator navigator;
    private ExternalNavigator externalNavigator;
    private SearchTransitioner searchTransitioner;
    private Analytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        themeMonitor = ThemeMonitor.startMonitoring(ThemingPreferences.newInstance(this));
        analytics = AnalyticsProvider.getAnalytics(this);
        analytics.trackScreen(Screen.HOME);

        navigator = new MainNavigator(analytics, this);
        externalNavigator = new ExternalNavigator(this, analytics);

        ExposedSearchToolbar toolbar = Views.findById(this, R.id.memento_toolbar);
        toolbar.setOnClickListener(onToolbarClickListener);
        setSupportActionBar(toolbar);

        ViewGroup activityContent = Views.findById(this, R.id.main_content);
        searchTransitioner = new SearchTransitioner(this, navigator, activityContent, toolbar, new ViewFader());

        notifier = Notifier.newInstance(this);

        FloatingActionButton addBirthdayFAB = Views.findById(this, R.id.main_birthday_add_fab);
        addBirthdayFAB.setOnClickListener(startAddBirthdayOnClick);
        askForSupport = new AskForSupport(this);
        SearchHintCreator hintCreator = new SearchHintCreator(getResources(), NamedayPreferences.newInstance(this));
        setTitle(hintCreator.createHint());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (themeMonitor.hasThemeChanged()) {
            reapplyTheme();
        } else if (askForSupport.shouldAskForRating()) {
            askForSupport.askForRatingFromUser(this);
        }
        searchTransitioner.onActivityResumed();
        externalNavigator.connectTo(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        externalNavigator.disconnectTo(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mainactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_date:
                showSelectDateDialog();
                return true;
            case R.id.action_settings:
                navigator.toSettings();
                return true;
            case R.id.action_about:
                navigator.toAbout();
                return true;
            case R.id.action_donate:
                navigator.toDonate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSelectDateDialog() {
        analytics.trackAction(Action.SELECT_DATE);
        DatePickerDialogFragment
                .newInstance(Date.today())
                .show(getSupportFragmentManager(), "date_picker");
    }

    @Override
    public void onDateSelected(Date dateSelected) {
        navigator.toDateDetails(dateSelected);
    }

    @Override
    public boolean onSearchRequested() {
        searchTransitioner.transitionToSearch();
        return true;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        notifier.cancelAllEvents();
    }

    private final OnClickListener onToolbarClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AndroidUtils.toggleKeyboard(v.getContext());
            onSearchRequested();
        }

    };

    private final OnClickListener startAddBirthdayOnClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.toAddEvent();
        }
    };
}
