package com.emidev.moviesincisco;

import android.os.Looper;
import android.util.Log;

import android.os.Handler;
import android.widget.TextView;

import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Parser {

    private final static Semaphore mutex = new Semaphore(1);
    private final static Locator locator = new Locator();

    private static String trimDash(String str) {
        if(str.charAt(0) == '-') {
            str = str.substring(1);
        }
        if(str.charAt(str.length() - 1) == '-') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public static boolean parseCredits(InputStreamReader file, MovieLocation movie) throws ParseException, JSONException, IOException {
        String path = null;

        JSONObject doc = (JSONObject) new JSONParser().parse(file);
        Log.d("doc", doc.toString());

        JSONArray results = (JSONArray) doc.get("crew");

        for(int i = 0; i < results.size(); i++) {
            JSONObject movieEntry = (JSONObject) results.get(i);
            String job = (String) movieEntry.get("job");
            if(job.equals("Director")) {
                String name = (String) movieEntry.get("name");
                if(name.equalsIgnoreCase(movie.getDirector())) {
                    return true;
                }
            }
        }

        results = (JSONArray) doc.get("cast");

        for(int i = 0; i < results.size(); i++) {
            JSONObject movieEntry = (JSONObject) results.get(i);
            String name = (String) movieEntry.get("name");
            if(name.equalsIgnoreCase(movie.getMainActor())) {
                return true;
            }
        }

        return false;
    }

    public static String parseTMDBtv(InputStreamReader file, MovieLocation movie) throws ParseException, JSONException, IOException {
        String path = null;

        JSONObject doc = (JSONObject) new JSONParser().parse(file);
        Log.d("doc", doc.toString());

        JSONArray results = (JSONArray) doc.get("results");

        for(int i = 0; i < results.size(); i++) {
            JSONObject movieEntry = (JSONObject) results.get(i);
            long id = (long) movieEntry.get("id");
            Log.d("id", Long.toString(id));
            Log.d("movie", movie.getTitle());
            Log.d("movie", movie.getMainActor());
            path = (String) movieEntry.get("poster_path");
            break;
        }
        return path;
    }

    public static String parseTMDBfile(InputStreamReader file, MovieLocation movie) throws ParseException, JSONException, IOException {
        String path = null;

        JSONObject doc = (JSONObject) new JSONParser().parse(file);
        Log.d("doc", doc.toString());

        JSONArray results = (JSONArray) doc.get("results");

        for(int i = 0; i < results.size(); i++) {
            JSONObject movieEntry = (JSONObject) results.get(i);
            long id = (long) movieEntry.get("id");
            if(locator.checkCredits(movie, id)){
                path = (String) movieEntry.get("poster_path");
                break;
            };
        }
        if(path == null) {
            path = locator.getTMDBtv(movie);
        }
        return path;
    }

    public static void parseMoviesLocations(MovieLocationDAO query, InputStreamReader file, MovieLocation movie, TextView lost) throws ParseException, JSONException, IOException {

        JSONObject doc = (JSONObject) new JSONParser().parse(file);
        Log.d("doc", doc.toString());
        String status = (String) doc.get("status");
        if(status.equals("OVER_QUERY_LIMIT")) {
            movie.setOverLimit(true);
            movie.setLatitude(0.0);
            movie.setLongitude(0.0);

            try {
                mutex.acquire();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Count", String.valueOf(lost.getText()));
                        int j = Integer.parseInt(lost.getText().toString());
                        lost.setText(String.valueOf(j + 1));
                    }
                });
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            finally {
                mutex.release();
            }
        } else {
            JSONArray results = (JSONArray) doc.get("results");
            if (results.size() > 0) {
                JSONObject address = (JSONObject) results.get(0);
                JSONObject geometry = (JSONObject) address.get("geometry");
                JSONObject location = (JSONObject) geometry.get("location");
                Log.d("Lat", String.valueOf(location.get("lat")));
                movie.setLatitude((Double) location.get("lat"));
                movie.setLongitude((Double) location.get("lng"));
                movie.setOverLimit(false);
                query.insert(movie);
            } else {
                movie.setLatitude(0.0);
                movie.setLongitude(0.0);
            }
        }
    }

    public static ArrayList<MovieLocation> parseMovies(InputStreamReader file) throws ParseException, JSONException, IOException {
        ArrayList<MovieLocation> movies = new ArrayList<MovieLocation>();

        JSONArray doc = (JSONArray) new JSONParser().parse(file);
        Log.d("doc", doc.toString());

        for (int i = 0; i < doc.size(); i++) {
            JSONObject movie = (JSONObject) doc.get(i);
            MovieLocation m = new MovieLocation();
            m.setTitle(trimDash(((String) movie.get("title")).split("Season")[0].trim()));

            if(movie.get("locations") != null) {
                Log.d("LocationOfJSONFile", (String) movie.get("locations"));
                m.setLocation((String) movie.get("locations"));
                Log.d("ReleaseYear", (String) movie.get("release_year"));
                m.setReleaseYear(Integer.parseInt(((String) movie.get("release_year"))));
                m.setProductionCompany((String) movie.get("production_company"));
                m.setDistributor((String) movie.get("distributor"));
                m.setDirector(((String) movie.get("director")).split("/")[0]);
                m.setWriter((String) movie.get("writer"));
                m.setMainActor((String) movie.get("actor_1"));
                movies.add(m);
            }
        }
        return movies;
    }
}
