package com.smov.gabriel.orientatree;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChallengeTextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChallengeTextFragment extends Fragment {

    private ChallengeActivity ca;

    private String right_answer;
    private String given_answer;

    private TextInputLayout challengeAnswer_textInputLayout;
    private Button challengeText_button;
    private CircularProgressIndicator challengeText_progressIndicator;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChallengeTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChallengeTextFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChallengeTextFragment newInstance(String param1, String param2) {
        ChallengeTextFragment fragment = new ChallengeTextFragment();
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
        View view = inflater.inflate(R.layout.fragment_challenge_text, container, false);

        ca = (ChallengeActivity) getActivity();

        // get the right answer to the question
        right_answer = ca.beacon.getWritten_right_answer();

        // binding the view elements
        challengeAnswer_textInputLayout = view.findViewById(R.id.challengeAnswer_textInputLayout);
        challengeText_button = view.findViewById(R.id.challengeText_button);
        challengeText_progressIndicator = view.findViewById(R.id.challengeText_progressIndicator);

        // button listener
        challengeText_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Envío de respuesta")
                        .setMessage("¿Desea enviar su respuesta?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                given_answer = challengeAnswer_textInputLayout.getEditText().getText().toString().trim();
                                if(given_answer.length() == 0) {
                                    challengeAnswer_textInputLayout.setError("No se puede dejar este campo vacío");
                                    challengeAnswer_textInputLayout.setErrorEnabled(true);
                                } else {
                                    challengeAnswer_textInputLayout.setErrorEnabled(false);
                                    //challengeText_progressIndicator.setVisibility(View.VISIBLE);
                                }
                            }
                        })
                        .show();
            }
        });

        return view;
    }
}