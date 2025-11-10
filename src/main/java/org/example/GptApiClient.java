package org.example;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;

public class GptApiClient {

    private static final String BASE_URL = "https://app.seker.live/fm1/";
    private final String userId;
    private String lastResponse = "";

    public GptApiClient(String userId) {
        this.userId = userId;
    }

    // send msg to chatGPT
    public void sendMessage(String text) {
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "send-message")
                    .queryString("id", userId)
                    .queryString("text", text)
                    .asString();
            JSONObject json = new JSONObject(response.getBody());
            lastResponse = json.getString("extra");
            System.out.println("GPT response: " + lastResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLastResponse() {
        return lastResponse;
    }

    // delete previous chat history
    public void clearHistory() {
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "clear-history")
                    .queryString("id", userId)
                    .asString();
            JSONObject json = new JSONObject(response.getBody());
            System.out.println("History cleared successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // remaining msgs left for user
    public void checkBalance() {
        try {
            HttpResponse<String> response = Unirest.get(BASE_URL + "check-balance")
                    .queryString("id", userId)
                    .asString();
            JSONObject json = new JSONObject(response.getBody());
            System.out.println("Remaining messages left: " + json.getInt("balance"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
