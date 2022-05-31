package com.emidev.moviesincisco;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.maps.android.ui.IconGenerator;

import java.util.Objects;

public class MoviesClusterRenderer extends DefaultClusterRenderer<MovieLocation> {
    Context context;

    private final IconGenerator iconGenerator;

    public MoviesClusterRenderer(Context context, GoogleMap map, ClusterManager<MovieLocation> clusterManager) {
        super(context, map, clusterManager);
        iconGenerator = new IconGenerator(context);
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

    @Override
    protected void onBeforeClusterRendered(Cluster<MovieLocation> cluster, MarkerOptions markerOptions) {
        iconGenerator.setBackground(ContextCompat.getDrawable(context, R.drawable.cluster_icon_small));
        //set icon number
        iconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon()));
        //markerOptions.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.cluster_icon), 100, 100, false)));
    }
}