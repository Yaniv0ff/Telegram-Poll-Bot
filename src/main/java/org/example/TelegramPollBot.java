package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TelegramPollBot extends TelegramLongPollingBot {

    private Map<Long, String> community = new HashMap<>(); // userId , username
    private final String BOT_USERNAME;
    private final String BOT_TOKEN;
    private Poll currentPoll;
    private boolean isPollActive = false;
    private Map<Long, String> userAnswers = new HashMap<>(); // userId , answers like "A,B,C"
    private Map<Integer, Map<String, Integer>> lastPollResultsPerQuestion = new HashMap<>();
    public static PollCreatorPanel creatorPanel;



    public TelegramPollBot(String token, String username) {
        this.BOT_TOKEN = token;
        this.BOT_USERNAME = username;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public Poll getCurrentPoll() {
        return currentPoll;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        long userId = message.getFrom().getId();
        String username = message.getFrom().getFirstName();
        String text = message.hasText() ? message.getText().trim().toLowerCase() : "";
        // join conditions
        if (text.equals("/start") || text.equals("hi") || text.equals("היי")) {
            if (!community.containsKey(userId)) {
                community.put(userId, username);
                // welcome new member
                sendMessage(userId, "Welcome to our community " + username + "!\nWere happy to have u!");
                // notify other members
                notifyCommunityAboutNewMember(userId, username);
            } else {
                sendMessage(userId, "You already joined the community.");
            }
        }
        else if (isPollActive && currentPoll != null) {
            handlePollAnswer(userId, text);
        }
        else {
            // correct msg for potential members
            sendMessage(userId, "To join the community, pls send 'היי' or 'Hi' or click /start.");
        }
    }


    private void sendMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(text);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void notifyCommunityAboutNewMember(long newMemberId, String newMemberName) {
        String message = "We have a new member!\n"
                + "Name: " + newMemberName + "\n"
                + "Current community size: " + community.size();
        for (Map.Entry<Long, String> entry : community.entrySet()) {
            long memberId = entry.getKey();
            if (memberId != newMemberId) {
                sendMessage(memberId, message);
            }
        }
    }


    public int getCommunitySize() {
        return community.size();
    }

    public Map<Long, String> getCommunity() {
        return community;
    }

    public boolean createPoll(Poll poll) {
        if (isPollActive) {
            System.out.println("Theres already an active poll, pls wait till its done");
            return false;
        }
        if (community.size() < 3) {
            System.out.println("Need at last 3 members to create a poll.");
            return false;
        }
        // clear old answers before new poll
        userAnswers.clear();
        currentPoll = poll;
        isPollActive = true;
        StringBuilder pollText = new StringBuilder("New poll created!\n\n");
        int qNum = 1;
        // print the questions
        for (Question q : poll.getQuestions()) {
            pollText.append("Q").append(qNum++).append(": ")
                    .append("*").append(q.getQuestionText()).append("*")
                    .append("\n\n");
            char optLetter = 'A';
            for (String opt : q.getOptions()) {
                pollText.append(optLetter++).append(") ").append(opt).append("\n");
            }
            pollText.append("\n");
        }
        pollText.append("pls reply with answers like (A,B,C...)\n");
        pollText.append("theres only 5 minutes to answer this survey!");

        // send to all members
        for (long id : community.keySet()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(id));
            msg.setText(pollText.toString());
            msg.setParseMode("Markdown");
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        // start timer for poll duration
        startPollTimer();
        return true;
    }


    private void handlePollAnswer(long userId, String text) {
        if (!isPollActive || currentPoll == null) return;
        // check if user answered already
        if (userAnswers.containsKey(userId)) {
            sendMessage(userId, "u already answered this survey");
            return;
        }
        text = text.trim().toUpperCase();
        if (text.isEmpty()) {
            sendMessage(userId, "empty answer. pls reply with letters like A,B,C...");
            return;
        }
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        int requiredAnswers = currentPoll.getQuestions().size();
        if (parts.length != requiredAnswers) {
            sendMessage(userId, "pls answer all questions. you must send " + requiredAnswers +
                    " letters separated by commas (example: A,B)");
            return;
        }
        // validate answers input
        List<Question> questions = currentPoll.getQuestions();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            char letter = part.charAt(0);
            Question q = questions.get(i);
            int optionCount = q.getOptionCount();
            List<Character> validLetters = new java.util.ArrayList<>();
            for (int j = 0; j < optionCount; j++) {
                validLetters.add((char) ('A' + j));
            }
            if (!validLetters.contains(letter)) {
                sendMessage(userId, "Invalid answer for Q" + (i + 1) + ": " + letter +
                        ". pls choose only from: " + validLetters);
                return;
            }
        }
        // all answers valid
        userAnswers.put(userId, text);
        sendMessage(userId, "answer received.");
        if (userAnswers.size() == community.size()) {
            closePoll();
        }
    }


    // validate answer's letters
    private List<Character> getValidOptionsForCurrentPoll() {
        List<Character> list = new java.util.ArrayList<>();
        if (currentPoll == null || currentPoll.getQuestions().isEmpty()) return list;
        int optionCount = currentPoll.getQuestions().get(0).getOptionCount();
        for (int i = 0; i < optionCount; i++) {
            list.add((char) ('A' + i));
        }
        return list;
    }


    private void startPollTimer() {
        new Thread(() -> {
            try {
                Thread.sleep(1000 * 60 * 5); // 5 min
                if (isPollActive && currentPoll != null && currentPoll.isExpired()) {
                    closePoll();
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }


    private void closePoll() {
        if (!isPollActive || currentPoll == null) return;
        isPollActive = false;
        Map<Integer, Map<String, Integer>> resultsByQuestion = new HashMap<>();
        // check all member answers
        for (String answer : userAnswers.values()) {
            answer = answer.trim().toUpperCase();
            String[] parts = answer.split(",");
            List<Question> questions = currentPoll.getQuestions();
            for (int i = 0; i < parts.length && i < questions.size(); i++) {
                String letter = parts[i].trim();
                if (letter.isEmpty()) continue;
                resultsByQuestion.putIfAbsent(i, new HashMap<>());
                Map<String, Integer> questionResults = resultsByQuestion.get(i);
                questionResults.put(letter, questionResults.getOrDefault(letter, 0) + 1);
            }
        }
        lastPollResultsPerQuestion = new HashMap<>(resultsByQuestion);
        // calc summary
        StringBuilder summary = new StringBuilder("Poll results:\n\n");
        List<Question> questions = currentPoll.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            summary.append("Q").append(i + 1).append(": ").append(q.getQuestionText()).append("\n");
            Map<String, Integer> counts = resultsByQuestion.getOrDefault(i, new HashMap<>());
            int totalVotes = counts.values().stream().mapToInt(Integer::intValue).sum();
            char letter = 'A';
            for (String option : q.getOptions()) {
                int count = counts.getOrDefault(String.valueOf(letter), 0);
                double percent = totalVotes == 0 ? 0 : (count * 100.0 / totalVotes);
                summary.append(letter).append(") ").append(option)
                        .append(" — ").append(String.format("%.1f", percent))
                        .append("% (").append(count).append(" votes)\n");
                letter++;
            }
            summary.append("\n");
        }
        // print summary to console
        System.out.println(summary);
        // send graphic summary to user
        if (creatorPanel != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                creatorPanel.showResults(null);
            });
        }
        // print summary in telegram chat
        for (long id : community.keySet()) {
            sendMessage(id, summary.toString());
        }
        currentPoll = null;
        userAnswers.clear();
    }


    public Map<Integer, Map<String, Integer>> getLastResultsPerQuestion() {
        return lastPollResultsPerQuestion == null
                ? new HashMap<>()
                : new HashMap<>(lastPollResultsPerQuestion);
    }



}

