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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.smov.gabriel.orientatree.InfoActivityActivity;
import com.smov.gabriel.orientatree.R;

public class LocationService extends Service {

    /*
      Asegurarse de que solo se llama a este servicio una vez se han comprobado y obtenido los permisos de ubicacion
      TODO
     */

    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "Location Service";

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

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
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
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
