package com.smov.gabriel.orientatree.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

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
import com.smov.gabriel.orientatree.ChallengeActivity;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class LocationService extends Service {

    // allows to know from the activity whether the service is being executed or no
    public static boolean executing = false;

    private boolean initialDataSet = false; // flag to signal if we already have al the initial data needed to play
    private boolean uploadingReach = false; // flag to signal if we are trying to upload a reach and therefore the others must wait

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
    private Set<String> beaconsReached; // set containing the ids of the beacons already reached

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

        // get the current user id
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();

        // get firestore instance
        db = FirebaseFirestore.getInstance();

        // get the activity on which the user is taking part and its beacons
        if (intent != null) {
            // (this step is needed because onStart is executed twice)
            Activity activityTemp = (Activity) intent.getSerializableExtra("activity");
            if (activityTemp != null) {
                activity = activityTemp; // here we have the activity
                beacons = new ArrayList<>();
                beaconsReached = new HashSet<>();
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
                                }
                                Collections.sort(beacons, new Beacon());
                                // DEBUG
                                Log.d(TAG, "La actividad tiene " + beacons.size() + " balizas:\n");
                                for (Beacon beacon : beacons) {
                                    Log.d(TAG, beacon.getBeacon_id() + "\n");
                                }
                                //
                                // now we have to check if some of those beacons are already reached
                                // so we search for the reaches for that participant and activity
                                db.collection("activities").document(activity.getId())
                                        .collection("participations").document(userID)
                                        .collection("beaconReaches")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                                    // here we have a list with the reaches achieved
                                                    BeaconReached beaconReached = documentSnapshot.toObject(BeaconReached.class);
                                                    beaconsReached.add(beaconReached.getBeacon_id());
                                                }
                                                // DEBUG
                                                Log.d(TAG, "De esas balizas " + beaconsReached.size() + " han sido alcanzadas:\n");
                                                if (beaconsReached.size() > 0) {
                                                    for (String reachedID : beaconsReached) {
                                                        Log.d(TAG, reachedID + "\n");
                                                    }
                                                }
                                                // DEBUG
                                                Log.d(TAG, "Por lo tanto, quedan por alcanzar " + (beacons.size() - beaconsReached.size()) + ":\n");
                                                for (Beacon beacon : beacons) {
                                                    if (!beaconsReached.contains(beacon.getBeacon_id())) {
                                                        Log.d(TAG, beacon.getBeacon_id());
                                                    }
                                                }
                                                //
                                                initialDataSet = true; // we have all the initial data ready
                                            }
                                        });
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

    private void onNewLocation(Location location) {

        // DEBUG
        Log.d(TAG, "Executing New Location...\n New location: " + location.getLatitude() + " " +
                location.getLongitude() + "\n");
        if (initialDataSet) {
            Log.d(TAG, "Ya tenemos los datos iniciales");
        } else {
            Log.d(TAG, "Aún no están los datos iniciales");
        }
        //
        mLocation = location;

        // get current time
        long millis = System.currentTimeMillis();
        Date current_time = new Date(millis);

        // get current location
        double lat1 = location.getLatitude();
        double lng1 = location.getLongitude();

        // check if we already have the initial data needed to play
        if (activity != null && initialDataSet) {

            // check if the activity has already finished
            if (current_time.after(activity.getFinishTime())) {
                // change the state and set the finish time to that of the activity, because it means that
                // the user did not get to the end of the activity
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .update("state", ParticipationState.FINISHED,
                                "finishTime", activity.getFinishTime())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // TODO: special notification. No more time. Actually, this should be done with CLOUD FUNCTION
                                // DEBUG
                                Log.d(TAG, "Se acabó el tiempo. Terminando la actividad...");
                                //
                                stopSelf();
                            }
                        });
            } else {
                // there is still time, so we continue playing
                // DEBUG
                Log.d(TAG, "Aun queda tiempo de actividad, seguimos jugando");
                //
                if ((beacons.size() - beaconsReached.size()) < 1) {
                    // no beacons left, so we finish the activity
                    // DEBUG
                    Log.d(TAG, "No quedan balizas por alcanzar, terminar la actividad");
                    //
                    db.collection("activities").document(activity.getId())
                            .collection("participations").document(userID)
                            .update("state", ParticipationState.FINISHED,
                                    "finishTime", current_time)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // TODO: special notification. All the beacons reached
                                    // DEBUG
                                    Log.d(TAG, "Actividad terminada. Todas las balizas alcanzadas");
                                    //
                                    stopSelf();
                                }
                            });
                } else {
                    // there are still beacons to be reached, so we play
                    if (activity.isScore()) {
                        playScore(lat1, lng1, current_time);
                    } else {
                        playClassical(lat1, lng1, current_time);
                    }
                }
            }
        } else {
            // DEBUG
            Log.d(TAG, "La actividad es nula o aún no tenemos los datos iniciales, por lo que no hacemos nada");
            //
        }
    }

    private void playScore(double lat1, double lng1, Date current_time) {
        // DEBUG
        Log.d(TAG, "La actividad es Score");
        //
        // check if there is any beacon next to our current location
        for (Beacon beacon : beacons) {
            if (!beaconsReached.contains(beacon.getBeacon_id())) {
                // for each beacon not yet reached get the distance to the current position
                double lat2 = beacon.getLocation().getLatitude();
                double lng2 = beacon.getLocation().getLongitude();
                float dist = getDistance(lat1, lat2, lng1, lng2);
                if (dist <= LOCATION_PRECISION && !uploadingReach) {
                    // if we are next to a certain beacon and the service is not already uploading a reach...
                    if ((beacons.size() - beaconsReached.size()) > 1 && !beacon.isGoal()) {
                        // if we are not yet looking for the goal and we find a beacon that is not the goal...
                        // DEBUG
                        Log.d(TAG, "Aún no estamos buscando la meta, y hemos alcanzado una baliza que no es la meta");
                        //
                        BeaconReached beaconReached = new BeaconReached(current_time, beacon.getBeacon_id()); // create a new BeaconReached
                        uploadingReach = true; // uploading...
                        db.collection("activities").document(activity.getId())
                                .collection("participations").document(userID)
                                .collection("beaconReaches").document(beaconReached.getBeacon_id())
                                .set(beaconReached)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        beaconsReached.add(beacon.getBeacon_id()); // add the current beacon id to the beacons reached during the service
                                        // DEBUG
                                        Log.d(TAG, "Añadiendo la baliza " + beacon.getBeacon_id() + " al conjunto de alcanzadas " +
                                                "que ahora tiene " + beaconsReached.size() + " elementos");
                                        //
                                        uploadingReach = false; // not uploading any more
                                        sendBeaconNotification(beacon, activity);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {
                                        uploadingReach = false; // not uploading any more
                                        // don't update nextBeacon, so we will try it again in the next location update
                                    }
                                });
                    } else if ((beacons.size() - beaconsReached.size()) == 1 && beacon.isGoal()) {
                        // DEBUG
                        Log.d(TAG, "Estamos buscando la meta y la hemos encontrado");
                        //
                        BeaconReached beaconReached = new BeaconReached(current_time, beacon.getBeacon_id()); // create a new BeaconReached
                        uploadingReach = true; // uploading...
                        db.collection("activities").document(activity.getId())
                                .collection("participations").document(userID)
                                .collection("beaconReaches").document(beaconReached.getBeacon_id())
                                .set(beaconReached)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        beaconsReached.add(beacon.getBeacon_id()); // add the current beacon id to the beacons reached during the service
                                        // DEBUG
                                        Log.d(TAG, "Hemos llegado a la meta, fin de la participacion y terminar el servicio");
                                        //
                                        // ESTO DEBERÍA HACERLO UNA CLOUD FUNCTION
                                        db.collection("activities").document(activity.getId())
                                                .collection("participations").document(userID)
                                                .update("state", ParticipationState.FINISHED,
                                                        "finishTime", current_time)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // DEBUG
                                                        Log.d(TAG, "Finalización anotada. Terminando el servicio...");
                                                        //
                                                        uploadingReach = false; // not uploading any more
                                                        // TODO: special notification. All beacons reached
                                                        sendBeaconNotification(beacon, activity);
                                                        stopSelf();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull @NotNull Exception e) {
                                                        uploadingReach = false;
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {
                                        uploadingReach = false; // not uploading any more
                                        // don't update nextBeacon, so we will try it again in the next location update
                                    }
                                });
                    } else {
                        // DEBUG
                        Log.d(TAG, "Estamos cerca de la meta, pero no la estamos buscando. O eso, o algo raro pasa");
                        //
                    }
                    break;
                }
            }
        }
    }

    private void playClassical(double lat1, double lng1, Date current_time) {
        // DEBUG
        Log.d(TAG, "La actividad es clásica");
        //
        // get the beacons that we have to reach next
        int searchedBeaconIndex = beaconsReached.size();
        Beacon searchedBeacon = beacons.get(searchedBeaconIndex);
        // DEBUG
        Log.d(TAG, "Ahora mismo hay " + searchedBeaconIndex + " balizas alcanzadas " +
                "por lo tanto, la siguiente que tenemos que alcanzar es: " + searchedBeacon.getName());
        //
        double lat2 = searchedBeacon.getLocation().getLatitude();
        double lng2 = searchedBeacon.getLocation().getLongitude();
        float dist = getDistance(lat1, lat2, lng1, lng2);
        if (dist <= LOCATION_PRECISION && !uploadingReach) {
            // if we are close enough and not in the middle of an uploading operation...
            BeaconReached beaconReached = new BeaconReached(current_time, searchedBeacon.getBeacon_id()); // create a new BeaconReached
            uploadingReach = true; // uploading...
            db.collection("activities").document(activity.getId())
                    .collection("participations").document(userID)
                    .collection("beaconReaches").document(beaconReached.getBeacon_id())
                    .set(beaconReached)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            // BeaconReached added to Firestore
                            // DEBUG
                            Log.d(TAG, "Alcanzada: " + searchedBeacon.getName());
                            //
                            beaconsReached.add(searchedBeacon.getBeacon_id()); // update the set with the reaches
                            if(beaconsReached.size() == beacons.size()) {
                                // ESTO DEBERÍA HACERLO CLOUD FUNCTION
                                // we reached the goal, so the participation must finish
                                db.collection("activities").document(activity.getId())
                                        .collection("participations").document(userID)
                                        .update("state", ParticipationState.FINISHED,
                                                "finishTime", current_time)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // DEBUG
                                                Log.d(TAG, "Finalización anotada. Terminando el servicio...");
                                                //
                                                uploadingReach = false; // not uploading any more
                                                // TODO: special notification. All beacons reached
                                                sendBeaconNotification(searchedBeacon, activity);
                                                stopSelf();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull @NotNull Exception e) {
                                                uploadingReach = false;
                                            }
                                        });
                            }else {
                                // there are still beacons to be reached, so we just continue
                                uploadingReach = false; // not uploading any more
                                sendBeaconNotification(searchedBeacon, activity);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            uploadingReach = false; // not uploading any more
                            // don't update nextBeacon, so we will try it again
                        }
                    });
        }
    }

    // displays the notification of the foreground service
    private void startMyOwnForeground() {

        String ON_GOING_NOTIFICATION_CHANNEL_ID = "onGoing.orientatree";

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
                            .build();
            startForeground(1, notification);
        }
    }

    // send the notification of a regular beacon
    private void sendBeaconNotification(Beacon beacon, Activity activity) {
        // 1 create the channel if needed, and set the intent for the action
        String BEACON_NOTIFICATION_CHANNEL_ID = "beacon.orientatree"; // name of the channel for beacon notifications

        // Create an explicit intent for an Activity in your app
        //Intent intent = new Intent(this, BeaconContentActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent intent = new Intent(this, ChallengeActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("beaconID", beacon.getBeacon_id());
        //intent.putExtra("templateID", activity.getTemplate());
        intent.putExtra("activity", activity);
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

    // starts asking for location updates
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

    // stops the service from asking for new location updates
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            fusedLocationClient.removeLocationUpdates(mLocationCallback);
            stopSelf();
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    // reckons the distance between two points in meters
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
