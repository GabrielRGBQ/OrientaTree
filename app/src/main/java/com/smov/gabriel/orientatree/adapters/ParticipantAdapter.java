package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.ReachesActivity;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.User;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity participantsListActivity;
    private ArrayList<Participation> participants;
    private int position;
    private Template template;
    private Activity activity;

    public ParticipantAdapter(android.app.Activity pActivity, Context context, ArrayList<Participation> participants,
                              Template template, Activity activity) {
        this.context = context;
        this.participants = participants;
        this.template = template;
        this.activity = activity;
        this.participantsListActivity = pActivity;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_participant, parent, false);
        return new ParticipantAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {

        this.position = position ;

        Participation participation = participants.get(position);
        String userID = participation.getParticipant();

        FirebaseUser current_user = holder.mAuth.getCurrentUser();
        String current_userID = current_user.getUid();

        // formatting date in order to display it on card
        String pattern = "HH:mm:ss";
        DateFormat df_hour = new SimpleDateFormat(pattern);

        holder.db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null) {
                            holder.email_textView.setText(user.getEmail());
                            holder.name_textView.setText(user.getName());
                            if (participation.getStartTime() != null) {
                                holder.start_textView.append(df_hour.format(participation.getStartTime()));
                            }
                            if (participation.getFinishTime() != null) {
                                holder.finish_textView.append(df_hour.format(participation.getFinishTime()));
                            }
                            StorageReference ref = holder.storageReference.child("profileImages/" + user.getId());
                            Glide.with(context)
                                    .load(ref)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                                    .skipMemoryCache(true) // prevent caching
                                    .into(holder.participant_circleImageView);
                        }
                    }
                });

        holder.row_participant_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches(userID, current_userID);
            }
        });

    }

    // update UI only if we are the participant, or we are the organizer of the activity
    private void updateUIReaches(String participantID, String current_userID) {
        if(template != null && activity != null
                && (participantID.equals(current_userID)
                    || current_userID.equals(activity.getPlanner_id()))) {
            Intent intent = new Intent(context, ReachesActivity.class);
            intent.putExtra("activity", activity);
            intent.putExtra("template", template);
            intent.putExtra("participantID", participantID);
            participantsListActivity.startActivityForResult(intent, 1);
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseFirestore db;

        FirebaseAuth mAuth;

        FirebaseStorage storage;
        StorageReference storageReference;

        LinearLayout row_participant_layout;

        CircleImageView participant_circleImageView;
        TextView email_textView, name_textView, start_textView, finish_textView;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            db = FirebaseFirestore.getInstance();

            mAuth = FirebaseAuth.getInstance();

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            participant_circleImageView = itemView.findViewById(R.id.participant_row_circleImageView);
            email_textView = itemView.findViewById(R.id.participantEmail_textView);
            name_textView = itemView.findViewById(R.id.participantName_textView);
            start_textView = itemView.findViewById(R.id.participantStart_row_textView);
            finish_textView = itemView.findViewById(R.id.participantFinish_row_textView);
            row_participant_layout = itemView.findViewById(R.id.row_participant_layout);
        }
    }
}
