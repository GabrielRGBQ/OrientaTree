package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MyParticipationActivity extends AppCompatActivity {

    // UI elements
    private Toolbar toolbar;
    private TextView myParticipationStart_textView, myParticipationFinish_textView,
            myParticipationTotal_textView, myParticipationBeacons_textView;
    private MaterialButton myParticipationBeacons_button, myParticipationTrack_button,
            myParticipationInscription_button, myParticipationDelete_button;

    // model objects
    private Participation participation;
    private Activity activity;
    private ArrayList<BeaconReached> reaches;
    private Template template;

    // useful IDs
    private String userID;
    private String activityID;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm:ss";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_participation);

        // get the intent
        Intent intent = getIntent();
        participation = (Participation) intent.getSerializableExtra("participation");
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template"); 

        // binding UI elements
        toolbar = findViewById(R.id.myParticipation_toolbar);
        myParticipationStart_textView = findViewById(R.id.myParticipationStart_textView);
        myParticipationFinish_textView = findViewById(R.id.myParticipationFinish_textView);
        myParticipationTotal_textView = findViewById(R.id.myParticipationTotal_textView);
        myParticipationBeacons_textView = findViewById(R.id.myParticipationBeacons_textView);
        myParticipationTrack_button = findViewById(R.id.myParticipationTrack_button);
        myParticipationBeacons_button = findViewById(R.id.myParticipationBeacons_button);
        myParticipationDelete_button = findViewById(R.id.myParticipationDelete_button);
        myParticipationInscription_button = findViewById(R.id.myParticipationInscription_button);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // setting useful IDs
        userID = mAuth.getCurrentUser().getUid();
        activityID = activity.getId();

        // setting the AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // setting the info
        // check that we received properly the participation and the activity
        if(participation != null && activityID != null
                && template != null) {
            // check if it has already started
            if(participation.getState() != ParticipationState.NOT_YET) {
                Date start_time = participation.getStartTime();
                Date finish_time = participation.getFinishTime();
                if(start_time != null) {
                    myParticipationStart_textView.setText(df_hour.format(start_time));
                } else {
                    myParticipationStart_textView.setText("");
                }
                if(finish_time != null) {
                    myParticipationFinish_textView.setText(df_hour.format(finish_time));
                } else {
                    myParticipationFinish_textView.setText("");
                }
                if((start_time != null && finish_time != null) 
                        && start_time.before(finish_time)) {
                    long diff_millis = Math.abs(finish_time.getTime() - start_time.getTime());
                    myParticipationTotal_textView.setText(formatMillis(diff_millis));
                } else {
                    myParticipationTotal_textView.setText("");
                }
                // get the reaches
                reaches = new ArrayList<>();
                db.collection("activities").document(activityID)
                        .collection("participations").document(userID)
                        .collection("beaconReaches")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    BeaconReached reach = documentSnapshot.toObject(BeaconReached.class);
                                    reaches.add(reach);
                                }
                                // set the number of beacons reached and the total number of beacons
                                if(template.getBeacons() != null) {
                                    int num_reaches = reaches.size();
                                    int number_of_beacons = template.getBeacons().size();
                                    if(num_reaches == number_of_beacons) {
                                        num_reaches --;
                                    }
                                    myParticipationBeacons_textView.setText(num_reaches + "/" + (number_of_beacons - 1));
                                } else {
                                    Toast.makeText(MyParticipationActivity.this, "No se pudo recuperar la información de las balizas alcanzadas", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull @NotNull Exception e) {
                                Toast.makeText(MyParticipationActivity.this, "No se pudo recuperar la información de las balizas", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // if the participation has not yet started
                // all fields empty
                myParticipationStart_textView.setText("");
                myParticipationFinish_textView.setText("");
                myParticipationTotal_textView.setText("");
                myParticipationBeacons_textView.setText("");
                // enable the button to cancel the inscription
                myParticipationInscription_button.setEnabled(true);
            }
        } else {
            // if we couldn't receive right the participation
            Toast.makeText(this, "Ocurrió un error al leer la información. Salga e inténtelo de nuevo", Toast.LENGTH_SHORT).show();
        }

        myParticipationBeacons_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches();
            }
        });

    }

    private void updateUIReaches() {
        if(template != null && activity != null && userID != null
            && (userID.equals(participation.getParticipant()))) {
            Intent intent = new Intent(MyParticipationActivity.this, ReachesActivity.class);
            intent.putExtra("activity", activity);
            intent.putExtra("template", template);
            intent.putExtra("participantID", userID);
            startActivity(intent);
        }
    }

    private String formatMillis (long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String time = hours % 24 + "h " + minutes % 60 + "m " + seconds % 60 + "s";
        return time;
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