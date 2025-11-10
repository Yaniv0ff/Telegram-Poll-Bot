package org.example;

import java.util.Timer;
import java.util.TimerTask;

public class PollScheduler {


    private TelegramPollBot bot;
    private Timer activeTimer;


    public PollScheduler(TelegramPollBot bot) {
        this.bot = bot;
    }

    // send poll with delay
    public void schedulePoll(Poll poll, int delayMinutes) {
        if (bot.getCommunitySize() < 3) {
            System.out.println("Cannot schedule poll. need at least 3 members.");
            return;
        }
        if (delayMinutes <= 0) {
            System.out.println("delay is 0 or negative, sending poll immediately");
            bot.createPoll(poll);
            return;
        }
        System.out.println("Poll scheduled to send in " + delayMinutes + " minute(s)...");
        if (activeTimer != null) {
            activeTimer.cancel();
        }
        activeTimer = new Timer();
        activeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Sending poll now!");
                bot.createPoll(poll);
                activeTimer.cancel();
                activeTimer = null;
            }
        }, delayMinutes * 60L * 1000L);
    }



}
