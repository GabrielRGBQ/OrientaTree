package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.services.LocationService;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OnGoingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView onGoing_imageView;
    private TextView type_textView, beacons_textView, title_textView, location_textView, template_textView,
            description_textView, start_textView, end_textView, state_textView, timer_textView;
    private MaterialButton norms_button, map_button;
    private Button start_button;
    private CircularProgressIndicator progressIndicator;

    private LinearLayout linearLayout;

    private Activity activity;
    private Template template;
    private Participation participation;

    private String userID;

    private FirebaseFirestore db;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    // constant that represents query for fine location permission
    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;

    // intent to the location service that runs in foreground while the activity is on
    private Intent locationServiceIntent;

    // this is true or false depending on whether we have location permissions or not
    private boolean havePermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_going);

        // set intent to location foreground service
        locationServiceIntent = new Intent(this, LocationService.class);

        // set Firebase services
        db = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        mAuth = FirebaseAuth.getInstance();

        userID = mAuth.getCurrentUser().getUid();

        // get the current activity
        Intent intent = getIntent();
        Activity activity = (Activity) intent.getSerializableExtra("activity");

        // set the toolbar
        toolbar = findViewById(R.id.onGoing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // binding UI elements
        onGoing_imageView = findViewById(R.id.onGoing_imageView);
        type_textView = findViewById(R.id.onGoing_type_textView);
        beacons_textView = findViewById(R.id.onGoing_beacons_textView);
        title_textView = findViewById(R.id.onGoing_title_textView);
        location_textView = findViewById(R.id.onGoing_location_textView);
        description_textView = findViewById(R.id.onGoing_description_textview);
        template_textView = findViewById(R.id.onGoing_template_textView);
        start_textView = findViewById(R.id.onGoing_start_textView);
        end_textView = findViewById(R.id.onGoing_end_textView);
        state_textView = findViewById(R.id.onGoing_state_textView);
        timer_textView = findViewById(R.id.onGoing_timer_textView);
        norms_button = findViewById(R.id.onGoing_norms_button);
        map_button = findViewById(R.id.onGoing_map_button);
        start_button = findViewById(R.id.onGoing_start_button);
        progressIndicator = findViewById(R.id.onGoing_map_progressBar);

        // binding layout (needed for the snackbar to show)
        linearLayout = findViewById(R.id.onGoing_linearLayout);

        // setting UI according to current data
        // data from the Activity
        template_textView.setText(activity.getTemplate());
        title_textView.setText(activity.getTitle());
        start_textView.setText("Inicio: " + df_hour.format(activity.getStartTime()));
        end_textView.setText("Fin: " + df_hour.format(activity.getFinishTime()));
        // data from the Template
        db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        template = documentSnapshot.toObject(Template.class);
                        if (template.getColor() != null) {
                            type_textView.setText(template.getType() + " " + template.getColor());
                        } else {
                            type_textView.setText(template.getType());
                        }
                        description_textView.setText(template.getDescription());
                        location_textView.setText(template.getLocation());
                        beacons_textView.setText(template.getBeacons().size() + " balizas");
                        if (template.getColor() != null) {
                            switch (template.getColor()) {
                                case "Naranja":
                                    type_textView.setTextColor(getResources().getColor(R.color.orange_activity));
                                    break;
                                case "Roja":
                                    type_textView.setTextColor(getResources().getColor(R.color.red_activity));
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (activity.getParticipants().contains(userID)) {
                            // if logged user is a participant...
                            db.collection("activities").document(activity.getId())
                                    .collection("participations").document(userID)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            participation = documentSnapshot.toObject(Participation.class);
                                            switch (participation.getState()) {
                                                case NOT_YET:
                                                    start_button.setEnabled(true);
                                                    state_textView.setText("Esperando salida");
                                                    break;
                                                case NOW:
                                                    state_textView.setText("Participando ahora");
                                                    map_button.setEnabled(true);
                                                    start_button.setText("Retomar");
                                                    start_button.setEnabled(true);
                                                    break;
                                                case FINISHED:
                                                    state_textView.setText("Actvidad terminada");
                                                    map_button.setEnabled(true);
                                                    start_button.setEnabled(false);
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                        } else {
                            // if logged user is the activity planner...
                            map_button.setEnabled(true);
                            // TODO: show him his specific actions
                            start_button.setEnabled(false);
                        }
                    }
                });
        // set activity/template image
        StorageReference ref = storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(this)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(onGoing_imageView);

        // in order to track location we have to check if we have at least one of the following permissions...
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // if we don't...
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        } else {
            // if we do...
            havePermissions = true;
        }

        map_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    progressIndicator.setVisibility(View.VISIBLE);
                    StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                    try {
                        // try to read the map image from Firebase into a file
                        File localFile = File.createTempFile("images", "png");
                        reference.getFile(localFile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        // if successful, launch the map activity passing the reference of the
                                        // file where the map was downloaded
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        updateUIMap(localFile);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        showSnackBar("Error al cargar el mapa. Vuelve a intentarlo.");
                                    }
                                });
                    } catch (IOException e) {
                        progressIndicator.setVisibility(View.INVISIBLE);
                        showSnackBar("Error al cargar el mapa. Vuelve a intentarlo.");
                    }
                }
            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (havePermissions) {
                    // only if we have location permissions...
                    // get current time
                    long millis = System.currentTimeMillis();
                    Date current_time = new Date(millis);
                    // update the participation to NOW
                    db.collection("activities").document(activity.getId())
                            .collection("participations").document(userID)
                            .update("state", ParticipationState.NOW,
                                    "startTime", current_time)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // if everything's fine, map enabled, start not enabled any more and begin foreground service
                                    map_button.setEnabled(true);
                                    start_button.setEnabled(false);
                                    locationServiceIntent.putExtra("activity", activity);
                                    startService(locationServiceIntent);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    showSnackBar("Error al comenzar la actividad. Inténtalo de nuevo.");
                                }
                            });
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ongoing_overflow_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = getIntent(); // Here I don't really know why I have to do this again
        Activity activity = (Activity) intent.getSerializableExtra("activity");
        switch (item.getItemId()) {
            case R.id.abandon_activity: {
                // get current time
                long millis = System.currentTimeMillis();
                Date current_time = new Date(millis);
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .update("state", ParticipationState.FINISHED,
                                "finishTime", current_time)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                stopService(locationServiceIntent);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                showSnackBar("Error al terminar la actividad. Inténtalo de nuevo.");
                            }
                        });
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUIMap(File map) {
        Intent intent = new Intent(OnGoingActivity.this, MapActivity.class);
        intent.putExtra("map", map);
        startActivity(intent);
    }

    private void showSnackBar(String message) {
        Snackbar.make(linearLayout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Do nothing, just dismiss
                    }
                })
                .setDuration(8000)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    //setStartListener();
                    havePermissions = true;
                } else {
                    showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

}