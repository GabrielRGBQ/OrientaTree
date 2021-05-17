package com.smov.gabriel.orientatree.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.InfoActivityActivity;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {

    /*
      Asegurarse de que solo se llama a este servicio una vez se han comprobado y obtenido los permisos de ubicacion
      TODO
     */

    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "Location Service";

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private FirebaseFirestore db;

    private FirebaseAuth mAuth;

    private String userID;

    private Activity activity;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startMyOwnForeground();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        requestLocationUpdates();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_LONG).show();

        mAuth = FirebaseAuth.getInstance();

        userID = mAuth.getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();

        // get the activity on which the user is taking part
        if(intent != null) {
            Activity activityTemp = (Activity) intent.getSerializableExtra("activity");
            if (activityTemp != null) {
                activity = activityTemp;
            }
        }

        // get the Activity on which the user is taking part
        /*db.collection("activities").document("c6c2d74a-d7bb-4e69-801c-5b604b2f701c")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        activity = documentSnapshot.toObject(Activity.class);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: handle this
                    }
                });*/

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        removeLocationUpdates();
        stopForeground(true);
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    private void startMyOwnForeground(){

        String ON_GOING_NOTIFICATION_CHANNEL_ID = "onGoing.orientatree";

        // not quite sure we are needing this...
        // maybe it's only needed when we want to trigger an action from the notification, which is not the case
        // maybe we will want it in the future though
        /*Intent notificationIntent = new Intent(this, LocationService.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "Current activity";
            String description = "This is the notification that is displayed during the time when the user is taking part in an activity";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(ON_GOING_NOTIFICATION_CHANNEL_ID, channelName, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ON_GOING_NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("Título de la actividad")
                    .setContentText("Algo de texto y cronómetro")
                    .setSmallIcon(R.drawable.ic_map)
                    //.setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                    .build();

            startForeground(2, notification);
        }
        else {
            Notification notification =
                    new Notification.Builder(this, ON_GOING_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("Título")
                            .setContentText("Descripción")
                            .setSmallIcon(R.drawable.ic_map)
                            .setColor(getResources().getColor(R.color.primary_color))
                            //.setContentIntent(pendingIntent)
                            .build();
            startForeground(1, notification);
        }

    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location location) {
        //Toast.makeText(this, "New location: " + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        if(activity != null) {
            long millis = System.currentTimeMillis();
            Date current_time = new Date(millis);
            if(current_time.after(activity.getFinishTime())) {
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .update("state", ParticipationState.FINISHED,
                                "finishTime", current_time)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Actividad terminada");
                                stopSelf();
                            }
                        });
            } else {
                Log.d(TAG, "Actividad sin terminar");
            }
        } else {
            Log.d(TAG, "Actividad nula");
        }
        Log.d(TAG, "New location: " + location.getLatitude() + " " + location.getLongitude());
        mLocation = location;
    }

    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        startService(new Intent(getApplicationContext(), LocationService.class));
        try {
            fusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            fusedLocationClient.removeLocationUpdates(mLocationCallback);
            stopSelf();
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

}
