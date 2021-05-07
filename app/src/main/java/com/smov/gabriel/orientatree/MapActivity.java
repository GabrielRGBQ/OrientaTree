package com.smov.gabriel.orientatree;

import androidx.fragment.app.FragmentActivity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // our map...
        mMap = googleMap;

        // where to center the map at the outset
        LatLng center_map = new LatLng(41.6457, -4.73006);

        // setting styles...
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if(!success) {
                // TODO: handle this
                Toast.makeText(this, "Style parsing failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            // TODO: handle this
            Toast.makeText(this, "Can't find parsing file", Toast.LENGTH_SHORT).show();
        }

        // get the map image and reduce its size
        Bitmap image_bitmap = decodeSampledBitmapFromResource(getResources(),
                R.drawable.map_v01, 540, 960); // TODO: find optimum numbers here
        BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

        // position of the image
        LatLngBounds newarkBounds = new LatLngBounds(
                new LatLng(41.644025, -4.73330),       // South west corner
                new LatLng(41.648094, -4.72800));

        // set image as overlay
        GroundOverlayOptions newarkMap = new GroundOverlayOptions()
                .image(image)
                .positionFromBounds(newarkBounds);

        // set the overlay on the map
        mMap.addGroundOverlay(newarkMap);

        //mMap.addMarker(new MarkerOptions().position(center_map).title("Campo Grande"));

        // initial camera position
        mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, 17.0f));

        // setting maximum and minimum zoom the user can perform on the map
        mMap.setMinZoomPreference(16.0f);
        mMap.setMaxZoomPreference(20.0f);

        // setting bounds for the map so that user can not navigate other places
        LatLngBounds map_bounds = new LatLngBounds(
                new LatLng(41.644998, -4.733466), // SW bounds
                new LatLng(41.648523, -4.727066)  // NE bounds
        );

        mMap.setLatLngBoundsForCameraTarget(map_bounds);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}