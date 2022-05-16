package com.emidev.moviesincisco;

import android.os.Build;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.os.Handler;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;

public class Locator {

    private final String urlJSON = "https://data.sfgov.org/resource/yitu-d5am.json?$limit=3000&$where=locations%3C%3E%22%22&$order=title";

    private final String urlConverter = "https://maps.googleapis.com/maps/api/geocode/json?address=";

    private final String urlTMDBFile = "https://api.themoviedb.org/3/search/movie?query=";

    private final String urlTMDBtv = "https://api.themoviedb.org/3/search/tv?query=";

    private final String urlTMDBCredits = "https://api.themoviedb.org/3/movie/";

    private final Semaphore mutex = new Semaphore(1);

    public Locator(){ }

    public boolean checkCredits(MovieLocation movie, long id) {
        URL server;
        HttpsURLConnection service;
        int status;

        try {
            server = new URL(urlTMDBCredits + URLEncoder.encode(String.valueOf(id), "UTF-8") + "/credits?api_key" + BuildConfig.TMDBKEY);
            service = (HttpsURLConnection) server.openConnection();
            service.setRequestProperty("Host", "api.themoviedb.org");
            service.setRequestProperty("Accept", "application/json");
            service.setRequestProperty("Accept-Charset", "UTF-8");
            service.setRequestMethod("GET");
            service.setDoInput(true);
            service.connect();
            status = service.getResponseCode();
            if (status != 200) {
                return false;
            }

            return Parser.parseCredits(new InputStreamReader(service.getInputStream(), StandardCharsets.UTF_8), movie);

        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTMDBtv(MovieLocation movie) {
        URL server;
        HttpsURLConnection service;
        int status;

        try {
            server = new URL(urlTMDBtv + URLEncoder.encode(movie.getTitle(), "UTF-8") + "&api_key" + BuildConfig.TMDBKEY);
            service = (HttpsURLConnection) server.openConnection();
            service.setRequestProperty("Host", "api.themoviedb.org");
            service.setRequestProperty("Accept", "application/json");
            service.setRequestProperty("Accept-Charset", "UTF-8");
            service.setRequestMethod("GET");
            service.setDoInput(true);
            service.connect();
            status = service.getResponseCode();
            if (status != 200) {
                return null;
            }

            return Parser.parseTMDBtv(new InputStreamReader(service.getInputStream(), StandardCharsets.UTF_8), movie);

        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getTMDBfile(MovieLocation movie) {
        URL server;
        HttpsURLConnection service;
        int status;

        try {
            server = new URL(urlTMDBFile + URLEncoder.encode(movie.getTitle(), "UTF-8") + "&api_key" + BuildConfig.TMDBKEY);
            service = (HttpsURLConnection) server.openConnection();
            service.setRequestProperty("Host", "api.themoviedb.org");
            service.setRequestProperty("Accept", "application/json");
            service.setRequestProperty("Accept-Charset", "UTF-8");
            service.setRequestMethod("GET");
            service.setDoInput(true);
            service.connect();
            status = service.getResponseCode();
            if (status != 200) {
                return null;
            }

            return Parser.parseTMDBfile(new InputStreamReader(service.getInputStream(), StandardCharsets.UTF_8), movie);

        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getMoviesLocations(MovieLocationDAO query, ArrayList<MovieLocation> movies, int beginPos, int endPos, TextView count, TextView lost, ProgressBar progressBar) {
        URL server;
        HttpsURLConnection service;
        int status;

        for(; beginPos < endPos; beginPos++) {
            try {
                server = new URL(urlConverter + URLEncoder.encode(movies.get(beginPos).getLocation() + ", San Francisco, California, USA", "UTF-8") + BuildConfig.MAPSKEY);
                service = (HttpsURLConnection) server.openConnection();
                service.setRequestProperty("Host", "maps.googleapis.com");
                service.setRequestProperty("Accept", "application/json");
                service.setRequestProperty("Accept-Charset", "UTF-8");
                service.setRequestMethod("GET");
                service.setDoInput(true);
                service.connect();
                status = service.getResponseCode();
                if (status != 200) {
                    return;
                }

                Parser.parseMoviesLocations(query, new InputStreamReader(service.getInputStream(), StandardCharsets.UTF_8), movies.get(beginPos), lost);

                try {
                    mutex.acquire();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            int j = Integer.parseInt(count.getText().toString());
                            progressBar.setProgress(j + 1);
                            count.setText(String.valueOf(j + 1));
                        }
                    });
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                finally {
                    mutex.release();
                }


            } catch (IOException | JSONException | ParseException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public ArrayList<MovieLocation> getMoviesJSON( ) {
        URL server;
        HttpsURLConnection service;
        int status;

        try {
            server = new URL(urlJSON);
            service = (HttpsURLConnection) server.openConnection();
            service.setRequestProperty("Host", "data.sfgov.org");
            service.setRequestProperty("Accept", "application/json");
            service.setRequestProperty("Accept-Charset", "UTF-8");
            service.setRequestMethod("GET");
            service.setDoInput(true);
            service.connect();
            status = service.getResponseCode();
            if (status != 200) {
                return null;
            }

            return Parser.parseMovies(new InputStreamReader(service.getInputStream(), StandardCharsets.UTF_8));

        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}

