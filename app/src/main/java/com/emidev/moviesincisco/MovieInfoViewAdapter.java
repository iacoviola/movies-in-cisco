package com.emidev.moviesincisco;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.room.Room;

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
    Context context;
    MoviesClusterRenderer clusterRenderer;

    public MovieInfoViewAdapter(LayoutInflater inflater, Context context, MoviesClusterRenderer renderer) {
        this.mInflater = inflater;
        this.context = context;
        this.clusterRenderer = renderer;

    }

    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        final View popup = mInflater.inflate(R.layout.info_window_layout, null);
        MovieLocation item = clusterRenderer.getClusterItem(marker);
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
        ImageView poster = popup.findViewById(R.id.info_window_image);
        path = item.getPoster();
        if(path != null) {
            Log.d("Title", item.getTitle());
            Log.d("Path", "https://image.tmdb.org/t/p/original" + path);
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
        ((TextView) popup.findViewById(R.id.title)).setText(item.getTitle());
        return popup;
    }

    @Override
    public View getInfoContents(@NonNull Marker marker) {
        return null;
    }
}