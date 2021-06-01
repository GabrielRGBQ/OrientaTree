package com.smov.gabriel.orientatree.model;

import java.util.Date;

public class BeaconReached {

    private Date reachMoment;
    private String beacon_id;
    private int quiz_answer;
    private String written_answer;
    private boolean answer_right;

    public BeaconReached () {

    }

    public BeaconReached(Date reachMoment, String beacon_id) {
        this.reachMoment = reachMoment;
        this.beacon_id = beacon_id;
    }

    public BeaconReached(Date reachMoment, String beacon_id, int quiz_answer, String written_answer, boolean answer_right) {
        this.reachMoment = reachMoment;
        this.beacon_id = beacon_id;
        this.quiz_answer = quiz_answer;
        this.written_answer = written_answer;
        this.answer_right = answer_right;
    }

    public Date getReachMoment() {
        return reachMoment;
    }

    public void setReachMoment(Date reachMoment) {
        this.reachMoment = reachMoment;
    }

    public String getBeacon_id() {
        return beacon_id;
    }

    public void setBeacon_id(String beacon_id) {
        this.beacon_id = beacon_id;
    }

    public int getQuiz_answer() {
        return quiz_answer;
    }

    public void setQuiz_answer(int quiz_answer) {
        this.quiz_answer = quiz_answer;
    }

    public String getWritten_answer() {
        return written_answer;
    }

    public void setWritten_answer(String written_answer) {
        this.written_answer = written_answer;
    }

    public boolean isAnswer_right() {
        return answer_right;
    }

    public void setAnswer_right(boolean answer_right) {
        this.answer_right = answer_right;
    }
}
