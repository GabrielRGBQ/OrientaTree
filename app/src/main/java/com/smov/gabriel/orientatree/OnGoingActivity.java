package com.smov.gabriel.orientatree;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class OnGoingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView onGoing_imageView;
    private TextView type_textView, beacons_textView, title_textView, location_textView, template_textView,
        description_textView, start_textView, end_textView, state_textView, timer_textView;
    private MaterialButton norms_button, map_button;
    private Button start_button;

    private Activity activity;
    private Template template;
    private Participation participation;

    private FirebaseFirestore db;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_going);

        // set Firebase services
        db = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

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
        start_button = findViewById(R.id.start_button);

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
                        if(template.getColor() != null) {
                            type_textView.setText(template.getType() + " " + template.getColor());
                        } else {
                            type_textView.setText(template.getType());
                        }
                        description_textView.setText(template.getDescription());
                        location_textView.setText(template.getLocation());
                        beacons_textView.setText(template.getBeacons().size() + " balizas");
                        if(template.getColor() != null) {
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
                    }
                });
        // set activity/template image
        StorageReference ref = storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(this)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(onGoing_imageView);

    }

}