package org.example;

import java.time.LocalDateTime;
import java.util.List;

public class Poll {
    private String title;
    private List<Question> questions;
    private LocalDateTime startTime;
    private int duration; // in min

    public Poll(String title, List<Question> questions, int duration) {
        this.title = title;
        this.questions = questions;
        this.duration = duration;
        this.startTime = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(startTime.plusMinutes(duration));
    }

    public List<Question> getQuestions() {
        return questions;
    }

}




