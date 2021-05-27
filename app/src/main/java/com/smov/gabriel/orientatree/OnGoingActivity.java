package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.View.GONE;

public class OnGoingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView onGoing_imageView;
    private TextView type_textView, title_textView, location_textView, template_textView,
            startAndFinish_textView;
    private MaterialButton norms_button, map_button;
    private Button start_button, participants_button;
    private CircularProgressIndicator progressIndicator/*, generalProgressIndicator*/;

    private LinearLayout linearLayout;

    Activity activity;
    Template template;
    Participation participation;

    String userID;

    FirebaseFirestore db;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);
    // to format the way dates are displayed
    private static String pattern_day = "dd/MM/yyyy";
    private static DateFormat df_date = new SimpleDateFormat(pattern_day);

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
        activity = (Activity) intent.getSerializableExtra("activity");

        // set the toolbar
        toolbar = findViewById(R.id.onGoing_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // binding UI elements
        onGoing_imageView = findViewById(R.id.onGoing_imageView);
        type_textView = findViewById(R.id.onGoing_type_textView);
        title_textView = findViewById(R.id.onGoing_title_textView);
        location_textView = findViewById(R.id.onGoing_location_textView);
        template_textView = findViewById(R.id.onGoing_template_textView);
        norms_button = findViewById(R.id.onGoing_norms_button);
        map_button = findViewById(R.id.onGoing_map_button);
        progressIndicator = findViewById(R.id.onGoing_map_progressBar);
        start_button = findViewById(R.id.onGoing_start_button);
        startAndFinish_textView = findViewById(R.id.onGoing_startFinish_textView);
        participants_button = findViewById(R.id.onGoing_participants_button);

        // binding layout (needed for the snackbar to show)
        linearLayout = findViewById(R.id.onGoing_linearLayout);

        // setting UI according to current data
        // data from the Activity
        template_textView.setText(activity.getTemplate());
        title_textView.setText(activity.getTitle());
        String date = "Día " + df_date.format(activity.getStartTime()) +
                ", Duración " + df_hour.format(activity.getStartTime()) +
                "-" + df_hour.format(activity.getFinishTime());
        startAndFinish_textView.setText(date);
        String score;
        if (activity.isScore()) {
            score = ", score";
        } else {
            score = ", orientación clásica";
        }
        // data from the Template
        db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        //generalProgressIndicator.setVisibility(GONE);
                        template = documentSnapshot.toObject(Template.class);
                        if (template.getColor() != null) {
                            type_textView.setText(template.getType() + " " + template.getColor() + score);
                        } else {
                            type_textView.setText(template.getType());
                        }
                        location_textView.setText(template.getLocation());
                        //beacons_textView.setText("Balizas: " + template.getBeacons().size() + score);
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
                                                    start_button.setVisibility(View.VISIBLE);
                                                    break;
                                                case NOW:
                                                    map_button.setEnabled(true);
                                                    if (!LocationService.executing) {
                                                        start_button.setText("Continuar");
                                                        start_button.setEnabled(true);
                                                        start_button.setVisibility(View.VISIBLE);
                                                    } else {
                                                        start_button.setEnabled(false);
                                                        start_button.setVisibility(GONE);
                                                        // show fragment
                                                        if (savedInstanceState == null) {
                                                            showFragment();
                                                        }
                                                    }
                                                    break;
                                                case FINISHED:
                                                    map_button.setEnabled(true);
                                                    start_button.setEnabled(false);
                                                    start_button.setVisibility(GONE);
                                                    // show fragment
                                                    if (savedInstanceState == null) {
                                                        showFragment();
                                                    }
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                        } else {
                            // if logged user is the activity planner...
                            map_button.setEnabled(true);
                            start_button.setEnabled(false);
                            start_button.setVisibility(GONE);
                            participants_button.setVisibility(View.VISIBLE);
                            participants_button.setEnabled(true);
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
                    new MaterialAlertDialogBuilder(OnGoingActivity.this)
                            .setMessage("¿Deseas comenzar/retomar la actividad? Solo deberías " +
                                    "hacerlo si el/la organizador/a ya te ha dado la salida")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // get current time
                                    long millis = System.currentTimeMillis();
                                    Date current_time = new Date(millis);
                                    // update the participation to NOW
                                    if(participation.getState() == ParticipationState.NOT_YET) {
                                        //generalProgressIndicator.setVisibility(View.VISIBLE);
                                        db.collection("activities").document(activity.getId())
                                                .collection("participations").document(userID)
                                                .update("state", ParticipationState.NOW,
                                                        "startTime", current_time)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //generalProgressIndicator.setVisibility(GONE);
                                                        // if everything's fine, map enabled, start not enabled any more and begin foreground service
                                                        map_button.setEnabled(true);
                                                        start_button.setEnabled(false);
                                                        start_button.setVisibility(GONE);
                                                        participation.setState(ParticipationState.NOW);
                                                        participation.setStartTime(current_time);
                                                        if (savedInstanceState == null) {
                                                            showFragment();
                                                        }
                                                        locationServiceIntent.putExtra("activity", activity);
                                                        Toast.makeText(OnGoingActivity.this, "Participando en la actividad", Toast.LENGTH_SHORT).show();
                                                        startService(locationServiceIntent);
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        showSnackBar("Error al comenzar la actividad. Inténtalo de nuevo.");
                                                    }
                                                });
                                    } else if(participation.getState() == ParticipationState.NOW) {
                                        start_button.setEnabled(false);
                                        start_button.setVisibility(GONE);
                                        if (savedInstanceState == null) {
                                            showFragment();
                                        }
                                        locationServiceIntent.putExtra("activity", activity);
                                        Toast.makeText(OnGoingActivity.this, "Participando en la actividad", Toast.LENGTH_SHORT).show();
                                        startService(locationServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        });

        participants_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIParticipants();
            }
        });

    }

    private void showFragment() {
        if (activity.getParticipants().contains(userID)) { // if current user is a participant
            // show participants fragment and start button, hide (just in case) participants button
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.onGoing_fragmentContainer, ParticipantFragment.class, null)
                    .commit();
            start_button.setVisibility(View.GONE);
            participants_button.setVisibility(GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!activity.getPlanner_id().equals(userID)) {
            getMenuInflater().inflate(R.menu.ongoing_overflow_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!activity.getPlanner_id().equals(userID)) {
            MenuItem abandon = menu.findItem(R.id.abandon_activity);
            MenuItem unsubscribe = menu.findItem(R.id.unsubscribe_activity);
            //generalProgressIndicator.setVisibility(View.VISIBLE);
            db.collection("activities").document(activity.getId())
                    .collection("participations").document(userID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            //generalProgressIndicator.setVisibility(GONE);
                            Participation participation = documentSnapshot.toObject(Participation.class);
                            if (participation != null) {
                                switch (participation.getState()) {
                                    case NOT_YET:
                                        abandon.setEnabled(false);
                                        unsubscribe.setEnabled(true);
                                        break;
                                    case NOW:
                                        abandon.setEnabled(true);
                                        unsubscribe.setEnabled(false);
                                        break;
                                    case FINISHED:
                                        abandon.setEnabled(false);
                                        unsubscribe.setEnabled(false);
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                abandon.setEnabled(false);
                                unsubscribe.setEnabled(false);
                            }
                        }
                    });
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!activity.getPlanner_id().equals(userID)) {
            switch (item.getItemId()) {
                case R.id.abandon_activity:
                    new MaterialAlertDialogBuilder(this)
                            .setMessage("¿Estás seguro/a de que quieres abandonar esta actividad en curso? Se " +
                                    "dará por finalizada y no podrás retomarla.")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // get current time
                                    long millis = System.currentTimeMillis();
                                    Date current_time = new Date(millis);
                                    //generalProgressIndicator.setVisibility(View.VISIBLE);
                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .update("state", ParticipationState.FINISHED,
                                                    "finishTime", current_time)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //generalProgressIndicator.setVisibility(GONE);
                                                    // after updating finish time and state...
                                                    stopService(locationServiceIntent); // stop service
                                                    start_button.setEnabled(false); // disable start
                                                    item.setEnabled(false); // disable this menu item
                                                    Toast.makeText(OnGoingActivity.this, "La actividad ha terminado", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    //generalProgressIndicator.setVisibility(GONE);
                                                    showSnackBar("Error al terminar la actividad. Inténtalo de nuevo.");
                                                }
                                            });
                                }
                            })
                            .show();
                    break;
                case R.id.unsubscribe_activity:
                    new MaterialAlertDialogBuilder(this)
                            .setMessage("¿Estás seguro/a de que quieres desinscribirte de esta actividad?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //generalProgressIndicator.setVisibility(View.VISIBLE);
                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    //generalProgressIndicator.setVisibility(GONE);
                                                    Participation participation = documentSnapshot.toObject(Participation.class);
                                                    if (participation.getState() == ParticipationState.NOT_YET) {
                                                        activity.removeParticipant(userID);
                                                        db.collection("activities").document(activity.getId())
                                                                .set(activity)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void unused) {
                                                                        db.collection("activities").document(activity.getId())
                                                                                .collection("participations").document(userID)
                                                                                .delete()
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                    @Override
                                                                                    public void onSuccess(Void unused) {
                                                                                        start_button.setEnabled(false);
                                                                                        Toast.makeText(OnGoingActivity.this, "Ya no estás inscrito/a en la actividad", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull @NotNull Exception e) {
                                                                        //generalProgressIndicator.setVisibility(GONE);
                                                                        showSnackBar("Error al deshacer la inscripción. Inténtalo de nuevo");
                                                                    }
                                                                });
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    //generalProgressIndicator.setVisibility(GONE);
                                                    showSnackBar("Error al deshacer la inscripción. Inténtalo de nuevo.");
                                                }
                                            });
                                }
                            })
                            .show();
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

    private void updateUIParticipants() {
        Intent intent = new Intent(OnGoingActivity.this, ParticipantsListActivity.class);
        intent.putExtra("activity", activity);
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
                    havePermissions = true;
                    Toast.makeText(this, "Ahora ya puedes comenzar la actividad", Toast.LENGTH_SHORT).show();
                } else {
                    showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

}