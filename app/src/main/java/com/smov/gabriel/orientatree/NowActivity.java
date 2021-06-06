package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.services.LocationService;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NowActivity extends AppCompatActivity {

    // UI elements
    private TextView nowType_textView, nowTitle_textView, nowTime_textView, nowOrganizer_textView,
            nowTemplate_textView, nowDescription_textView, nowNorms_textView,
            nowLocation_textView, nowMode_textView, nowState_textView;
    private ExtendedFloatingActionButton nowParticipant_extendedFab, nowOrganizer_extendedFab;
    private MaterialButton nowCredentials_button;
    private Toolbar toolbar;
    private ImageView now_imageView;
    private CoordinatorLayout now_coordinatorLayout;
    private CircularProgressIndicator now_progressIndicator;

    // declaring Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // some model objects required
    private Activity activity;
    private Template template;
    private User user;
    private Participation participation;

    // some useful IDs
    private String userID;
    private String organizerID;

    // here we represent whether the current user is the organizer of the activity or not
    private boolean isOrganizer = false;

    // formatters
    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);
    // to format the way dates are displayed
    private static String pattern_day = "dd/MM/yyyy";
    private static DateFormat df_date = new SimpleDateFormat(pattern_day);

    // location permissions
    // constant that represents query for fine location permission
    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;
    // this is true or false depending on whether we have location permissions or not
    private boolean havePermissions = false;

    // needed to check that the user is at the start spot
    private FusedLocationProviderClient fusedLocationClient;

    // intent to the location service that runs in foreground while the activity is on
    private Intent locationServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now);

        // binding UI elements
        toolbar = findViewById(R.id.now_toolbar);
        nowCredentials_button = findViewById(R.id.nowCredentials_button); // only visible to organizer
        nowParticipant_extendedFab = findViewById(R.id.nowParticipant_extendedFab); // only visible to participant
        nowOrganizer_extendedFab = findViewById(R.id.nowOrganizer_extendedFab); // only visible to organizer
        nowType_textView = findViewById(R.id.nowType_textView);
        nowTitle_textView = findViewById(R.id.nowTitle_textView);
        nowTime_textView = findViewById(R.id.nowTime_textView);
        nowOrganizer_textView = findViewById(R.id.nowOrganizer_textView);
        nowTemplate_textView = findViewById(R.id.nowTemplate_textView);
        nowDescription_textView = findViewById(R.id.nowDescription_textView);
        nowNorms_textView = findViewById(R.id.nowNorms_textView);
        nowLocation_textView = findViewById(R.id.nowLocation_textView);
        now_imageView = findViewById(R.id.now_imageView);
        now_coordinatorLayout = findViewById(R.id.now_coordinatorLayout);
        nowState_textView = findViewById(R.id.nowState_textView);
        nowMode_textView = findViewById(R.id.nowMode_textView);
        now_progressIndicator = findViewById(R.id.now_progressIndicator);

        // set the toolbar
        toolbar = findViewById(R.id.now_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // location services initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // set intent to location foreground service
        locationServiceIntent = new Intent(this, LocationService.class);

        // get the current user's ID
        userID = mAuth.getCurrentUser().getUid();

        // get the activity from the intent
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");

        // check that the activity is not null
        if (activity != null) {
            organizerID = activity.getPlanner_id();
            if (organizerID.equals(userID)
                    && !activity.getParticipants().contains(userID)) {
                // if the current user is the organizer
                isOrganizer = true;
            } else if (!organizerID.equals(userID)
                    && activity.getParticipants().contains(userID)) {
                // if the current user is a participant...
                isOrganizer = false;
            } else {
                Toast.makeText(this, "Algo salió mal al " +
                        "obtener el organizador de la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                return;
            }
            // if the activity is not null, set the UI, otherwise tell the user and do nothing
            nowTitle_textView.setText(activity.getTitle());
            if(activity.isScore()) {
                nowMode_textView.append("score");
            } else {
                nowMode_textView.append("orientación clásica");
            }
            String timeString = "";
            // append start and finish hours
            timeString = timeString + df_hour.format(activity.getStartTime()) + " - " +
                    df_hour.format(activity.getFinishTime());
            // append date
            timeString = timeString + " (" + df_date.format(activity.getStartTime()) + ")";
            nowTime_textView.setText(timeString);
            // get and set the activity image
            StorageReference ref = storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(now_imageView);
            // get the template of the activity
            db.collection("templates").document(activity.getTemplate())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            template = documentSnapshot.toObject(Template.class);
                            if (template != null) {
                                // get the data from the template
                                nowType_textView.setText(template.getType() + " " + template.getColor());
                                nowDescription_textView.setText(template.getDescription());
                                nowTemplate_textView.append(template.getName());
                                nowLocation_textView.append(template.getLocation());
                                // now that we have all the data from both the activity and the template, perform specific
                                // actions depending on whether the user is the organizer or a participant
                                if (isOrganizer) {
                                    // if organizer:
                                    // 1) disable participant options
                                    nowParticipant_extendedFab.setEnabled(false);
                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                    // 2) enable organizer options
                                    nowOrganizer_extendedFab.setEnabled(true);
                                    nowOrganizer_extendedFab.setVisibility(View.VISIBLE);
                                    nowCredentials_button.setEnabled(true);
                                    nowCredentials_button.setVisibility(View.VISIBLE);
                                } else {
                                    // if participant:
                                    // 1) disable organizer options
                                    nowOrganizer_extendedFab.setEnabled(false);
                                    nowOrganizer_extendedFab.setVisibility(View.GONE);
                                    nowCredentials_button.setEnabled(false);
                                    nowCredentials_button.setVisibility(View.GONE);
                                    // 2) set listener to the participations collection to know which participant options
                                    // should be enabled
                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                                                    @Nullable FirebaseFirestoreException e) {
                                                    if (e != null) {
                                                        Toast.makeText(NowActivity.this, "Algo salió mal al obtener la participación. " +
                                                                "Sal y vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (snapshot != null && snapshot.exists()) {
                                                        participation = snapshot.toObject(Participation.class);
                                                        switch (participation.getState()) {
                                                            case NOT_YET:
                                                                nowState_textView.setText("Estado: no comenzada");
                                                                nowParticipant_extendedFab.setEnabled(true);
                                                                nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                                                                nowParticipant_extendedFab.setText("Comenzar");
                                                                break;
                                                            case NOW:
                                                                nowState_textView.setText("Estado: aún no terminada");
                                                                if(!LocationService.executing) {
                                                                    nowParticipant_extendedFab.setEnabled(true);
                                                                    nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                                                                    nowParticipant_extendedFab.setText("Continuar");
                                                                }
                                                                break;
                                                            case FINISHED:
                                                                nowState_textView.setText("Estado: terminada");
                                                                nowParticipant_extendedFab.setEnabled(false);
                                                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    } else {
                                                        Toast.makeText(NowActivity.this, "Algo salió mal al obtener la participación. " +
                                                                "Sal y vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                    // in order to track location we have to check if we have at least one of the following permissions...
                                    // so if the user is a participant we make this checking
                                    if (ActivityCompat.checkSelfPermission(NowActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                            && ActivityCompat.checkSelfPermission(NowActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        // if we don't...
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                                    } else {
                                        // if we do...
                                        havePermissions = true;
                                    }
                                }
                                // get the organizer for we need his/her name and surname
                                db.collection("users").document(organizerID)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                user = documentSnapshot.toObject(User.class);
                                                nowOrganizer_textView.append(user.getName() + " " + user.getSurname());
                                            }
                                        });
                            } else {
                                // if the template is null
                                Toast.makeText(NowActivity.this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            // if we couldn't read the template
                            Toast.makeText(NowActivity.this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    });
        } else {
            // if the activity is null
            Toast.makeText(this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        // participant FAB listener
        nowParticipant_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(havePermissions) {
                    // if the user has given the permissions required...
                    new MaterialAlertDialogBuilder(NowActivity.this)
                            .setMessage("¿Deseas comenzar/retomar la actividad? Solo deberías " +
                                    "hacerlo si el/la organizador/a ya te ha dado la salida")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Comenzar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    now_progressIndicator.setVisibility(View.VISIBLE);
                                    // 1) get location
                                    try {
                                        fusedLocationClient.getLastLocation()
                                                .addOnSuccessListener(NowActivity.this, new OnSuccessListener<Location>() {
                                                    @Override
                                                    public void onSuccess(Location location) {
                                                        if(location != null) {
                                                            Toast.makeText(NowActivity.this, "Se ha obtenido la ubicación correctamente", Toast.LENGTH_SHORT).show();
                                                            // 2) TODO: check that we are close to the start spot
                                                            // 3) if we are near enough...
                                                            final ProgressDialog pd = new ProgressDialog(NowActivity.this);
                                                            pd.setTitle("Cargando el mapa...");
                                                            pd.show();
                                                            StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                                                            try {
                                                                // try to read the map image from Firebase into a file
                                                                File localFile = File.createTempFile("images", "png");
                                                                reference.getFile(localFile)
                                                                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                                                            @Override
                                                                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                                                // if we already have the map image
                                                                                // quit the dialog
                                                                                pd.dismiss();
                                                                                // check that the service is not already being executed
                                                                                if(!LocationService.executing) {
                                                                                    // now we have to do different things depending on whether the participation
                                                                                    // is at NOT_YET or at NOW
                                                                                    switch (participation.getState()) {
                                                                                        case NOT_YET:
                                                                                            // get current time
                                                                                            long millis = System.currentTimeMillis();
                                                                                            Date current_time = new Date(millis);
                                                                                            // update the start time
                                                                                            db.collection("activities").document(activity.getId())
                                                                                                    .collection("participations").document(userID)
                                                                                                    .update("state", ParticipationState.NOW,
                                                                                                            "startTime", current_time)
                                                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onSuccess(Void unused) {
                                                                                                            now_progressIndicator.setVisibility(View.GONE);
                                                                                                            // hide the button
                                                                                                            nowParticipant_extendedFab.setEnabled(false);
                                                                                                            nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                                            // start service
                                                                                                            locationServiceIntent.putExtra("activity", activity);
                                                                                                            startService(locationServiceIntent);
                                                                                                            // update UI
                                                                                                            updateUIMap(localFile);
                                                                                                        }
                                                                                                    })
                                                                                                    .addOnFailureListener(new OnFailureListener() {
                                                                                                        @Override
                                                                                                        public void onFailure(@NonNull @NotNull Exception e) {
                                                                                                            now_progressIndicator.setVisibility(View.GONE);
                                                                                                            showSnackBar("Error al comenzar la actividad. Inténtalo de nuevo.");
                                                                                                        }
                                                                                                    });
                                                                                            break;
                                                                                        case NOW:
                                                                                            now_progressIndicator.setVisibility(View.GONE);
                                                                                            // hide button
                                                                                            nowParticipant_extendedFab.setEnabled(false);
                                                                                            nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                            // start service
                                                                                            locationServiceIntent.putExtra("activity", activity);
                                                                                            startService(locationServiceIntent);
                                                                                            // update UI
                                                                                            updateUIMap(localFile);
                                                                                            break;
                                                                                        default:
                                                                                            now_progressIndicator.setVisibility(View.GONE);
                                                                                            Toast.makeText(NowActivity.this, "Parece que la actividad ya ha terminado", Toast.LENGTH_SHORT).show();
                                                                                            break;
                                                                                    }
                                                                                } else {
                                                                                    now_progressIndicator.setVisibility(View.GONE);
                                                                                    Toast.makeText(NowActivity.this, "No se pudo iniciar la actividad... ya ha un servicio ejecutándose", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            }
                                                                        })
                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                pd.dismiss();
                                                                                showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                                                            }
                                                                        })
                                                                        .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                                                            @Override
                                                                            public void onProgress(@NonNull @NotNull FileDownloadTask.TaskSnapshot snapshot) {
                                                                                double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                                                                pd.setMessage("Progreso: " + (int) progressPercent + "%");
                                                                            }
                                                                        });
                                                            } catch (IOException e) {
                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                pd.dismiss();
                                                                showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                                            }
                                                        } else {
                                                            now_progressIndicator.setVisibility(View.GONE);
                                                            Toast.makeText(NowActivity.this, "Hubo algún problema al obtener la ubicación. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    } catch (SecurityException e) {
                                        showSnackBar("Parece que hay algún problema con los permisos de ubicación");
                                    }
                                }
                            })
                            .show();
                } else {
                    // if we don't have the permissions we ask for them
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        });

        // organizer FAB listener
        nowOrganizer_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(template != null && activity != null) {
                    updateUIParticipants();
                } else {
                    Toast.makeText(NowActivity.this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        nowCredentials_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(NowActivity.this)
                        .setTitle("Claves de acceso a la actividad")
                        .setMessage("Identificador: " + activity.getVisible_id() +
                                "\nContraseña: " + activity.getKey())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void updateUIParticipants() {
        Intent intent = new Intent(NowActivity.this, ParticipantsListActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void updateUIMap(File map) {
        Intent intent = new Intent(NowActivity.this, MapActivity.class);
        intent.putExtra("map", map);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void showSnackBar(String message) {
        if(now_coordinatorLayout != null) {
            Snackbar.make(now_coordinatorLayout, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Do nothing, just dismiss
                        }
                    })
                    .setDuration(8000)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
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