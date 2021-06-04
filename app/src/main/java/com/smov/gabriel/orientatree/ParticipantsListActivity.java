package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ParticipantAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;

import java.util.ArrayList;

public class ParticipantsListActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private Activity activity;
    private Template template;

    private RecyclerView participantsList_recyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participation> participations;

    // needed to pass it to the adapter so that cards can be clicked and head to a new activity
    private ParticipantsListActivity participantsListActivity;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants_list);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        // binding interface elements
        participantsList_recyclerView = findViewById(R.id.participantsList_recyclerView);

        participantsListActivity = (ParticipantsListActivity) this;

        // setting the AppBar
        toolbar = findViewById(R.id.participantsList_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get the activity
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");

        if(activity != null && template != null) {
            db.collection("activities").document(activity.getId())
                    .collection("participations")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                return;
                            }
                            participations = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                Participation participation = doc.toObject(Participation.class);
                                participations.add(participation);
                            }
                            participantAdapter = new ParticipantAdapter(participantsListActivity, ParticipantsListActivity.this,
                                    participations, template, activity);
                            participantsList_recyclerView.setAdapter(participantAdapter);
                            participantsList_recyclerView.setLayoutManager(new LinearLayoutManager(ParticipantsListActivity.this));
                        }
                    });
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