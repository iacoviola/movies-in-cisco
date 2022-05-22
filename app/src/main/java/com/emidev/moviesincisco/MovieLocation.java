package com.emidev.moviesincisco;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.ArrayList;

@Entity(tableName = "movie_location")
public class MovieLocation implements ClusterItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "year")
    private int releaseYear;
    @ColumnInfo(name = "location")
    private String location;
    private String funFact;
    @ColumnInfo(name = "production")
    private String productionCompany;
    @ColumnInfo(name = "distributor")
    private String distributor;
    @ColumnInfo(name = "director")
    private String director;
    @ColumnInfo(name = "writer")
    private String writer;
    @ColumnInfo(name = "latitude")
    private double latitude;
    @ColumnInfo(name = "longitude")
    private double longitude;
    @ColumnInfo(name = "poster")
    private String poster;
    @ColumnInfo(name = "main_actor")
    private String mainActor;
    private boolean isOverLimit;

    public MovieLocation() {
        this.poster = null;
    }

    //Copy constructor
    @Ignore
    public MovieLocation(MovieLocation movieLocation) {
        this.title = movieLocation.getTitle();
        this.releaseYear = movieLocation.getReleaseYear();
        this.location = movieLocation.getLocation();
        this.funFact = movieLocation.getFunFact();
        this.productionCompany = movieLocation.getProductionCompany();
        this.distributor = movieLocation.getDistributor();
        this.director = movieLocation.getDirector();
        this.writer = movieLocation.getWriter();
        this.latitude = movieLocation.getLatitude();
        this.longitude = movieLocation.getLongitude();
        this.isOverLimit = movieLocation.isOverLimit();
        this.mainActor = movieLocation.getMainActor();
        this.poster = movieLocation.getPoster();
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return title;
    }

    public int getId() {
        return id;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public String getLocation() {
        return location;
    }

    public String getFunFact() {
        return funFact;
    }

    public String getProductionCompany() {
        return productionCompany;
    }

    public String getDistributor() {
        return distributor;
    }

    public String getDirector() {
        return director;
    }

    public String getWriter() {
        return writer;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPoster() {
        return poster;
    }

    public String getMainActor() {
        return mainActor;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setFunFact(String funFact) {
        this.funFact = funFact;
    }

    public void setProductionCompany(String productionCompany) {
        this.productionCompany = productionCompany;
    }

    public void setDistributor(String distributor) {
        this.distributor = distributor;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public void setMainActor(String mainActor) {
        this.mainActor = mainActor;
    }

    public boolean isOverLimit() {
        return isOverLimit;
    }

    public void setOverLimit(boolean overLimit) {
        isOverLimit = overLimit;
    }

    @NonNull
    @Override
    public String toString() {
        return "MovieLocation{" +
                "title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
