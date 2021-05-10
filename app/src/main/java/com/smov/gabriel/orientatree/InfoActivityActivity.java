package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.services.LocationService;

public class InfoActivityActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;

    private Button stopButton, startButton;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_activity);

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent intent = new Intent(this, LocationService.class);

        db = FirebaseFirestore.getInstance();

        stopButton = findViewById(R.id.stop_button);
        startButton = findViewById(R.id.start_button);

        // we have to check if we have at least one of the following permissions...
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // if we don't...
            Toast.makeText(this, "We have to ask for the permissions...", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        } else {
            // if we do...
            //getLastLocation();
            // place here button listeners
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(intent);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(intent);
            }
        });

    }

    /*public void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Toast.makeText(InfoActivityActivity.this, "" + location.getLatitude() + location.getLongitude(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    Toast.makeText(this, "User gave permissions...", Toast.LENGTH_SHORT).show();
                    //getLastLocation();
                    // button listeners
                } else {
                    // user didn't give us the permission...
                    Toast.makeText(this, "User didn't give permissions", Toast.LENGTH_SHORT).show();
                }
        }
    }
}