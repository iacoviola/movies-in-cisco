package com.emidev.moviesincisco;

import static java.lang.Math.abs;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.LinkedList;

public class MovieClusterManager<MovieLocation extends ClusterItem> extends ClusterManager<MovieLocation> {
    public MovieClusterManager(Context context, GoogleMap googleMap) {
        super(context, googleMap);
    }

    boolean itemsInSameLocation(Cluster<MovieLocation> cluster) {
        LinkedList<MovieLocation> items = new LinkedList<>(cluster.getItems());
        MovieLocation item = items.remove(0);

        double longitude = item.getPosition().longitude;
        double latitude = item.getPosition().latitude;

        for (MovieLocation t : items) {
            if (abs(longitude - t.getPosition().longitude) > 0.00001 && abs(latitude - t.getPosition().latitude) > 0.00001) {
                return false;
            }
        }

        return true;
    }
}
