package com.smov.gabriel.orientatree.model;

import java.util.Date;

public class Participation {

    private String participant;
    private ParticipationState state = ParticipationState.NOT_YET;
    private Date startTime;
    private Date finishTime;

    public Participation() {

    }

    public Participation(String participant, Date startTime) {
        this.participant = participant;
        this.startTime = startTime;
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
}

