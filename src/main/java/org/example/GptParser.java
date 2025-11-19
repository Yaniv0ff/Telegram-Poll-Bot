package org.example;

import java.util.ArrayList;
import java.util.List;

public class GptParser {

    public static List<Question> parseResponse(String gptResponse) {
        List<Question> questions = new ArrayList<>();
        if (gptResponse == null || gptResponse.isEmpty()) return questions;
        String[] lines = gptResponse.split("\\r?\\n");
        String currentQ = null;
        List<String> opts = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // detect question lines
            if (line.matches("(?i)^(#+\\s*)?(question|שאלה)\\s*\\d+:?.*") || line.matches("^\\d+\\.\\s.*")) {
                // save previous question if exists
                if (currentQ != null && !opts.isEmpty()) {
                    questions.add(new Question(currentQ.trim(), new ArrayList<>(opts)));
                    opts.clear();
                }
                // extract text
                if (line.contains(":")) {
                    int idx = line.indexOf(":");
                    currentQ = line.substring(idx + 1).trim();
                } else {
                    currentQ = line.replaceFirst("(?i)^(#+\\s*)?(question|שאלה)\\s*\\d+\\s*", "").trim();
                }
                continue;
            }

            // detect actual question text lines
            if (currentQ != null && !line.matches("^[A-Da-d][).].*") && !line.matches("^[אבגדה]\\.?\\s*.*")) {
                if (currentQ.isEmpty()) currentQ = line.trim();
                else currentQ += " " + line.trim();
                continue;
            }

            // detect options
            if (line.matches("^[A-Da-d][).]\\s*.*") || line.matches("^[אבגדה]\\.?\\s*.*")) {
                String optionText = line.replaceFirst("^[A-Da-d][).]\\s*|^[אבגדה]\\.?\\s*", "").trim();
                if (!optionText.isEmpty()) opts.add(optionText);
            }
        }

        // add last question if exists
        if (currentQ != null && !opts.isEmpty()) {
            questions.add(new Question(currentQ.trim(), opts));
        }

        return questions;
    }
}
