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

    private AdvancedSmashing parent;

    KahootHandle(int gamePin, int smasherIndex, OkHttpClient httpClient, AdvancedSmashing parent, String token) {
        this.gamePin = gamePin + "";
        this.smasherIndex = smasherIndex;
        this.httpClient = httpClient;
        this.parent = parent;
        connectClient(token);
    }

    void disconnect() {
        try {
            sendDisconnectMessage();
        } catch (JSONException e) {
            Log.e("Disconnect failed", "Json error");
        }
    }

    private void connectClient(String rawToken) {
        Request request = new Request.Builder().url("wss://kahoot.it/cometd/" + gamePin + "/" + rawToken).addHeader("Origin", "https://kahoot.it").build();
        kahootListener listener = new kahootListener();
        client = httpClient.newWebSocket(request, listener);
    }

    void AnswerQuestion(int maxChoice) {
        int choice = parent.GetResponse(maxChoice, smasherIndex);
        if (choice != -1) {
            try {
                JSONObject choiceJson = new JSONObject();
                choiceJson.put("choice", choice);

                JSONObject dataJson = new JSONObject();
                dataJson.put("id", 45); //This has changed before
                dataJson.put("type", "message");
                dataJson.put("gameid", Integer.parseInt(gamePin));
                dataJson.put("host", "kahoot.it");
                dataJson.put("content", choiceJson.toString());

                JSONObject messageJson = new JSONObject();
                messageJson.put("data", dataJson);
                sendMessage(messageJson, "/service/controller");
            } catch (JSONException e) {
                Log.e("Answer failed", "Json error");
            }
            parent.answers.set(smasherIndex, choice);
        }
    }

    private void sendMessage(JSONObject message, String channel) throws JSONException {
        message.put("id", currentMessageId.toString());
        currentMessageId++;
        message.put("channel", channel);
        if (!clientId.equals("")) {
            message.put("clientId", clientId);
        }

        client.send(("[" + message.toString() + "]").replace("\\/", "/"));
    }

    private void sendLoginInfo() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("type", "login");
        data.put("gameid", gamePin);
        data.put("host", "kahoot.it");
        data.put("name", parent.GetName(smasherIndex));

        JSONObject message = new JSONObject();
        message.put("data", data);
        sendMessage(message, "/service/controller");
    }

    private void sendConnectMessage() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("connectionType", "websocket");
        sendMessage(json, "/meta/connect");
    }

    private void sendDisconnectMessage() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("connectionType", "websocket");
        sendMessage(json, "/meta/disconnect");

        httpClient.dispatcher().executorService().shutdown();
    }


    private void handshake(JSONObject message) throws JSONException {
        clientId = message.getString("clientId");
        sendConnectMessage();
    }

    private void connect(JSONObject message) throws JSONException {
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

    private void player(JSONObject message) throws JSONException {
        JSONObject data = new JSONObject(message.getJSONObject("data").getString("content"));

        if (data.has("questionIndex")) {
            if (receivedQuestion) {
                receivedQuestion = false;
                parent.makeAnswerPossible();
                JSONArray possibleAnswers = data.getJSONArray("quizQuestionAnswers");
                AnswerQuestion(possibleAnswers.getInt(data.getInt("questionIndex")));
                return;
            }
            parent.answers.set(smasherIndex, -1);
            receivedQuestion = true;
        } else if (data.has("isCorrect")) {
            parent.addQuestionResult(smasherIndex, data.getBoolean("isCorrect"), data.getInt("rank"));
        }
    }

    private void controller(JSONObject message) throws JSONException {
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
                    case "/meta/connect":
                        connect(jsonMessage);
                        break;
                    case "/service/player":
                        player(jsonMessage);
                        break;
                    case "/service/controller":
                        controller(jsonMessage);
                        break;
                    case "/service/status":
                        //Nothing important (yet)
                        break;
                    default:
                        Log.e("Bad channel", jsonMessage.getString("channel"));
                        Log.e("Message", message);
                }
            } catch (JSONException e) {
                Log.e("Bad Json", message);
            }
        }
    }
}
