package ru.gdgkazan.simpleweather.screen.weatherlist;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;
import ru.gdgkazan.simpleweather.screen.general.SimpleDividerItemDecoration;
import ru.gdgkazan.simpleweather.screen.weather.WeatherActivity;
import ru.gdgkazan.simpleweather.screen.weather.WeatherLoader;

/**
 * @author Artur Vasilov
 */
public class WeatherListActivity extends AppCompatActivity implements CitiesAdapter.OnItemClick, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty)
    View mEmptyView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;


    private CitiesAdapter mAdapter;

    private LoadingView mLoadingView;

    private List<City> initialCites;
    private ArrayList<City> loadedCites = new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        initialCites = getInitialCities();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, false));
        mAdapter = new CitiesAdapter(initialCites, this);
        mRecyclerView.setAdapter(mAdapter);
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED,Color.GREEN,Color.CYAN,Color.BLUE);
        load(initialCites, false);
        /*
         * TODO : task
         *
         * 1) Load all cities forecast using one or multiple loaders
         * 2) Try to run these requests as most parallel as possible
         * or better do as less requests as possible
         * 3) Show loading indicator during loading process
         * 4) Allow to update forecasts with SwipeRefreshLayout
         * 5) Handle configuration changes
         *
         * Note that for the start point you only have cities names, not ids,
         * so you can't load multiple cities in one request.
         *
         * But you should think how to manage this case. I suggest you to start from reading docs mindfully.
         */


    }

    private void load(List<City> cities, boolean restart) {
        LoaderManager loaderNanager = getSupportLoaderManager();
        for (int i = 0; i < cities.size(); i++) {
            loadWeather(loaderNanager,restart, cities.get(i), i + 1);
        }
    }

    private void loadWeather(LoaderManager loaderNanager,boolean restart, City city, int id) {
        mLoadingView.showLoadingIndicator();
        LoaderManager.LoaderCallbacks<City> callbacks = new WeatherCallbacks(city);
        if (restart) {
            loaderNanager.restartLoader(id, Bundle.EMPTY, callbacks);
        } else {
            loaderNanager.initLoader(id, Bundle.EMPTY, callbacks);
        }
    }

    @Override
    public void onRefresh() {
        load(initialCites,true);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private class WeatherCallbacks implements LoaderManager.LoaderCallbacks<City> {
        private City city;
        private String cityName;

        private WeatherCallbacks(City city) {
            this.city = city;
            this.cityName = city.getName();
        }

        @Override
        public Loader<City> onCreateLoader(int id, Bundle args) {
            if (id <= initialCites.size()) {
                return new WeatherLoader(WeatherListActivity.this, cityName);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<City> loader, City city) {
            showWeather(city);
        }

        @Override
        public void onLoaderReset(Loader<City> loader) {
            // Do nothing
        }
    }

    private void showWeather(@Nullable City city) {

        if (city == null || city.getMain() == null || city.getWeather() == null
                || city.getWind() == null) {
            showError();
            return;
        }
        loadedCites.add(city);
        if(loadedCites.size()>=initialCites.size()){
            mLoadingView.hideLoadingIndicator();
            sortAllCities(loadedCites);
            mAdapter.changeDataSet(loadedCites);
            loadedCites.clear();
        }

    }

    private void showError() {
        mLoadingView.hideLoadingIndicator();
        Snackbar snackbar = Snackbar.make(mRecyclerView, "Error loading weather",Snackbar.LENGTH_LONG)
                .setAction("Retry",v -> load(initialCites,true));
        snackbar.show();
    }

    private void sortAllCities(List<City> cities) {
        Collections.sort(cities, (o1, o2) -> o1.getName().compareTo(o2.getName()));
    }

    @Override
    public void onItemClick(@NonNull City city) {
        startActivity(WeatherActivity.makeIntent(this, city.getName()));
    }

    @NonNull
    private List<City> getInitialCities() {
        List<City> cities = new ArrayList<>();
        String[] initialCities = getResources().getStringArray(R.array.initial_cities);
        for (String city : initialCities) {
            cities.add(new City(city));
        }
        return cities;
    }
}
