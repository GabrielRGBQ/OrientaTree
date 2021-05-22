package com.smov.gabriel.orientatree;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.Beacon;

public class BeaconContentActivity extends AppCompatActivity {

    private TextView textView2, textView3;
    private Toolbar toolbar;

    private Beacon beacon;

    private String beaconID;
    private String templateID;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_content);

        db = FirebaseFirestore.getInstance();

        beaconID = getIntent().getExtras().getString("beaconID");
        templateID = getIntent().getExtras().getString("templateID");

        toolbar = findViewById(R.id.content_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textView2 = findViewById(R.id.textView2Content);
        textView3 = findViewById(R.id.textView3Content);

        if(beaconID != null && templateID != null) {
            db.collection("templates").document(templateID)
                    .collection("beacons").document(beaconID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            beacon = documentSnapshot.toObject(Beacon.class);
                            textView2.setText("Beacon id: " + beacon.getBeacon_id());
                            textView3.setText("Beacon name: " + beacon.getName());
                        }
                    });
        }
    }
}