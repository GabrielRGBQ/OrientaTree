package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.smov.gabriel.orientatree.helpers.GeofenceHelper;
import com.smov.gabriel.orientatree.model.Activity;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback/*, GoogleMap.OnMapLongClickListener*/ {

    private static final String TAG = "MapsActivity";

    private GoogleMap mMap;

    // current activity
    private Activity activity;

    private GeofencingClient geofencingClient;

    private GeofenceHelper geofenceHelper;

    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;
    private final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 1002;

    private float GEOFENCE_RADIUS = 20;

    private String GEOFENCE_ID = "SOME_GEOFENCE_ID"; // provisional... later on this will depend on the beacon
    // if the id is the same for the different geofences, they will override one another

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get the current activity
        Intent intent = getIntent();
        Activity activity = (Activity) intent.getSerializableExtra("activity");

        //geofencingClient = LocationServices.getGeofencingClient(this);
        //geofenceHelper = new GeofenceHelper(this);

        // start service of location or ask for permission in case we dont have it
        /*if(Build.VERSION.SDK_INT == 23) {
            // check if we have the permission
            if((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION))
                == PackageManager.PERMISSION_GRANTED) {
                // if we have it...
                startService();
            } else {
                // if we don't have it, we ask the user for permission
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            }
        } else {
            // start location service
            startService();
        }*/
    }

    // start location service
    public void startService() {
        Intent intent = new Intent(MapActivity.this, LocationServices.class);
        startService(intent);
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
        /*try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                // TODO: handle this
                Toast.makeText(this, "Style parsing failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            // TODO: handle this
            Toast.makeText(this, "Can't find parsing file", Toast.LENGTH_SHORT).show();
        }*/

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
        /*mMap.setMinZoomPreference(16.0f);
        mMap.setMaxZoomPreference(20.0f);

        // setting bounds for the map so that user can not navigate other places
        LatLngBounds map_bounds = new LatLngBounds(
                new LatLng(41.644998, -4.733466), // SW bounds
                new LatLng(41.648523, -4.727066)  // NE bounds
        );

        mMap.setLatLngBoundsForCameraTarget(map_bounds);*/

        //enableUserLocation();

        //mMap.setOnMapLongClickListener(this);
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

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            // ask for permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show dialog to ask the user for the permision
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_ACCESS_REQUEST_CODE);
            }
        }
    }

    // here we decide what to do depending on what the user did and whether he accepted the permission or not
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // we have the permission
                enableLocationPermissionCheck(); // wrapped in function to suppress warning
            } else {
                // we do not have the permission
                // TODO
            }
        }
        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // we have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                // we do not have the permission
                Toast.makeText(this, "The permission is needed in order to trigger geofences...",
                        Toast.LENGTH_SHORT).show();
            }
        }*/
        /*switch (requestCode) {
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService(); // we can start the location tracking service
                } else {
                    Toast.makeText(this, "Give me permissions", Toast.LENGTH_SHORT).show();
                }
        }*/
    }

    // when ckicking the map this method adds a marker and a circle representing the geofence
    // also, we create the real geofence
    /*@Override
    public void onMapLongClick(LatLng latLng) {
        if (Build.VERSION.SDK_INT >= 29) {
            // we need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                tryAddingGeofence(latLng);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // show dialog and ask for permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        } else {
            tryAddingGeofence(latLng);
        }
    }*/

    private void tryAddingGeofence(LatLng latLng) {
        mMap.clear();
        addMarker(latLng);
        addCircle(latLng, GEOFENCE_RADIUS);
        addGeofence(latLng, GEOFENCE_RADIUS);
    }

    private void addGeofence(LatLng latLng, float radius) {
        // here, we are passing the three types of action, but this will depend on our app
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL
                        | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();
        addGeofencePermissionCheck(geofencingRequest, pendingIntent); // wrapped in function to suppress warning
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
        circleOptions.fillColor(Color.argb(64, 255, 0, 0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    private void enableLocationPermissionCheck() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void addGeofencePermissionCheck(GeofencingRequest geofencingRequest, PendingIntent pendingIntent) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence added...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMesage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMesage);
                    }
                });
    }
}