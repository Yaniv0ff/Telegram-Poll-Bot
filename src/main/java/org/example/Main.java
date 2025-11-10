package org.example;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {

        Properties props = new Properties();
        try (FileInputStream file = new FileInputStream("config.properties")) {
            props.load(file);
        } catch (IOException e) {
            System.out.println("error. pls create config.properties with your BOT_TOKEN and BOT_USERNAME");
            return;
        }
        String token = props.getProperty("BOT_TOKEN");
        String username = props.getProperty("BOT_USERNAME");
        String gptId = props.getProperty("GPT_ID");
        if (token == null || username == null || gptId == null) {
            System.out.println("pls check for valid BOT_TOKEN or BOT_USERNAME or GPT_ID in config.properties");
            return;
        }
        try {
            TelegramPollBot bot = new TelegramPollBot(token, username);
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            SwingUtilities.invokeLater(() -> {
                PollCreatorPanel panel = new PollCreatorPanel(bot, gptId);
                TelegramPollBot.creatorPanel = panel;
            });
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
