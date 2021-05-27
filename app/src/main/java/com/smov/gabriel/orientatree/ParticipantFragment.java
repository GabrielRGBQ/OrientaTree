package com.smov.gabriel.orientatree;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ParticipantAdapter;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.User;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ParticipantFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ParticipantFragment extends Fragment {

    private TextView participantBeacons_textView, participantTime_textView,
        participantStart_textView, participantFinish_textView;

    private OnGoingActivity onGoingActivity;

    private int num_beacons;
    private int beacons_reached;

    private String hour_pattern = "HH:mm:ss";
    private Format df_hour = new SimpleDateFormat(hour_pattern);

    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ParticipantFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ParticipantFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ParticipantFragment newInstance(String param1, String param2) {
        ParticipantFragment fragment = new ParticipantFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_participant, container, false);

        participantBeacons_textView = view.findViewById(R.id.participantBeacons_textView);
        participantTime_textView = view.findViewById(R.id.participantTime_textView);
        participantStart_textView = view.findViewById(R.id.participantStart_textView);
        participantFinish_textView = view.findViewById(R.id.participantFinish_textView);

        onGoingActivity = (OnGoingActivity) getActivity();

        if(onGoingActivity.participation != null) {
            Date current_time = new Date(System.currentTimeMillis());
            if(onGoingActivity.participation.getStartTime() != null) {
                Date start_time = onGoingActivity.participation.getStartTime();
                participantStart_textView.setText(df_hour.format(start_time));
                if(onGoingActivity.participation.getState() == ParticipationState.NOW) {
                    // taking part now, so we display the current timing
                    long diff = Math.abs(start_time.getTime() - current_time.getTime()) / 1000;
                    time = (double) diff;
                    // set the timer
                    if(time < 86400) { // the maximum time it can display is 23:59:59...
                        timer = new Timer();
                        timerTask = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                onGoingActivity.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        time++;
                                        participantTime_textView.setText(getTimerText());
                                    }
                                });
                            }

                        };
                        timer.scheduleAtFixedRate(timerTask, 0 ,1000);
                    }
                } else if (onGoingActivity.participation.getState() == ParticipationState.FINISHED) {
                    // participation finished, so we show the total time (static, not counting)
                    if(onGoingActivity.participation.getFinishTime() != null) {
                        Date finish_time = onGoingActivity.participation.getFinishTime();
                        participantFinish_textView.setText(df_hour.format(finish_time));
                        long diff = Math.abs(start_time.getTime() - finish_time.getTime()) / 1000;
                        double total_time = (double) diff;
                        participantTime_textView.setText(getTimerText(total_time));
                    }
                }
            }
        }

        // realtime listener to the beaconReaches in order to set when a new beacon has been reached
        if(onGoingActivity.template != null) {
            if(onGoingActivity.template.getBeacons() != null) {
                num_beacons = onGoingActivity.template.getBeacons().size();
                onGoingActivity.db.collection("activities").document(onGoingActivity.activity.getId())
                        .collection("participations").document(onGoingActivity.userID)
                        .collection("beaconReaches")
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value,
                                                @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    return;
                                }
                                beacons_reached = value.size();
                                participantBeacons_textView.setText(beacons_reached + "/" + num_beacons);
                            }
                        });
            }
        }

        return view;
    }

    // used when we need a timer because the participation is not finished
    private String getTimerText() {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    // used when the activity is already finished, and we do not need a timer any more
    private String getTimerText(double time) {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return formatTime(seconds, minutes, hours);
    }

    private String formatTime(int seconds, int minutes, int hours) {
        return String.format("%02d",hours) + ":" + String.format("%02d",minutes) + ":" + String.format("%02d",seconds);
    }
}