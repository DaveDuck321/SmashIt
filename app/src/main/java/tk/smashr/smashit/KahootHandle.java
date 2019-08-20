package tk.smashr.smashit;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

class KahootHandle {
    final private int smasherIndex;
    final private OkHttpClient httpClient;
    private String gamePin;
    private WebSocket client;
    private String clientId = "";
    private Integer currentMessageId = 2;
    private boolean connected = false;
    private boolean receivedQuestion = false;
    //Unideal
    private AdvancedSmashing parent;

    KahootHandle(int gamePin1, int smasherIndex, OkHttpClient httpClient, AdvancedSmashing parent1, String token) {
        gamePin = gamePin1 + "";
        this.smasherIndex = smasherIndex;
        this.httpClient = httpClient;
        this.parent = parent1;
        connectClient(token);
    }

    void disconnect() {
        sendDisconnectMessage();
    }

    private void connectClient(String rawToken) {
        Request request = new Request.Builder().url("wss://kahoot.it/cometd/" + gamePin + "/" + rawToken).addHeader("Cookie", "no.mobitroll.session=" + gamePin).addHeader("Origin", "https://kahoot.it").build();
        kahootListener listner = new kahootListener();
        client = httpClient.newWebSocket(request, listner);
    }

    void AnswerQuestion(int maxChoice) {
        int choice = parent.GetResponse(maxChoice, smasherIndex);
        if (choice != -1) {
            currentMessageId++;
            client.send("[{\"channel\":\"/service/controller\",\"data\":{\"id\":45,\"type\":\"message\",\"gameid\":" + gamePin + ",\"host\":\"kahoot.it\",\"content\":\"{\\\"choice\\\":" + choice + ",\\\"meta\\\":{\\\"lag\\\":64,\\\"device\\\":{\\\"userAgent\\\":\\\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36\\\",\\\"screen\\\":{\\\"width\\\":1920,\\\"height\\\":1040}}}}\"},\"id\":\"" + (currentMessageId - 1) + "\",\"clientId\":\"" + clientId + "\"}]");
            parent.answers.set(smasherIndex, choice);
        }
    }

    private void sendMessage(JSONObject message, String channel) {
        try {
            message.put("id", currentMessageId.toString());
            currentMessageId++;
            message.put("channel", channel);
            if (!clientId.equals("")) {
                message.put("clientId", clientId);
            }

            client.send(("[" + message.toString() + "]").replace("\\", ""));
        } catch (JSONException e) {
            //Bad JSON
            Log.e("JSON", "Bad JSON!");
        }
    }

    private void sendLoginInfo() {
        try {
            JSONObject message = new JSONObject("{\"data\":{\"type\":\"login\",\"gameid\":" + gamePin + ",\"host\":\"kahoot.it\",\"name\":\"" + parent.GetName(smasherIndex) + "\"}}");
            sendMessage(message, "/service/controller");
        } catch (JSONException e) {
            //Bad JSON
            Log.e("JSON", "Bad JSON!");
        }
    }

    private void sendSubscription(String subscribeTo, boolean subscribe) {
        try {
            JSONObject message = new JSONObject();
            message.put("subscription", subscribeTo);
            sendMessage(message, subscribe ? "/meta/subscribe" : "/meta/unsubscribe");
        } catch (JSONException e) {
            //Bad JSON
            Log.e("JSON", "Bad JSON!");
        }
    }

    private void sendConnectMessage() {
        currentMessageId++;
        client.send("[{\"channel\":\"/meta/connect\",\"connectionType\":\"websocket\",\"id\":\"" + (currentMessageId - 1) + "\",\"clientId\":\"" + clientId + "\"}]");
    }

    private void sendDisconnectMessage() {
        currentMessageId++;
        client.send("[{\"channel\":\"/meta/disconnect\",\"connectionType\":\"websocket\",\"id\":\"" + (currentMessageId - 1) + "\",\"clientId\":\"" + clientId + "\"}]");
        httpClient.dispatcher().executorService().shutdown();
    }


