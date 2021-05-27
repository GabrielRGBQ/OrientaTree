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

import java.util.ArrayList;

public class ParticipantsListActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private Activity activity;

    private RecyclerView participantsList_recyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participation> participations;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants_list);

        db = FirebaseFirestore.getInstance();

        participantsList_recyclerView = findViewById(R.id.participantsList_recyclerView);

        toolbar = findViewById(R.id.participantsList_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get the activity
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");

        if(activity != null) {
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
                            participantAdapter = new ParticipantAdapter(ParticipantsListActivity.this, participations);
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