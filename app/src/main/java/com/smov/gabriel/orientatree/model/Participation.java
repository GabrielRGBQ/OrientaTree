package com.smov.gabriel.orientatree.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class Participation implements Comparator<Participation>, Serializable {

    private String participant;
    private ParticipationState state = ParticipationState.NOT_YET;
    private Date startTime;
    private Date finishTime;
    private boolean completed;
    private ArrayList<BeaconReached> reaches;

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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ArrayList<BeaconReached> getReaches() {
        return reaches;
    }

    public void setReaches(ArrayList<BeaconReached> reaches) {
        this.reaches = reaches;
    }

    @Override
    public int compare(Participation o1, Participation o2) {
        if (o1.isCompleted() && !o2.isCompleted()) {
            return -1;
        } else if (!o1.isCompleted() && o2.isCompleted()) {
            return 1;
        } else if (o1.isCompleted() && o2.isCompleted()) {
            // both are completed, so we look for the total time
            if(o1.startTime != null && o1.finishTime != null
                && o2.startTime != null && o1.finishTime != null) {
                long o1_total = Math.abs(o1.startTime.getTime() - o1.finishTime.getTime());
                long o2_total = Math.abs(o2.startTime.getTime() - o2.finishTime.getTime());
                if(o1_total < o2_total) {
                    return -1;
                } else if(o1_total > o2_total) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                // both are completed but something rare happened with their times
                // this shouldn't be normal. We return 0
                return 0;
            }
        } else {
            // non of them are completed, so we look for the number of reaches
            if(o1.getReaches().size() > o2.getReaches().size()){
                return -1;
            } else if(o1.getReaches().size() < o2.getReaches().size()) {
                return 1;
            } else {
                // both have the same number of reaches, check if they both started
                if(o1.getStartTime() != null && o2.getStartTime() == null) {
                    return -1;
                } else if(o1.getStartTime() == null && o2.getStartTime() != null) {
                    return 1;
                } else if(o1.getStartTime() != null && o2.getStartTime() != null) {
                    // both of them started, check if they both finished
                    if(o1.getFinishTime() != null && o2.getFinishTime() == null) {
                        return -1;
                    } else if(o1.getFinishTime() == null && o2.getFinishTime() != null) {
                        return 1;
                    } else if(o1.getFinishTime() != null && o2.getFinishTime() != null) {
                        // both of them started and finished, check the total time
                        long o1_total = Math.abs(o1.startTime.getTime() - o1.finishTime.getTime());
                        long o2_total = Math.abs(o2.startTime.getTime() - o2.finishTime.getTime());
                        if(o1_total < o2_total) {
                            return -1;
                        } else if(o1_total > o2_total) {
                            return 1;
                        } else {
                            return 0;
                        }
                    } else {
                        // both started but none of them finished
                        return 0;
                    }
                } else {
                    // non of them even started
                    return 0;
                }
            }
        }
    }
}

