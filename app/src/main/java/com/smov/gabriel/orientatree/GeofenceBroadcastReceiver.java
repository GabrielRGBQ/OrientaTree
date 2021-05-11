package com.smov.gabriel.orientatree;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.smov.gabriel.orientatree.helpers.NotificationHelper;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        NotificationHelper notificationHelper = new NotificationHelper(context);

        Toast.makeText(context, "Geofence triggered...", Toast.LENGTH_SHORT).show();

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event...");
            return;
        }

        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();

        for (Geofence geofence : geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }

        //Location location = geofencingEvent.getTriggeringLocation(); // location within the geofence where the user is when the event is triggered

        int transitionType = geofencingEvent.getGeofenceTransition();
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notificationHelper.sendHighPriorityNotification("ENTRANDO", "Entrado en geofence", MapActivity.class);
                Toast.makeText(context, "Entering the geofence...", Toast.LENGTH_SHORT).show();
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                notificationHelper.sendHighPriorityNotification("DENTRO", "Dentro de geofence", MapActivity.class);
                Toast.makeText(context, "Moving in the geofence...", Toast.LENGTH_SHORT).show();
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                notificationHelper.sendHighPriorityNotification("SALIENDO", "Saliendo de geofence", MapActivity.class);
                Toast.makeText(context, "Leaving the geofence...", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}