package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.services.LocationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class InfoActivityActivity extends AppCompatActivity {

    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;

    private Button stopButton, startButton, mapButton;

    private FirebaseFirestore db;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private Intent locationServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_activity);

        locationServiceIntent = new Intent(this, LocationService.class);

        db = FirebaseFirestore.getInstance();

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        stopButton = findViewById(R.id.stop_button);
        startButton = findViewById(R.id.start_button);
        mapButton = findViewById(R.id.map_button);

        // we have to check if we have at least one of the following permissions...
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // if we don't...
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        } else {
            // if we do...
            setButtonListeners();
        }

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //updateUIMap();
                StorageReference reference = storageReference.child("maps/Version 1.png");
                try {
                    // try to download the map into a File...
                    File localFile = File.createTempFile("images", "png");
                    reference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // successfully read the map into a File...
                            //Toast.makeText(InfoActivityActivity.this, "Mapa obtenido", Toast.LENGTH_SHORT).show();
                            updateUIMap(localFile);
                    /*try {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(new FileInputStream(localFile), null, o);
                        Toast.makeText(InfoActivityActivity.this, "Mapa recuperado", Toast.LENGTH_SHORT).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(InfoActivityActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }*/
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        // something went wrong downloading the map
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(InfoActivityActivity.this, "Error al obtener el mapa", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    // something went wrong downloading the map INTO a File...
                    e.printStackTrace();
                    Toast.makeText(InfoActivityActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void updateUIMap(File map) {
        Intent intent = new Intent(InfoActivityActivity.this, MapActivity.class);
        intent.putExtra("map", map);
        startActivity(intent);
    }

    private void setButtonListeners() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(locationServiceIntent);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(locationServiceIntent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    setButtonListeners();
                } else {
                    // user didn't give us the permission...
                    Toast.makeText(this, "User didn't give permissions", Toast.LENGTH_SHORT).show();
                }
        }
    }
}