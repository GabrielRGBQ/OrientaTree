package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.User;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<User> participants;
    private Activity currentActivity;
    private int position;

    public ParticipantAdapter(Context context, ArrayList<User> participants,
                              Activity currentActivity) {
        this.context = context;
        this.participants = participants;
        this.currentActivity = currentActivity;
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
        User user = participants.get(position);

        // formatting date in order to display it on card
        String pattern = "HH:mm:ss";
        DateFormat df_hour = new SimpleDateFormat(pattern);

        holder.email_textView.setText(user.getEmail());
        holder.name_textView.setText(user.getName());

        holder.db.collection("activities").document(currentActivity.getId())
                .collection("participations").document(user.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Participation participation = documentSnapshot.toObject(Participation.class);
                        if (participation != null) {
                            if (participation.getStartTime() != null) {
                                holder.start_textView.append(df_hour.format(participation.getStartTime()));
                            }
                            if (participation.getFinishTime() != null) {
                                holder.finish_textView.append(df_hour.format(participation.getFinishTime()));
                            }
                        }
                    }
                });

        StorageReference ref = holder.storageReference.child("profileImages/" + user.getId());
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.participant_circleImageView);

    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseFirestore db;

        FirebaseStorage storage;
        StorageReference storageReference;

        CircleImageView participant_circleImageView;
        TextView email_textView, name_textView, start_textView, finish_textView;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            db = FirebaseFirestore.getInstance();

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            participant_circleImageView = itemView.findViewById(R.id.participant_row_circleImageView);
            email_textView = itemView.findViewById(R.id.participantEmail_textView);
            name_textView = itemView.findViewById(R.id.participantName_textView);
            start_textView = itemView.findViewById(R.id.participantStart_row_textView);
            finish_textView = itemView.findViewById(R.id.participantFinish_row_textView);

        }
    }
}
