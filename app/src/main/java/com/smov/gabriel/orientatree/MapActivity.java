package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback/*, GoogleMap.OnMapLongClickListener*/ {

    private TextView reachesMap_textView, map_timer_textView;
    private MaterialButton mapBeacons_button;
    private FloatingActionButton map_fab, mapLocationOff_fab;
    private CircularProgressIndicator map_progressIndicator;
    private Toolbar toolbar;

    private GoogleMap mMap;

    private Map templateMap;
    private Participation participation;

    private Template template;
    private Activity activity;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;

    private int num_beacons; // number of beacons that this activity has
    private int beacons_reached; // number of beacons that have already been reached by the participant

    // useful IDs
    private String userID;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1110;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // get current user's ID
        userID = mAuth.getCurrentUser().getUid();

        //get map file
        Intent intent = getIntent();
        template = (Template) intent.getSerializableExtra("template");
        activity = (Activity) intent.getSerializableExtra("activity");

        // bind UI elements
        mapBeacons_button = findViewById(R.id.beaconsMap_button);
        reachesMap_textView = findViewById(R.id.reachesMap_textView);
        map_timer_textView = findViewById(R.id.map_timer_textView);
        map_fab = findViewById(R.id.map_fab);
        map_progressIndicator = findViewById(R.id.map_progressIndicator);
        mapLocationOff_fab = findViewById(R.id.mapLocationOff_fab);
        toolbar = findViewById(R.id.map_toolbar);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // set listener to the beacons button
        mapBeacons_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches(activity);
            }
        });

        // realtime listener to display the timer
        if (activity != null && template != null && userID != null
            /*&& template.getType() == TemplateType.DEPORTIVA*/) {
            db.collection("activities").document(activity.getId())
                    .collection("participations").document(userID)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable @org.jetbrains.annotations.Nullable DocumentSnapshot value, @Nullable @org.jetbrains.annotations.Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                return;
                            }
                            if (value != null && value.exists()) {
                                participation = value.toObject(Participation.class);
                                if (participation != null) {
                                    Date current_time = new Date(System.currentTimeMillis());
                                    if (participation.getStartTime() != null) {
                                        Date start_time = participation.getStartTime();
                                        switch (participation.getState()) {
                                            case NOT_YET:
                                                break;
                                            case NOW:
                                                // taking part now, so we display the current time record
                                                long diff_to_now = Math.abs(start_time.getTime() - current_time.getTime()) / 1000;
                                                time = (double) diff_to_now;
                                                // set the timer
                                                if (time < 86400) { // the maximum time it can display is 23:59:59...
                                                    timer = new Timer();
                                                    timerTask = new TimerTask() {
                                                        @Override
                                                        public void run() {
                                                            MapActivity.this.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    time++;
                                                                    map_timer_textView.setText(getTimerText());
                                                                }
                                                            });
                                                        }

                                                    };
                                                    timer.scheduleAtFixedRate(timerTask, 0, 1000);
                                                }
                                                break;
                                            case FINISHED:
                                                // participation finished, so we show the total time (static, not counting)
                                                if (timerTask != null) {
                                                    // stop the timer
                                                    timerTask.cancel();
                                                }
                                                if (participation.getFinishTime() != null) {
                                                    Date finish_time = participation.getFinishTime();
                                                    long diff_to_finish = Math.abs(start_time.getTime() - finish_time.getTime()) / 1000;
                                                    double total_time = (double) diff_to_finish;
                                                    map_timer_textView.setText(getTimerText(total_time));
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    });
            if (activity.isLocation_help()) {
                // if location is allowed in this activity...
                map_fab.setEnabled(true);
                map_fab.setVisibility(View.VISIBLE);
            }
        }

        // realtime listener to display the number of beacons already reached
        if (activity != null && template != null) {
            if (template.getBeacons() != null) {
                num_beacons = template.getBeacons().size();
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .collection("beaconReaches")
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value,
                                                @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    return;
                                }
                                beacons_reached = value.size();
                                int show_reaches = beacons_reached;
                                if (beacons_reached > (num_beacons - 1)) {
                                    show_reaches = num_beacons - 1;
                                }
                                reachesMap_textView.setText(show_reaches + "/"
                                        + (num_beacons - 1));
                            }
                        });
            }
        }

        map_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableLocation();
            }
        });

        mapLocationOff_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableLocation();
            }
        });
    }

    private void disableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if(mMap != null) {
                // hide location off fab
                mapLocationOff_fab.setVisibility(View.GONE);
                mapLocationOff_fab.setEnabled(false);
                // show location fab
                map_fab.setEnabled(true);
                map_fab.setVisibility(View.VISIBLE);
                // disable location
                mMap.setMyLocationEnabled(false);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                // hide location fab
                map_fab.setVisibility(View.GONE);
                map_fab.setEnabled(false);
                // show location off fab
                mapLocationOff_fab.setEnabled(true);
                mapLocationOff_fab.setVisibility(View.VISIBLE);
                // enable location
                mMap.setMyLocationEnabled(true);
            }
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateUIReaches(Activity activity) {
        Intent intent = new Intent(MapActivity.this, ReachesActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
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

        // setting styles...
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
        }

        if (template != null) {
            db.collection("maps").document(template.getMap_id())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            // getting the map
                            templateMap = documentSnapshot.toObject(Map.class);

                            // where to center the map at the outset
                            LatLng center_map = new LatLng(templateMap.getCentering_point().getLatitude(),
                                    templateMap.getCentering_point().getLongitude());

                            // get the map image from a file and reduce its size
                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                            //File mypath = new File(directory, activity.getId() + ".png");
                            File mypath = new File(directory, activity.getTemplate() + ".png");
                            Bitmap image_bitmap = decodeFile(mypath, 540, 960);
                            BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

                            LatLngBounds overlay_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getOverlay_corners().get(0).getLatitude(),
                                            templateMap.getOverlay_corners().get(0).getLongitude()),       // South west corner
                                    new LatLng(templateMap.getOverlay_corners().get(1).getLatitude(),
                                            templateMap.getOverlay_corners().get(1).getLongitude()));

                            // set image as overlay
                            GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                    .image(image)
                                    .positionFromBounds(overlay_bounds);

                            // set the overlay on the map
                            mMap.addGroundOverlay(overlayMap);

                            mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, templateMap.getInitial_zoom()));

                            // setting maximum and minimum zoom the user can perform on the map
                            mMap.setMinZoomPreference(templateMap.getMin_zoom());
                            mMap.setMaxZoomPreference(templateMap.getMax_zoom());

                            // setting bounds for the map so that user can not navigate other places
                            LatLngBounds map_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getMap_corners().get(0).getLatitude(),
                                            templateMap.getMap_corners().get(0).getLongitude()), // SW bounds
                                    new LatLng(templateMap.getMap_corners().get(1).getLatitude(),
                                            templateMap.getMap_corners().get(1).getLongitude())  // NE bounds
                            );
                            mMap.setLatLngBoundsForCameraTarget(map_bounds);
                        }
                    });
        } else {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
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

    // decode image from file
    private Bitmap decodeFile(File f, int width, int height) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            int scale = calculateInSampleSize(o, width, height);

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    // used when we need a timer because the participation is not finished
    private String getTimerText() {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    // used when the activity is already finished, and we do not need a timer any more
    private String getTimerText(double time) {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    private String formatTime(int seconds, int minutes, int hours) {
        return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    //havePermissions = true;
                    Toast.makeText(this, "Ahora ya puedes comenzar la actividad", Toast.LENGTH_SHORT).show();
                } else {
                    //showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}