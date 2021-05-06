package com.smov.gabriel.orientatree;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.Activity;

import java.util.ArrayList;

public class InfoActivityActivity extends AppCompatActivity {

    private TextView sampleParticipant_textView;

    private String activity_id;

    private Activity selected_activity;

    private ArrayList<String> participants;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_activity);

        getIntentData();

        db = FirebaseFirestore.getInstance();

        sampleParticipant_textView = findViewById(R.id.sampleParticipant_textView);

        DocumentReference docRef = db.collection("activities").document(activity_id);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                selected_activity = documentSnapshot.toObject(Activity.class);
                participants = selected_activity.getParticipants();
                if(participants != null && participants.size() >= 1) {
                    sampleParticipant_textView.setText(participants.get(0));
                } else {
                    sampleParticipant_textView.setText(selected_activity.getTitle());
                }
            }
        });
    }

    void getIntentData() {
        if(getIntent().hasExtra("activity_id")) {
            activity_id = getIntent().getStringExtra("activity_id");
        } else {

        }
    }
}