    private void handshake(JSONObject message) {
        try {
            clientId = message.getString("clientId");
            //sendSubscription("/service/controller", true);
            //sendSubscription("/service/player", true);
            //sendSubscription("/service/status", true);
            currentMessageId++;
            client.send("[{\"channel\":\"/meta/connect\",\"connectionType\":\"websocket\",\"advice\":{\"timeout\":0},\"id\":\"" + (currentMessageId - 1) + "\",\"clientId\":\"" + clientId + "\"}]");
        } catch (JSONException e) {
            //Bad JSON
            Log.e("JSON", "Bad JSON!");
        }
    }

    private void connect(JSONObject message) {
        try {
            message.getJSONObject("advice");
            Log.e("Timeout", message.getInt("timeout") + "");
        } catch (JSONException e) {
            sendConnectMessage();
            if(!connected) {
                sendLoginInfo();
                connected = true;
            }

        }
    }

    private void player(JSONObject message) {
        try {
            JSONObject data = new JSONObject(message.getJSONObject("data").getString("content"));//.replace("[","\"[").replace("]","]\""));

            if (data.has("questionIndex")) {
                if (receivedQuestion) {
                    receivedQuestion = false;
                    parent.makeAnswerPossible();
                    JSONArray possibleAnswers = data.getJSONArray("quizQuestionAnswers");
                    AnswerQuestion(possibleAnswers.getInt(data.getInt("questionIndex")));
                } else {
                    parent.answers.set(smasherIndex, -1);
                    receivedQuestion = true;
                }
            } else if (data.has("isCorrect")) {
                parent.addQuestionResult(smasherIndex, data.getBoolean("isCorrect"), data.getInt("rank"));
                //parent.scores.set(smasherIndex,data.getInt("totalPointsWithoutBonuses"));
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), e.toString());
        }
    }

    private void controller(JSONObject message) {
        try {
            if (message.has("successful")) {
                //Nothing special
                return;
            }
            JSONObject data = message.getJSONObject("data");
            if (data.getString("type").equals("loginResponse")) {

                if (data.has("error")) {
                    Log.d("Bad name","Retying");
                    sendLoginInfo();
                } else {
                    //parent.AddNewSmasher();
                    parent.LoggedIn(smasherIndex, true);
                }
            }

        } catch (JSONException e) {
            //Bad JSON
            Log.e("JSON", "Bad JSON!");
        }
    }

    private final class kahootListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket websocket, Response response) {
            client.send("[{\"version\":\"1.0\",\"minimumVersion\":\"1.0\",\"channel\":\"/meta/handshake\",\"supportedConnectionTypes\":[\"websocket\",\"long-polling\"],\"advice\":{\"timeout\":60000,\"interval\":0},\"id\":\"1\"}]");
        }

        @Override
        public void onMessage(WebSocket websocket, String message) {
            message = message.substring(1, message.length() - 1);
            //Log.println(Log.INFO, "Message",String.format("Got string message! %s: ", message));
            try {
                JSONObject jsonMessage = new JSONObject(message);
                switch (jsonMessage.getString("channel")) {
                    case "/meta/handshake":
                        handshake(jsonMessage);
                        break;
                    case "/meta/subscribe":
                        Log.e("Subscribe received", message);
                        break;
                    case "/meta/connect":
                        connect(jsonMessage);
                        break;
                    case "/meta/unsubscribe":
                        Log.e("Unsubscribe received", message);
                        break;
                    case "/service/player":
                        player(jsonMessage);
                        break;
                    case "/service/controller":
                        controller(jsonMessage);
                        break;
                    default:
                        Log.e("Bad channel", jsonMessage.getString("channel"));
                }
            } catch (JSONException e) {
                Log.e("Bad Json", message);
            }
        }
    }
}
