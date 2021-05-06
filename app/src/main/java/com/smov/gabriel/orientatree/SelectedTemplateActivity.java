package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class SelectedTemplateActivity extends AppCompatActivity {

    private LinearLayout selected_linear_layout; // needed to show snackbar

    private Toolbar toolbar;

    private CircularProgressIndicator progressIndicator;

    private ImageView selected_imageView;
    private TextView selected_overline_textView, selected_title_textView, description_textView;
    private Chip chip_date, chip_start, chip_finish;
    private Button program_button;

    private String template_id;

    private FirebaseFirestore db;

    private FirebaseAuth mAuth;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private Date chosen_day; // date that the user chooses for the activity
    private Date start_date; // date representing the start time chosen
    private Date finish_date; // date representing the finish time chosen
    // all of the following are aux variables...
    private int start_hour;
    private int start_minute;
    private int finish_hour;
    private int finish_minute;

    private Activity new_activity;

    private DateFormat df_date;
    private DateFormat df_hour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_template);

        getIntentData();

        selected_linear_layout = findViewById(R.id.selected_linear_layout);

        toolbar = findViewById(R.id.selected_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(template_id);
        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressIndicator = findViewById(R.id.programmed_progressBar);

        selected_imageView = findViewById(R.id.selected_imageView);
        selected_overline_textView = findViewById(R.id.selected_overline_textView);
        selected_title_textView = findViewById(R.id.selected_title_textView);
        description_textView = findViewById(R.id.description_textview);
        chip_date = findViewById(R.id.chip_date);
        chip_start = findViewById(R.id.chip_start);
        chip_finish = findViewById(R.id.chip_finish);
        program_button = findViewById(R.id.program_button);

        // need this to display the chosen date and hour on the chips
        String pattern_date = "dd/MM/yyyy";
        String pattern_hour = "HH:mm";
        df_date = new SimpleDateFormat(pattern_date);
        df_hour = new SimpleDateFormat(pattern_hour);

        // allow description to scroll
        description_textView.setMovementMethod(new ScrollingMovementMethod());

        db = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        StorageReference ref = storageReference.child("templateImages/" + template_id + ".jpg");

        Glide.with(this)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(selected_imageView);

        selected_title_textView.setText(template_id);

        DocumentReference docRef = db.collection("templates").document(template_id);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Template template = documentSnapshot.toObject(Template.class);
                selected_overline_textView.setText(template.getType());
                if(template.getColor() != null) {
                    switch (template.getColor()) {
                        case "Naranja":
                            selected_overline_textView.setTextColor(Color.parseColor("#FFA233"));
                            break;
                        case "Roja":
                            selected_overline_textView.setTextColor(Color.parseColor("#E32A10"));
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        MaterialDatePicker.Builder date_builder = MaterialDatePicker.Builder.datePicker();
        date_builder.setTitleText("Escoge el día");
        // need this to prevent user from choosing past days...
        CalendarConstraints.DateValidator dateValidator = new CalendarConstraints.DateValidator() {
            @Override
            public boolean isValid(long date) {
                boolean res = false;
                Date date_picker = new Date(date);
                Calendar cal_picker = Calendar.getInstance();
                cal_picker.setTime(date_picker);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.AM_PM, Calendar.AM);
                cal.set(Calendar.HOUR, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if(cal_picker.after(cal)) {
                    res = true;
                }
                return res;
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {

            }
        };
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(dateValidator);
        CalendarConstraints calendarConstraints = constraintsBuilder.build();
        //date_builder.setCalendarConstraints(calendarConstraints);
        MaterialDatePicker materialDatePicker = date_builder.build();
        materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
            @Override
            public void onPositiveButtonClick(Object selection) {
                chosen_day = new Date((long)selection);
                String dateAsString = df_date.format(chosen_day);
                chip_date.setText(dateAsString);
            }
        });

        chip_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER");
            }
        });

        chip_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosen_day != null) {
                    MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                            .setTitleText("Elige la hora de inicio")
                            .build();
                    materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            start_hour = materialTimePicker.getHour();
                            start_minute = materialTimePicker.getMinute();
                            // obtaining Date object that is stored in FireStore document
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(chosen_day);
                            cal.add(Calendar.HOUR_OF_DAY, start_hour - 2);
                            cal.add(Calendar.MINUTE, start_minute);
                            Date start_check = cal.getTime();
                            if(finish_date != null) {
                                if(finish_date.after(start_check)) {
                                    start_date = cal.getTime(); // this is the Date object
                                    String startHourAsString = df_hour.format(start_date);
                                    chip_start.setText(startHourAsString);
                                } else {
                                    showSnackBar("La hora de inicio no puede ser posterior a la hora de fin");
                                }
                            } else {
                                start_date = cal.getTime(); // this is the Date object
                                String startHourAsString = df_hour.format(start_date);
                                chip_start.setText(startHourAsString);
                            }
                        }
                    });
                    materialTimePicker.show(getSupportFragmentManager(), "TIME_PICKER");
                } else {
                    showSnackBar("Primero debes seleccionar el día");
                }
            }
        });

        chip_finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start_date != null) {
                    MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                            .setTitleText("Elige la hora de fin")
                            .build();
                    materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish_hour = materialTimePicker.getHour();
                            finish_minute = materialTimePicker.getMinute();
                            // obtaining Date object that is stored in FireStore document
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(chosen_day);
                            cal.add(Calendar.HOUR_OF_DAY, finish_hour - 2);
                            cal.add(Calendar.MINUTE, finish_minute);
                            // checking that the activity finish time is after its start time
                            Date finish_check = cal.getTime();
                            if(finish_check.after(start_date)) {
                                finish_date = cal.getTime(); // this is the Date object
                                String finishHourAsString = df_hour.format(finish_date);
                                chip_finish.setText(finishHourAsString);
                            } else {
                                showSnackBar("La hora de fin debe ser posterior a la hora de inicio");
                            }
                        }
                    });
                    materialTimePicker.show(getSupportFragmentManager(), "TIME_PICKER");
                } else {
                    if(chosen_day == null) {
                        showSnackBar("Primero debes seleccionar el día");
                    } else {
                        showSnackBar("Primero debes seleccionar la hora de inicio");
                    }
                }
            }
        });

        program_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(chosen_day != null) {
                    if(start_date != null) {
                        if(finish_date != null) {
                            showTitleDialog();
                        } else {
                            showSnackBar("Primero debes seleccionar la hora de finalización");
                        }
                    } else {
                        showSnackBar("Primero debes seleccionar la hora de inicio");
                    }
                } else {
                    showSnackBar("Primero debes seleccionar el día");
                }
            }
        });

    }

    private void showTitleDialog() {
        final EditText input = new EditText(SelectedTemplateActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Introduzca un título para su nueva actividad")
                .setMessage("Día: " + df_date.format(chosen_day) + "\n" +
                        "\nHora de inicio: " + df_hour.format(start_date) + "\n" +
                        "\nHora de fin: " + df_hour.format(finish_date) + "\n")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressIndicator.setVisibility(View.VISIBLE);
                        String activity_title = input.getText().toString().trim();
                        if(activity_title.length() == 0) {
                            activity_title = "Actividad " + mAuth.getCurrentUser().getDisplayName();
                        }
                        String aux_activity_key, activity_id, activity_key;
                        activity_id = UUID.randomUUID().toString();
                        aux_activity_key = UUID.randomUUID().toString();
                        activity_key = aux_activity_key.substring(0, Math.min(aux_activity_key.length(), 6));
                        new_activity = new Activity(activity_id, activity_key, activity_title, template_id,
                                mAuth.getCurrentUser().getUid(), start_date, finish_date);
                        db.collection("activities").document(activity_id)
                                .set(new_activity)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        showConfirmationDialog();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressIndicator.setVisibility(View.INVISIBLE);
                                        showSnackBar("Algo salió mal al crear la actividad. Vuelva a intentarlo.");
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Actividad programada")
                .setMessage(new_activity.getTitle() + "\nlas claves para participar son: \n" +
                        "\nID -> " + new_activity.getVisible_id() + "\n" +
                        "\nKey -> " + new_activity.getKey() + "\n" +
                        "\nPodrá volver a consultar estos datos cuando desee consultando sus actividades programadas.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    void getIntentData() {
        if(getIntent().hasExtra("template_id")) {
            template_id = getIntent().getStringExtra("template_id");
        } else {

        }
    }

    private void showSnackBar(String message) {
        Snackbar.make(selected_linear_layout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .setDuration(8000)
                .show();
    }
}