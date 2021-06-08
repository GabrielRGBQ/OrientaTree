package com.smov.gabriel.orientatree.model;

import java.util.Comparator;
import java.util.Date;

public class Participation implements Comparator<Participation> {

    private String participant;
    private ParticipationState state = ParticipationState.NOT_YET;
    private Date startTime;
    private Date finishTime;

    public Participation() {

    }

    public Participation(String participant) {
        this.participant = participant;
    }

    public String getParticipant() {
        return participant;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }

    public ParticipationState getState() {
        return state;
    }

    public void setState(ParticipationState state) {
        this.state = state;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    @Override
    public int compare(Participation o1, Participation o2) {
        int res = 0;
        return 0;
    }
}

