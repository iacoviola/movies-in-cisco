package com.emidev.moviesincisco;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Map renderer
    private GoogleMap mMap;

    //Activity binding is useless here
    //private ActivityMapsBinding binding;

    //Allows to make network requests, must be wrapped in a thread
    private final Locator locator = new Locator();
    //List of movies
    private ArrayList<MovieLocation> movies;
    //Handles the marker's clustering
    private MovieClusterManager<MovieLocation> clusterManager;

    //Saves the last zoom level of the map
    private double previousZoom = -1.0;
    //Checks if the app's logo is currently fading out
    private boolean isFading = false;
    //Checks if there are multiple movies in the same location
    private boolean multiple = false;
    //Counts the number of movies in the same location
    private int count = 1;
    //Saves the style of the map
    //true = dark
    //false = light
    private boolean mapStyle = false;

    private MoviesClusterRenderer renderer;

    private String path;

    private boolean isDismissed = false;

    PopupWindow popupWindow;

    //Set up map clusterer
    private void setUpClusterer() {
        clusterManager = new MovieClusterManager<>(this, mMap);
        clusterManager.setAnimation(false);
        //Point the map's listeners at the listeners implemented by the cluster manager.
        mMap.setOnCameraIdleListener(clusterManager);
        renderer = new MoviesClusterRenderer(this, mMap, clusterManager);
        clusterManager.setRenderer(renderer);

        addItems();
    }

    private void addItems() {
        //Add cluster items in close proximity
        for(int i = 0; i < movies.size(); i++) {
            MovieLocation current = movies.get(i);
            Log.d("Movies", current.getTitle());
            clusterManager.addItem(current);
        }
    }

    //Function to fade in the app's logo
    private void fadeIn(View view) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(1000)
                .setListener(null);
    }

    //Function to fade out the app's logo
    private void fadeOut(View view) {
        isFading = true;
        view.setAlpha(1f);
        view.animate()
                .alpha(0f)
                .setDuration(1000)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                        isFading = false;
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Set the activity to full screen and makes the status bar transparent
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        //Initializes the local database
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "movies_in_cisco").build();
        //Initializes the Data Access Object to access the database
        MovieLocationDAO query = db.movieLocationDAO();

        //Shared preferences to check if the user has already loaded everything from the web-service
        SharedPreferences sharedPreferences = getSharedPreferences("movies_in_cisco", Context.MODE_PRIVATE);

        //Loading screen
        RelativeLayout loadingScreen = findViewById(R.id.loadingRelativeLayout);

        //Check if the user has already loaded everything from the web-service
        boolean initDB = sharedPreferences.getBoolean("initDB", false);
        //If the user still has to load everything from the web-service then show the loading screen
        //and load everything from the web-service
        if(!initDB) {
            //Passed as parameters to the locator to show progression on the loading screen
            TextView count = findViewById(R.id.loadedTextView);
            TextView total = findViewById(R.id.totalTextView);
            ProgressBar progressBar = findViewById(R.id.progressBar2);
            TextView lost = findViewById(R.id.lost);

            //New thread to load everything from the web-service
            new Thread(new Runnable() {
                @Override
                public void run() {
                    movies = locator.getMoviesJSON();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Update the loading screen
                            //with the number of movies to load
                            total.setText(String.valueOf(movies.size()));
                            progressBar.setMax(movies.size());
                        }
                    });
                    //Creating a new thread pool to speed up the loading process
                    //Not many threads are needed because the web-service doesn't
                    //Allow to convert more than 50 addresses per second
                    ExecutorService pool = Executors.newFixedThreadPool(100);

                    //A thread every 750 movies
                    final int MAX_REQUESTS_PER_THREAD = 750;

                    //Future list to store the results of the threads
                    //And to check if all the threads have finished
                    ArrayList<Future<?>> futures = new ArrayList<>();

                    for (int i = 0; i < movies.size(); i += MAX_REQUESTS_PER_THREAD) {
                        final int finalI = i;
                        //Create a new thread in the thread pool
                        //And add it to the Future list
                        futures.add(pool.submit(new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //For every movie in the list
                                //Get the address of the movie
                                locator.getMoviesLocations(query, movies, finalI,
                                        (finalI + MAX_REQUESTS_PER_THREAD > movies.size()) ? movies.size(): finalI + MAX_REQUESTS_PER_THREAD - 1,
                                        count, lost, progressBar);
                            }
                        })));
                    }

                    //Wait for the threads to finish
                    for (Future<?> future : futures) {
                        try {
                            future.get(300, TimeUnit.SECONDS);
                        } catch (Throwable cause) {
                            cause.printStackTrace();
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Initialize the map
                            loadingScreen.setVisibility(View.GONE);
                            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map);
                            Objects.requireNonNull(mapFragment).getMapAsync(MapsActivity.this);
                            //Now the app has loaded all the movies
                            //It can, from now on, be used with the
                            //local database, only to retrieve the movies, not the posters
                            sharedPreferences.edit().putBoolean("initDB", true).apply();
                        }
                    });
                }
            }).start();
        } else {
            //The app has already been initialized

            //Hide the loading screen
            loadingScreen.setVisibility(View.GONE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Get the movies from the database
                    movies = new ArrayList<>(query.getAll());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Initialize the map
                            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map);
                            Objects.requireNonNull(mapFragment).getMapAsync(MapsActivity.this);
                        }
                    });
                }
            }).start();
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //Changing the map style to remove useless elements
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.light_style));

        ImageButton switchMap = findViewById(R.id.switchButton);
        switchMap.setColorFilter(getResources().getColor(R.color.holo_red_light));
        //Creating bounds for the map, to lock the camera
        //Over the bay area
        LatLngBounds sanFrancisco = new LatLngBounds(new LatLng(37.1398299, -122.873825), new LatLng(38.4298239, -121.98178));
        //Move the camera to the bounds
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(sanFrancisco, 0));

        //Set the status bar color to white
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        //On map loaded lock the camera and set the minimum zoom
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.setLatLngBoundsForCameraTarget(sanFrancisco);
                mMap.setMinZoomPreference(9);
            }
        });

        //Setting up the map clusterer
        setUpClusterer();

        //Setting a custom adapter to show a window when a marker is clicked for every marker
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new MovieInfoViewAdapter(LayoutInflater.from(this), this, renderer));

        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MovieLocation>() {
            @Override
            public boolean onClusterItemClick(MovieLocation item) {
                Marker marker = renderer.getMarker(item);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (item.getPoster() == null) {
                            path = locator.getTMDBfile(item);
                            item.setPoster(path);
                        } else {
                            path = item.getPoster();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                marker.showInfoWindow();
                            }
                        });
                    }
                }).start();
                return true;
            }
        });

        //When a cluster (not a marker) is clicked, show the info window of the cluster
        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MovieLocation>() {
            @Override
            public boolean onClusterClick(final Cluster<MovieLocation> cluster) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isDismissed = false;
                        //Declaring variables
                        ArrayList<MovieLocation> moviesInCluster;
                        LayoutInflater inflater;
                        View popupView;
                        GridLayout grid;
                        //Checks if a movie is multiple times in the cluster
                        boolean alreadyAdded = false;
                        //Check if the cluster is composed of markers in the same location
                        //If it is it means that we need to show the info window for the cluster
                        //And render all the movies in the cluster
                        if (clusterManager.itemsInSameLocation(cluster)) {
                            //Putting all the movies in the cluster in a list
                            moviesInCluster = new ArrayList<>(cluster.getItems());
                            //Inflate the layout of the popup window
                            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                            popupView = inflater.inflate(R.layout.cluster_info_window_layout, null);
                            //Create the popup window
                            popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, 375 * 2, true);

                            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                @Override
                                public void onDismiss() {
                                    isDismissed = true;
                                }
                            });

                            //Getting the grid layout from the popup window layout
                            grid = popupView.findViewById(R.id.cluster_info_window_layout);
                            //Column count to 4
                            grid.setColumnCount(4);
                            //Column rows to fit the number of movies in the cluster
                            grid.setRowCount(moviesInCluster.size() / 4 + 1);
                            //For each movie in the cluster
                            for (int i = 0; i < moviesInCluster.size(); i++) {
                                //Check if the movie was already added to the grid
                                for(int j = 0; j < i; j++) {
                                    if (Objects.requireNonNull(moviesInCluster.get(i).getTitle()).equals(moviesInCluster.get(j).getTitle())) {
                                        alreadyAdded = true;
                                        break;
                                    }
                                }
                                //If the movie was not added already to the grid
                                //We can check if it is in the cluster more than once
                                //And how many times.
                                //Then we can operate to add it to the grid
                                if(!alreadyAdded) {
                                    //Declaring variables for local use
                                    RelativeLayout movieContainer;
                                    RelativeLayout.LayoutParams movieContainerParams;
                                    TextView movieTitle;
                                    LinearLayout.LayoutParams movieTitleParams;
                                    ImageView moviePoster;
                                    String path;

                                    for(int j = i + 1; j < moviesInCluster.size(); j++) {
                                        if (Objects.requireNonNull(moviesInCluster.get(i).getTitle()).equals(moviesInCluster.get(j).getTitle())) {
                                            multiple = true;
                                            count++;
                                        }
                                    }

                                    //RelativeLayout to contain the movie poster and title
                                    movieContainer = new RelativeLayout(getApplicationContext());
                                    //Setting the layout parameters for the movie container
                                    movieContainerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    movieContainer.setLayoutParams(new LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT));
                                    movieContainer.setGravity(Gravity.CENTER);

                                    //TextView to contain the movie title
                                    movieTitle = new TextView(getApplicationContext());
                                    //Setting the layout parameters for the movie title
                                    movieTitleParams = new LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    movieTitleParams.setMargins(0,375,0,0);
                                    movieTitle.setLayoutParams(movieTitleParams);
                                    movieTitle.setText(moviesInCluster.get(i).getTitle());
                                    movieTitle.setTypeface(null, Typeface.BOLD);
                                    movieTitle.setGravity(Gravity.CENTER);
                                    movieTitle.setSingleLine(false);
                                    movieTitle.setPadding(3, 3, 3, 3);
                                    movieTitle.setTextColor(Color.BLACK);

                                    //Creating the movie poster to add to the movie container
                                    moviePoster = new ImageView(getApplicationContext());
                                    moviePoster.setLayoutParams(new LinearLayout.LayoutParams(250, 375));
                                    moviePoster.setPadding(5, 5, 5, 5);
                                    moviePoster.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                    moviePoster.setAdjustViewBounds(true);

                                    //Path of the poster image in the TMDB database
                                    path = locator.getTMDBfile(moviesInCluster.get(i));

                                    //Adding the container to the popup window
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            //Using Glide to load the poster image
                                            Glide.with(getApplicationContext())
                                                    .load("https://image.tmdb.org/t/p/original" + path)
                                                    .placeholder(R.drawable.ic_baseline_local_movies_24)
                                                    .transition(withCrossFade())
                                                    .into(moviePoster);
                                            //Adding the poster to the movie container
                                            movieContainer.addView(moviePoster);
                                            //Adding the movie title to the movie container
                                            movieContainer.addView(movieTitle);
                                            //If there are multiple instances of the same movie, add the number of instances to the container
                                            if(multiple){
                                                //Declaring variables for local use
                                                TextView times;
                                                LinearLayout.LayoutParams timesParams;
                                                times = new TextView(getApplicationContext());

                                                //Setting the layout parameters for the number of instances
                                                timesParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                timesParams.setMargins(10,290,0,0);
                                                times.setPadding(10, 5, 10, 5);
                                                times.setLayoutParams(timesParams);
                                                times.setText(String.format(getResources().getString(R.string.times), count));
                                                times.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.background_rounded));
                                                times.setTypeface(null, Typeface.BOLD);
                                                times.setGravity(Gravity.END);
                                                times.setTranslationZ(45);
                                                times.setTextColor(Color.BLACK);

                                                //Adding rule to align the number inside the container
                                                movieContainerParams.addRule(RelativeLayout.ALIGN_TOP, times.getId());
                                                //Adding the number of instances to the container
                                                if(isDismissed) {
                                                    popupWindow.dismiss();
                                                    popupWindow = null;
                                                    return;
                                                }
                                                movieContainer.addView(times);
                                                multiple = false;
                                                count = 1;
                                            }
                                            //Adding rule to align the title inside the container
                                            movieContainerParams.addRule(RelativeLayout.ALIGN_TOP, movieTitle.getId());
                                            //Adding the container to the layout
                                            grid.addView(movieContainer);
                                            //Showing the popup window on the center of the screen
                                            if(isDismissed){
                                                popupWindow.dismiss();
                                                popupWindow = null;
                                                return;
                                            }
                                            if(popupWindow != null) {
                                                popupWindow.showAtLocation(new View(getApplicationContext()), Gravity.CENTER, 0, 0);
                                            }
                                        }
                                    });
                                }
                                if(isDismissed) {
                                    if(popupWindow != null) {
                                        popupWindow.dismiss();
                                        popupWindow = null;
                                    }
                                    isDismissed = false;
                                    return;
                                }
                                alreadyAdded = false;
                            }
                        } else {
                            //If the cluster does not contain just movies in the same location
                            //Then we zoom in on the cluster
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                                    cluster.getPosition(), (float) Math.floor(mMap
                                                            .getCameraPosition().zoom + 1)), 300,
                                            null);
                                }
                            });
                        }
                    }
                }).start();
                return true;
            }
        });

        //Handling the app's logo
        ImageView cisco = findViewById(R.id.ciscoLogo);

        //When the camera is zoomed in, the logo gets hidden
        //When the camera is zoomed out, the logo gets shown
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                CameraPosition cameraPosition = mMap.getCameraPosition();
                if(previousZoom != cameraPosition.zoom)
                {
                    if(cameraPosition.zoom >= 9 && cameraPosition.zoom <= 11){
                        if(!cisco.isShown())
                            fadeIn(cisco);
                    } else {
                        if(cisco.isShown() && !isFading)
                            fadeOut(cisco);
                    }
                }
                previousZoom = cameraPosition.zoom;
            }
        });

        switchMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mapStyle) {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.dark_style));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        getWindow().getDecorView().setSystemUiVisibility(0);
                    }
                    RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setDuration(500);
                    rotate.setFillAfter(true);
                    switchMap.startAnimation(rotate);
                    switchMap.setColorFilter(getResources().getColor(R.color.holo_red_dark));
                    mapStyle = true;
                }
                else {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.light_style));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }
                    RotateAnimation rotate = new RotateAnimation(0, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setDuration(500);
                    rotate.setFillAfter(true);
                    switchMap.startAnimation(rotate);
                    switchMap.setColorFilter(getResources().getColor(R.color.holo_red_light));
                    mapStyle = false;
                }
            }
        });
    }
}