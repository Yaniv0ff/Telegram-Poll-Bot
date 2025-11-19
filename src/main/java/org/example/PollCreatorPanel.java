package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class PollCreatorPanel extends JFrame {


    private TelegramPollBot bot;
    private List<QuestionPanel> questionPanels;
    private JTextField delayField;
    private PollScheduler scheduler;
    private JTextField gptTopicField;
    private GptApiClient gptClient;
    private JComboBox<Integer> numQuestionsBox;
    private JComboBox<Integer> numAnswersBox;



    public PollCreatorPanel(TelegramPollBot bot, String gptId) {
        this.bot = bot;
        this.questionPanels = new ArrayList<>();
        this.scheduler = new PollScheduler(bot);
        this.gptClient = new GptApiClient(gptId);

        setTitle("Survey Creator");
        setSize(800, 960);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // title
        JLabel titleLabel = new JLabel("Create a New Survey", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        // gpt panel
        JPanel gptPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        TitledBorder gptBorder = BorderFactory.createTitledBorder("Automatic GPT Poll (optional)");
        gptBorder.setTitleFont(new Font("Arial", Font.BOLD, 18));
        gptPanel.setBorder(gptBorder);
        gptPanel.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel topicLabel = new JLabel("GPT Topic:");
        gptPanel.add(topicLabel);
        topicLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gptTopicField = new JTextField();
        gptPanel.add(gptTopicField);

        // question amount
        JLabel qLabel = new JLabel("Number of questions:");
        gptPanel.add(qLabel);
        qLabel.setFont(new Font("Arial", Font.BOLD, 16));
        numQuestionsBox = new JComboBox<>(new Integer[]{1, 2, 3});
        numQuestionsBox.setSelectedIndex(0); // default= 1 question
        gptPanel.add(numQuestionsBox);

        // answers amount
        JLabel answerLabel = new JLabel("Answers per question:");
        gptPanel.add(answerLabel);
        answerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        numAnswersBox = new JComboBox<>(new Integer[]{2, 3, 4});
        numAnswersBox.setSelectedIndex(2); // default= 4 answers
        gptPanel.add(numAnswersBox);
        add(gptPanel, BorderLayout.NORTH);

        // delay by minutes
        JLabel delayLabel = new JLabel("Delay (minutes):");
        gptPanel.add(delayLabel);
        delayLabel.setFont(new Font("Arial", Font.BOLD, 16));
        delayField = new JTextField("0");
        gptPanel.add(delayField);

        // manual question insertion
        JPanel questionsContainer = new JPanel();
        questionsContainer.setLayout(new BoxLayout(questionsContainer, BoxLayout.Y_AXIS));
        for (int i = 1; i <= 3; i++) {
            QuestionPanel qp = new QuestionPanel(i);
            questionPanels.add(qp);
            questionsContainer.add(Box.createVerticalStrut(10));
            questionsContainer.add(qp);
        }
        JScrollPane scrollPane = new JScrollPane(questionsContainer);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // send poll button
        JButton sendButton = new JButton("Send Poll");
        sendButton.setFont(new Font("Arial", Font.BOLD, 16));
        sendButton.addActionListener(this::sendSurvey);

        // clear button
        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 16));
        clearButton.addActionListener(e1 -> clearAllFields());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomPanel.add(sendButton);
        bottomPanel.add(clearButton);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);

    }


    private void sendSurvey(ActionEvent e) {
        // check community size first
        if (bot.getCommunitySize() < 3) {
            JOptionPane.showMessageDialog(this,
                    "Need at least 3 members to send a survey.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String topic = gptTopicField.getText().trim();

        // check if user asked chatGPT
        if (!topic.isEmpty()) {
            int numQuestions = (Integer) numQuestionsBox.getSelectedItem();
            int numAnswers = (Integer) numAnswersBox.getSelectedItem();
            String prompt = "Create " + numQuestions + " poll questions about: " + topic +
                    ". Each question should have " + numAnswers + " possible answers.";
            gptClient.sendMessage(prompt);
            String gptResponse = gptClient.getLastResponse();
            JOptionPane.showMessageDialog(this,
                    "GPT generated the following questions:\n\n" + gptResponse,
                    "GPT Response", JOptionPane.INFORMATION_MESSAGE);

            // parse GPT response into Question objects
            List<Question> gptQuestions = GptParser.parseResponse(gptResponse);
            if (gptQuestions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "GPT response could not be parsed into valid questions.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // delay poll (also for GPT polls)
            int delayMinutes = 0;
            try {
                String delayText = delayField.getText().trim();
                if (delayText.contains(".")) {
                    delayMinutes = (int) Math.round(Double.parseDouble(delayText));
                } else {
                    delayMinutes = Integer.parseInt(delayText);
                }
                if (delayMinutes < 0) delayMinutes = 0;
            } catch (NumberFormatException ex) {
                delayMinutes = 0;
            }

            Poll poll = new Poll("GPT Poll", gptQuestions, 5);
            if (delayMinutes == 0) {
                boolean success = bot.createPoll(poll);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "GPT Poll sent successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "error. pls wait for the current survey to complete",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                scheduler.schedulePoll(poll, delayMinutes);
                JOptionPane.showMessageDialog(this,
                        "GPT Poll will be sent out in " + delayMinutes + " minutes",
                        "Scheduled", JOptionPane.INFORMATION_MESSAGE);
            }

            return;
        }

        // manual survey creation
        List<Question> questions = new ArrayList<>();
        for (QuestionPanel qp : questionPanels) {
            String qText = qp.questionField.getText().trim();
            List<String> filledOptions = new ArrayList<>();
            for (JTextField optField : qp.optionFields) {
                String opt = optField.getText().trim();
                if (!opt.isEmpty()) filledOptions.add(opt);
            }
            // check if text fields are empty
            if (qText.isEmpty() && filledOptions.isEmpty()) {
                continue;
            }
            if (qText.isEmpty() && !filledOptions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "One of your questions has answers but no question text.\nPlease fill in the question text.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // check answer's count
            if (!qText.isEmpty() && (filledOptions.size() < 2 || filledOptions.size() > 4)) {
                JOptionPane.showMessageDialog(this,
                        "Each question must have between 2–4 answers.\nPlease fix all invalid questions before sending.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // add valid questions to list
            if (!qText.isEmpty()) {
                questions.add(new Question(qText, filledOptions));
            }
        }
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter at least 1 valid question (each with 2–4 answers).",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // delay poll
        int delayMinutes = 0;
        try {
            String delayText = delayField.getText().trim();
            if (delayText.contains(".")) {
                delayMinutes = (int) Math.round(Double.parseDouble(delayText));
            } else {
                delayMinutes = Integer.parseInt(delayText);
            }
            if (delayMinutes < 0) delayMinutes = 0;
        } catch (NumberFormatException ex) {
            delayMinutes = 0;
        }

        Poll poll = new Poll("Survey", questions, 5);
        if (delayMinutes == 0) {
            boolean success = bot.createPoll(poll);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Survey sent successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "error. pls wait for the current survey to complete",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            scheduler.schedulePoll(poll, delayMinutes);
            JOptionPane.showMessageDialog(this,
                    "Survey will sent out in " + delayMinutes + " minutes",
                    "Scheduled", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    // clear all manual question fields
    private void clearAllFields() {
        for (QuestionPanel qp : questionPanels) {
            qp.clearFields();
        }
        gptTopicField.setText("");
        delayField.setText("0");
        numQuestionsBox.setSelectedIndex(0);
        numAnswersBox.setSelectedIndex(2);
        JOptionPane.showMessageDialog(this,
                "All fields cleared. Ready to create a new survey!",
                "Cleared", JOptionPane.INFORMATION_MESSAGE);
    }


    public void showResults(ActionEvent e) {
        Map<Integer, Map<String, Integer>> results = bot.getLastResultsPerQuestion();
        if (results == null || results.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No results available yet.",
                    "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // get results from current poll
        Poll current = bot.getCurrentPoll();
        List<Question> questions = (current == null) ? new ArrayList<>() : current.getQuestions();
        StringBuilder sb = new StringBuilder("Survey Results:\n\n");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("Q").append(i + 1).append(": ").append(q.getQuestionText()).append("\n");
            Map<String, Integer> questionResults = results.getOrDefault(i, new HashMap<>());
            int total = questionResults.values().stream().mapToInt(Integer::intValue).sum();
            char letter = 'A';
            for (String option : q.getOptions()) {
                int count = questionResults.getOrDefault(String.valueOf(letter), 0);
                double percent = total == 0 ? 0 : (count * 100.0 / total);
                sb.append(letter).append(") ").append(option)
                        .append(" — ").append(String.format("%.1f", percent))
                        .append("% (").append(count).append(" votes)\n");
                letter++;
            }
            sb.append("\n");
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(480, 400));
        JOptionPane.showMessageDialog(this, scroll,
                "Survey Results", JOptionPane.INFORMATION_MESSAGE);
    }



    // secondary class for question representation
    private static class QuestionPanel extends JPanel {
        private JTextField questionField;
        private List<JTextField> optionFields;

        public QuestionPanel(int number) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            TitledBorder qBorder = BorderFactory.createTitledBorder("Question " + number);
            qBorder.setTitleFont(new Font("Arial", Font.BOLD, 16));
            setBorder(qBorder);

            Font labelFont = new Font("Arial", Font.PLAIN, 16);
            Font fieldFont = new Font("Arial", Font.PLAIN, 16);

            // question row
            JLabel questionLabel = new JLabel("Question text:");
            questionLabel.setFont(labelFont);
            add(questionLabel);
            questionField = new JTextField();
            questionField.setFont(fieldFont);
            questionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            add(questionField);
            add(Box.createVerticalStrut(8));

            // answers row
            JLabel answersLabel = new JLabel("Answer options (2–4):");
            answersLabel.setFont(labelFont);
            add(answersLabel);
            add(Box.createVerticalStrut(5));

            // answers containers
            optionFields = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                JTextField opt = new JTextField();
                opt.setFont(fieldFont);
                opt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
                optionFields.add(opt);
                add(opt);
                add(Box.createVerticalStrut(5));
            }
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBackground(new Color(248, 248, 248));
            setOpaque(true);
        }


        public Question toQuestion() {
            List<String> filledOptions = new ArrayList<>();
            for (JTextField field : optionFields) {
                String text = field.getText().trim();
                if (!text.isEmpty()) filledOptions.add(text);
            }
            String qText = questionField.getText().trim();
            // check if any fields are empty
            if (qText.isEmpty() && filledOptions.isEmpty())
                return null;
            // check if any question fields are empty
            if (qText.isEmpty() && !filledOptions.isEmpty())
                return null;

            return new Question(qText, filledOptions);
        }


        public void clearFields() {
            questionField.setText("");
            for (JTextField opt : optionFields) {
                opt.setText("");
            }
        }


    }



}


