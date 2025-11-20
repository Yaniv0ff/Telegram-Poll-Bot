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

            // detect question headers
            if (line.matches("(?i)^(#+\\s*)?(question|שאלה)\\s*\\d+:?.*") || line.matches("^\\d+\\.\\s.*")) {
                // save previous question if exists
                if (currentQ != null && !opts.isEmpty()) {
                    questions.add(new Question(currentQ.trim(), new ArrayList<>(opts)));
                    opts.clear();
                }

                // remove markdown and numbering
                currentQ = line
                        .replaceFirst("(?i)^(#+\\s*)?(question|שאלה)\\s*\\d+:?\\s*", "")
                        .replaceFirst("^\\d+\\.\\s*", "")
                        .replaceAll("\\*\\*", "")
                        .trim();
                continue;
            }

            // detect options
            if (line.matches("(?i)^[-•]?[\\s]*[A-Da-dא-ד][).:\\-]?\\s+.*")) {
                String optionText = line
                        .replaceFirst("(?i)^[-•]?[\\s]*[A-Da-dא-ד][).:\\-]?\\s+", "")
                        .replaceAll("\\*\\*", "")
                        .trim();
                if (!optionText.isEmpty()) opts.add(optionText);
                continue;
            }

            // detect question continuation lines
            if (currentQ != null && !line.matches("(?i)^[-•]?[\\s]*[A-Da-dא-ד][).:\\-]?\\s+.*")) {
                if (currentQ.isEmpty()) currentQ = line.replaceAll("\\*\\*", "").trim();
                else currentQ += " " + line.replaceAll("\\*\\*", "").trim();
            }
        }

        // add last question
        if (currentQ != null && !opts.isEmpty()) {
            questions.add(new Question(currentQ.trim(), new ArrayList<>(opts)));
        }

        return questions;
    }
}
