package com.smov.gabriel.orientatree;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.Template;

import uk.co.senab.photoview.PhotoViewAttacher;

public class ChallengeActivity extends AppCompatActivity {

    private ImageView challenge_imageView;
    private TextView challengeTitle_textView, challengeText_textView,
        challengeQuestion_textView;

    private Toolbar toolbar;

    private Activity activity;
    Beacon beacon;
    private Template template;

    // some useful IDs
    private String beaconID;
    private String templateID;
    String activityID;

    private FirebaseFirestore db;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // get the beacon data from the intent
        beaconID = getIntent().getExtras().getString("beaconID");
        //templateID = getIntent().getExtras().getString("templateID");
        activity = (Activity) getIntent().getSerializableExtra("activity");
        if(activity != null) {
            templateID = activity.getTemplate();
            activityID = activity.getId();
        }

        // set the toolbar
        toolbar = findViewById(R.id.challenge_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // bind view elements
        challenge_imageView = findViewById(R.id.challenge_imageView);
        challengeTitle_textView = findViewById(R.id.challengeTitle_textView);
        challengeText_textView = findViewById(R.id.challengeText_textView);
        challengeQuestion_textView = findViewById(R.id.challengeQuestion_textView);

        // allow zooming the image view
        PhotoViewAttacher pAttacher;
        pAttacher = new PhotoViewAttacher(challenge_imageView);
        pAttacher.update();

        // get the beacon from Firestore using the data that we received from the intent
        if(beaconID != null && templateID != null) {
            db.collection("templates").document(templateID)
                    .collection("beacons").document(beaconID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            beacon = documentSnapshot.toObject(Beacon.class);
                            challengeTitle_textView.setText(beacon.getName());
                            challengeText_textView.setText(beacon.getText());
                            challengeQuestion_textView.setText(beacon.getQuestion());

                            // get the template from Firestore so that we know the type of the activity
                            // and we can display the proper Fragment
                            db.collection("templates").document(templateID)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            template = documentSnapshot.toObject(Template.class);
                                            switch (template.getColor()) {
                                                case ROJA:
                                                    // show quiz fragment
                                                    if (savedInstanceState == null) {
                                                        showFragmentQuiz();
                                                    }
                                                    break;
                                                case NARANJA:
                                                    // show short answer fragment
                                                    if (savedInstanceState == null) {
                                                        showFragmentText();
                                                    }
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                        }
                    });
        }

        // get the image of the beacon
        StorageReference ref = storageReference.child("challengeImages/" + beaconID + ".jpg");
        Glide.with(this)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(challenge_imageView);

    }

    private void showFragmentText() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.challenge_fragmentContainer, ChallengeTextFragment.class, null)
                .commit();
    }

    private void showFragmentQuiz() {
        // TODO
    }
}