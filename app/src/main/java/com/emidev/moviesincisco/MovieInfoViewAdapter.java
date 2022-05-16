package com.emidev.moviesincisco;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;

public class MovieInfoViewAdapter implements GoogleMap.InfoWindowAdapter {
    private final LayoutInflater mInflater;
    private final Locator locator = new Locator();
    private String path = null;
    private final ClusterManager<MovieLocation> clusterManager;
    private MovieLocation clickedClusterItem;

    public MovieInfoViewAdapter(LayoutInflater inflater, ClusterManager<MovieLocation> click) {
        this.mInflater = inflater;
        this.clusterManager = click;

        clusterManager
                .setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MovieLocation>() {
                    @Override
                    public boolean onClusterItemClick(MovieLocation item) {
                        clickedClusterItem = item;
                        return false;
                    }
                });
    }

    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        final View popup = mInflater.inflate(R.layout.info_window_layout, null);
        ImageView poster = popup.findViewById(R.id.info_window_image);
        if(clickedClusterItem.getPoster() == null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    path = locator.getTMDBfile(clickedClusterItem);
                    clickedClusterItem.setPoster(path);
                }
            });

            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            path = clickedClusterItem.getPoster();
        }
        RequestListener<Drawable> req = new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                    marker.showInfoWindow();
                }
                return false;
            }
        };
        if(path != null) {
            Glide.with(popup)
                    .load("https://image.tmdb.org/t/p/original" + path)
                    .placeholder(R.drawable.ic_baseline_local_movies_24)
                    .listener(req)
                    .transition(withCrossFade())
                    .into(poster);

        } else {
            Glide.with(popup)
                    .load(R.drawable.ic_baseline_local_movies_24)
                    .placeholder(R.drawable.ic_baseline_local_movies_24)
                    .listener(req)
                    .transition(withCrossFade())
                    .into(poster);
        }
        ((TextView) popup.findViewById(R.id.title)).setText(clickedClusterItem.getTitle());
        return popup;
    }

    @Override
    public View getInfoContents(@NonNull Marker marker) {
        return null;
    }
}
