package com.emidev.moviesincisco;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.Objects;

public class MoviesClusterRenderer extends DefaultClusterRenderer<MovieLocation> {
    Context context;

    public MoviesClusterRenderer(Context context, GoogleMap map, ClusterManager<MovieLocation> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
    }

    //Convert a vector to a bitmap
    private BitmapDescriptor BitmapFromVector(int vectorResId) {
        //Generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        //Set bounds to the vector drawable.
        Objects.requireNonNull(vectorDrawable).setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        //Creating a bitmap from the vector drawable.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        //Add the bitmap to the canvas.
        Canvas canvas = new Canvas(bitmap);
        // Draw the drawable onto the canvas.
        vectorDrawable.draw(canvas);
        //Return the bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<MovieLocation> cluster) {
        //start clustering if at least 2 items overlap
        return cluster.getSize() > 1;
    }

    @Override
    protected void onBeforeClusterItemRendered(MovieLocation item, MarkerOptions markerOptions) {
        markerOptions.icon(BitmapFromVector(R.drawable.ic_baseline_local_movies_24));
        markerOptions.snippet(item.getSnippet());
        markerOptions.title(item.getTitle());
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}