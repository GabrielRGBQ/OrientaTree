package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ReachAdapter extends RecyclerView.Adapter<ReachAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity reachesActivity;
    private int position;
    private String templateID;
    private ArrayList<BeaconReached> reaches;

    public ReachAdapter(android.app.Activity reachesActivity, Context context,
                        ArrayList<BeaconReached> reaches, String templateID){
        this.context = context;
        this.reachesActivity = reachesActivity;
        this.reaches = reaches;
        this.templateID = templateID;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_reach, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {

        this.position = position;
        BeaconReached reach = reaches.get(position);

        String beaconID = reach.getBeacon_id();

        // pattern to format the our at which the beacon was reached
        String pattern = "HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        holder.reachTime_textView.setText("Alcanzada: " + df.format(reach.getReachMoment()));

        // get the beacon to set the name and the number
        holder.db.collection("templates").document(templateID)
                .collection("beacons").document(beaconID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Beacon beacon = documentSnapshot.toObject(Beacon.class);
                        holder.reachTitle_textView.setText(beacon.getName());
                        if(beacon.isGoal()) {
                            holder.reachNumber_textView.setText("Meta");
                        } else {
                            holder.reachNumber_textView.setText("Baliza número " + beacon.getNumber());
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return reaches.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView reachState_textView, reachTitle_textView,
            reachNumber_textView, reachTime_textView;

        FirebaseFirestore db;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            // start Firebase services
            db = FirebaseFirestore.getInstance();

            // bind UI elements
            reachState_textView = itemView.findViewById(R.id.reachState_textView);
            reachTitle_textView = itemView.findViewById(R.id.reachTitle_textView);
            reachNumber_textView = itemView.findViewById(R.id.reachNumber_textView);
            reachTime_textView = itemView.findViewById(R.id.reachTime_textView);

        }
    }
}
