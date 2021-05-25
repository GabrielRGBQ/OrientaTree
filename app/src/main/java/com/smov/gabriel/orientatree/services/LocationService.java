package com.smov.gabriel.orientatree.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.common.internal.Constants;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.BeaconContentActivity;
import com.smov.gabriel.orientatree.OnGoingActivity;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class LocationService extends Service {

    // allows to know from the activity whether the service is being executed or no
    public static boolean executing = false;

    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "Location Service";

    private static final float LOCATION_PRECISION = 20f;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLocation;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private FirebaseFirestore db;

    private FirebaseAuth mAuth;

    private String userID;

    private Activity activity;
    private ArrayList<Beacon> beacons; // all the beacons
    private Set<String> beacon_indexes;

    private int totalBeacons; // total number of beacons
    private int nextBeacon = 0; // which one is the next beacon

    private boolean uploadingReach = false; // flag to signal if we are trying to upload a reach and therefore the others must wait
    private boolean indexesUpdated = false; // flag to signal if we have updated the set of beacons that have not been reached yet

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        executing = true;

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
        //Toast.makeText(this, "service starting", Toast.LENGTH_LONG).show();

        mAuth = FirebaseAuth.getInstance();

        userID = mAuth.getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();

        // get the activity on which the user is taking part and its beacons
        if (intent != null) {
            Activity activityTemp = (Activity) intent.getSerializableExtra("activity");
            if (activityTemp != null) {
                activity = activityTemp; // activity gotten from intent
                beacons = new ArrayList<>();
                beacon_indexes = new HashSet<>();
                db.collection("templates").document(activity.getTemplate())
                        .collection("beacons")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                // getting beacons from Firestore
                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    Beacon beacon = documentSnapshot.toObject(Beacon.class);
                                    beacons.add(beacon);
                                    beacon_indexes.add(beacon.getBeacon_id());
                                }
                                // get the number o beacons that the activity has
                                totalBeacons = beacons.size();
                                Log.d(TAG, "##################\n" + beacon_indexes.size() + "\n#################");
                                // place the beacons in order
                                Collections.sort(beacons, new Beacon());
                            }
                        });
                // get the number of already reached beacons at the moment of starting the service
                // so that we know which beacon is next
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .collection("beaconReaches")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                nextBeacon = task.getResult().size() + 1;
                                for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    BeaconReached beaconReached = documentSnapshot.toObject(BeaconReached.class);
                                    beacon_indexes.remove(beaconReached.getBeacon_id());
                                }
                                indexesUpdated = true;
                            }
                        });
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        executing = false;
        removeLocationUpdates();
        stopForeground(true);
    }

    private void startMyOwnForeground() {

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
                    .setColor(getColor(R.color.primary_color))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                    .build();

            startForeground(2, notification);
        } else {
            Notification notification =
                    new Notification.Builder(this, ON_GOING_NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("Título")
                            .setContentText("Descripción")
                            .setSmallIcon(R.drawable.ic_map)
                            .setColor(getColor(R.color.primary_color))
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
        // get current time
        long millis = System.currentTimeMillis();
        Date current_time = new Date(millis);

        // get current location
        double lat1 = location.getLatitude();
        double lng1 = location.getLongitude();

        // check if we already got the activity data. If we don't have it yet, we won't do anything
        if (activity != null) { // if we already have the activity data...

            // check if the activity has already finished
            if (current_time.after(activity.getFinishTime())) { // if the activity time is finished...
                // change the state and set the finish time to that of the activity, because it means that
                // the user did not get to the end of the activity
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .update("state", ParticipationState.FINISHED,
                                "finishTime", activity.getFinishTime())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Ending activity...");
                                stopSelf();
                            }
                        });
            } else { // if the activity did not finish yet...

                // check if it is classical or score type
                if (activity.isScore()) { // if activity is score type... Here all the score activity logic
                    Log.d(TAG, "La actividad es score");
                    if(beacons != null && beacon_indexes != null && nextBeacon > 0 && indexesUpdated) { // if all the data is prepared already...
                        if(beacon_indexes.size() == 0) { // no more beacons... finish participation
                            db.collection("activities").document(activity.getId())
                                    .collection("participations").document(userID)
                                    .update("state", ParticipationState.FINISHED,
                                            "finishTime", activity.getFinishTime())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "Actividad terminada");
                                            stopSelf();
                                        }
                                    });
                        } else {
                            for(Beacon beacon : beacons) {
                                if(beacon_indexes.contains(beacon.getBeacon_id())){
                                    // get the distance to the current position
                                    double lat2 = beacon.getLocation().getLatitude();
                                    double lng2 = beacon.getLocation().getLongitude();
                                    float dist = getDistance(lat1, lat2, lng1, lng2);
                                    if (dist <= LOCATION_PRECISION && !uploadingReach) { // if it is near enough...
                                        BeaconReached beaconReached = new BeaconReached(current_time, beacon.getBeacon_id()); // create a new BeaconReached
                                        uploadingReach = true; // uploading...
                                        db.collection("activities").document(activity.getId())
                                                .collection("participations").document(userID)
                                                .collection("beaconReaches").document(beaconReached.getBeacon_id())
                                                .set(beaconReached)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        uploadingReach = false; // not uploading any more
                                                        sendBeaconNotification(beacon, activity);
                                                        beacon_indexes.remove(beacon.getBeacon_id());
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull @NotNull Exception e) {
                                                        uploadingReach = false; // not uploading any more
                                                        // don't update nextBeacon, so we will try it again
                                                    }
                                                });
                                        break;
                                    }
                                }
                            }
                            Toast.makeText(this, "Te quedan " + beacon_indexes.size(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Beacons o indexes null");
                    }
                } else { // if activity is classical type...
                    // classical activity logic...
                    Log.d(TAG, "La actividad es clásica");
                    if (beacons != null && nextBeacon > 0) { // if we already have read beacons and reaches...
                        if (nextBeacon <= totalBeacons) { // if there are still beacons left to be reached...
                            Beacon beacon = beacons.get(nextBeacon - 1); // get the beacon we are looking for now
                            // get the selected beacon's location
                            double lat2 = beacon.getLocation().getLatitude();
                            double lng2 = beacon.getLocation().getLongitude();
                            float dist = getDistance(lat1, lat2, lng1, lng2); // get the distance to the current position
                            if (dist <= LOCATION_PRECISION && !uploadingReach) { // if near enough and not already trying to upload...
                                BeaconReached beaconReached = new BeaconReached(current_time, beacon.getBeacon_id()); // create a new BeaconReached
                                // add the new BeaconReached to Firestore...
                                uploadingReach = true; // uploading...
                                db.collection("activities").document(activity.getId())
                                        .collection("participations").document(userID)
                                        .collection("beaconReaches").document(beaconReached.getBeacon_id())
                                        .set(beaconReached)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                // BeaconReached added to Firestore
                                                sendBeaconNotification(beacon, activity);
                                                uploadingReach = false; // not uploading any more
                                                Log.d(TAG, "Alcanzada: " + beacon.getName());
                                                nextBeacon++; // update which the next beacon is
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull @NotNull Exception e) {
                                                uploadingReach = false; // not uploading any more
                                                // don't update nextBeacon, so we will try it again
                                            }
                                        });
                            } else { // if we are not close to any beacon or there is one trying to be uploaded...
                                Toast.makeText(this, "Too far away from: " + beacon.getName() + " or uploading", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Too far away from: " + beacon.getName() + " or uploading");
                            }
                        } else { // if no more beacons left...
                            db.collection("activities").document(activity.getId())
                                    .collection("participations").document(userID)
                                    .update("state", ParticipationState.FINISHED,
                                            "finishTime", activity.getFinishTime())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "Actividad terminada");
                                            stopSelf();
                                        }
                                    });
                        }
                    } else { // if activity or beacons null...
                        Log.d(TAG, "Couldn't read beacons and/or reaches yet");
                    }
                }
            }
        } else {
            Log.d(TAG, "Actividad nula");
        }
        Log.d(TAG, "New location: " + location.getLatitude() + " " + location.getLongitude());
        mLocation = location;
    }

    private void sendBeaconNotification(Beacon beacon, Activity activity) {
        // 1 create the channel if needed, and set the intent for the action
        String BEACON_NOTIFICATION_CHANNEL_ID = "beacon.orientatree"; // name of the channel for beacon notifications

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, BeaconContentActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("beaconID", beacon.getBeacon_id());
        intent.putExtra("templateID", activity.getTemplate());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificaciones balizas";
            String description = "Notificaciones que aparecen al llegar a una baliza";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(BEACON_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // 2 create the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BEACON_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flag)
                    .setColor(getColor(R.color.secondary_color))
                    .setContentTitle("Baliza " + beacon.getName())
                    //.setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                    .setLights(getColor(R.color.primary_color), 3000, 3000)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentText("Ya puedes ver el contenido de la baliza");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);

            // 3 show the notification
            notificationManager.notify(beacon.getNumber(), builder.build());
            /*Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(new long[] {1000, 100, 1000, 100, 1000}, 1);
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            ringtone.play();*/
        } else {
            // 2.1 create the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BEACON_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_flag)
                    .setColor(getColor(R.color.secondary_color))
                    .setContentTitle("Baliza " + beacon.getName())
                    .setContentText("Ya puedes ver el contenido de la baliza")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                    .setLights(getColor(R.color.primary_color), 3000, 3000)
                    .setFullScreenIntent(pendingIntent, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(alarmSound);

            // 3.1 show the notification
            NotificationManagerCompat nManager = NotificationManagerCompat.from(this);
            nManager.notify(beacon.getNumber(), builder.build());
        }
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

    private float getDistance(double lat1, double lat2, double lng1, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double p = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(p), Math.sqrt(1 - p));
        float dist = (float) (earthRadius * c);
        return dist;
    }

}
