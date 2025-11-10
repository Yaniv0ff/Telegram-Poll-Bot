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
            // detect question
            if (rawLine.matches("^\\d+\\.\\s.*") && !rawLine.startsWith(" ")) {
                // save previous question if exists
                if (currentQ != null && !opts.isEmpty()) {
                    questions.add(new Question(currentQ, new ArrayList<>(opts)));
                    opts.clear();
                }
                currentQ = line.replaceFirst("^\\d+\\.\\s*", "").trim();
            }
            // detect answer
            else if (rawLine.matches("^\\s+\\d+\\.\\s.*") || rawLine.matches("^\\d+\\.\\s.*")) {
                String optionText = line.replaceFirst("^\\d+\\.\\s*", "").trim();
                if (!optionText.isEmpty()) opts.add(optionText);
            }
        }
        // add last question if exists
        if (currentQ != null && !opts.isEmpty()) {
            questions.add(new Question(currentQ, opts));
        }

        return questions;
    }
}
