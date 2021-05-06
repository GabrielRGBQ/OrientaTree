package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FindActivityAdapter extends RecyclerView.Adapter<FindActivityAdapter.MyViewHolder> {

    private Context context;

    private ArrayList<Activity> activities;
    private int position;

    public FindActivityAdapter(Context context, ArrayList<Activity> activities) {
        this.context = context;
        this.activities = activities;
    }

    @NonNull
    @Override
    public FindActivityAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_find_activity, parent, false);
        return new FindActivityAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindActivityAdapter.MyViewHolder holder, int position) {
        this.position = position ;
        Activity activity = activities.get(position);

        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        holder.find_template_textView.setText(activity.getTemplate());
        holder.find_title_textView.setText(activity.getTitle());
        holder.find_date_textView.setText("Fecha: " + dateAsString);
        holder.visibleId_textView.setText("ID: " + activity.getVisible_id());

        if(activity.getParticipants() != null) {
            if(activity.getParticipants().contains(holder.mAuth.getCurrentUser().getUid())) {
                holder.find_subscribed_textView.setText("Inscrito");
                holder.subscribe_button.setEnabled(false);
            } else {
                holder.find_subscribed_textView.setText("No inscrito");
                holder.unsubscribe_button.setEnabled(false);
            }
        } else {
            holder.find_subscribed_textView.setText("No inscrito");
            holder.unsubscribe_button.setEnabled(false);
        }

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.find_row_imageView);

        holder.subscribe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkKeyDialog(activity, holder);
            }
        });

        holder.unsubscribe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.circularProgressIndicator.setVisibility(View.VISIBLE);
                activity.removeParticipant(holder.mAuth.getCurrentUser().getUid());
                holder.db.collection("activities").document(activity.getId())
                        .set(activity)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                holder.circularProgressIndicator.setVisibility(View.INVISIBLE);
                                holder.find_subscribed_textView.setText("No inscrito");
                                holder.subscribe_button.setEnabled(true);
                                holder.unsubscribe_button.setEnabled(false);
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseAuth mAuth;

        FirebaseStorage storage;
        StorageReference storageReference;

        FirebaseFirestore db;

        TextView find_title_textView, find_date_textView, find_template_textView, visibleId_textView, find_subscribed_textView;
        ImageView find_row_imageView;
        Button subscribe_button, unsubscribe_button;
        CircularProgressIndicator circularProgressIndicator;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            find_title_textView = itemView.findViewById(R.id.find_row_title_textView);
            find_date_textView = itemView.findViewById(R.id.find_row_date_textView);
            find_template_textView = itemView.findViewById(R.id.find_row_template_textView);
            visibleId_textView = itemView.findViewById(R.id.find_row_id_textView);
            find_subscribed_textView = itemView.findViewById(R.id.subscribed_textView);
            find_row_imageView = itemView.findViewById(R.id.find_row_imageView);
            subscribe_button = itemView.findViewById(R.id.subscribe_button);
            unsubscribe_button = itemView.findViewById(R.id.unsubscribe_button);
            circularProgressIndicator = itemView.findViewById(R.id.row_find_progress_bar);

            mAuth = FirebaseAuth.getInstance();

            db = FirebaseFirestore.getInstance();

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
        }
    }

    private void checkKeyDialog(Activity activity, @NonNull FindActivityAdapter.MyViewHolder holder) {
        final EditText input = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Introduzca la clave de acceso (6 caracteres)")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        holder.circularProgressIndicator.setVisibility(View.VISIBLE);
                        String input_key = input.getText().toString().trim();
                        if(input_key.equals(activity.getKey())) {
                            //Toast.makeText(context, "Clave correcta!!", Toast.LENGTH_SHORT).show();
                            activity.addParticipant(holder.mAuth.getCurrentUser().getUid());
                            holder.db.collection("activities").document(activity.getId())
                                    .set(activity)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            holder.circularProgressIndicator.setVisibility(View.INVISIBLE);
                                            holder.find_subscribed_textView.setText("Inscrito");
                                            holder.subscribe_button.setEnabled(false);
                                            holder.unsubscribe_button.setEnabled(true);
                                        }
                                    });
                        } else {
                            holder.circularProgressIndicator.setVisibility(View.INVISIBLE);
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Clave incorrecta")
                                    .setMessage("La clave introducida para esa actividad es incorrecta")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
