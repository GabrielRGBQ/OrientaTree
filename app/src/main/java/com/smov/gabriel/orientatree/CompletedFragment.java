package com.smov.gabriel.orientatree;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.TestAdapter;
import com.smov.gabriel.orientatree.model.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CompletedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CompletedFragment extends Fragment {

    private RecyclerView completed_recyclerView;
    private TestAdapter testAdapter;
    private ArrayList<Test> tests;

    private HomeActivity homeActivity;

    private ConstraintLayout no_activities_layout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CompletedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CompletedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CompletedFragment newInstance(String param1, String param2) {
        CompletedFragment fragment = new CompletedFragment();
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
        View view = inflater.inflate(R.layout.fragment_completed, container, false);

        homeActivity = (HomeActivity)getActivity();

        no_activities_layout = view.findViewById(R.id.completed_empty_layout);

        tests = new ArrayList<>();

        long millis=System.currentTimeMillis();
        Date date = new Date(millis );

        homeActivity.db.collection("tests")
                //.whereEqualTo("year", 2020)
                .whereLessThan("finishTime", date)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Test test = document.toObject(Test.class);
                            tests.add(test);
                        }
                        if(tests.size() < 1) {
                            no_activities_layout.setVisibility(View.VISIBLE);
                        } else {
                            no_activities_layout.setVisibility(View.GONE);
                        }
                        testAdapter = new TestAdapter(getContext(), tests);
                        completed_recyclerView = view.findViewById(R.id.completed_recyclerView);
                        completed_recyclerView.setAdapter(testAdapter);
                        completed_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    }
                });

        return view;
    }
}