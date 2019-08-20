package tk.smashr.smashit;

import android.util.Base64;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONException;

class KahootChallenge {
    final private WebView kahootConsole;
    final private RequestQueue queue;
    private final AdvancedSmashing parent;
    private final MetaRequest stringRequest;

    KahootChallenge(int gamepin1, WebView kahootConsoleRef, RequestQueue queue1, AdvancedSmashing parent1) {
        String gamepin = gamepin1 + "";
        this.kahootConsole = kahootConsoleRef;
        this.queue = queue1;
        this.parent = parent1;

        String getUrl = "https://kahoot.it/reserve/session/" + gamepin;
        stringRequest = new MetaRequest(com.android.volley.Request.Method.GET, getUrl, null,
                response -> {
                    try {
                        final String token = response.getJSONObject("headers").getString("x-kahoot-session-token");
                        //Removes angular check, very hacky will break
                        String challenge = response.getString("challenge");
                        challenge = challenge.replaceAll("this.angular(.)+?\\)", "false");

                        kahootConsole.evaluateJavascript(challenge, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String mask) {
                                String newMask = StringEscapeUtils.unescapeEcmaScript(mask);
                                newMask = newMask.substring(1, newMask.length() - 1);
                                byte[] base64Decoded = Base64.decode(token.getBytes(), 0);
                                for (int i = 0; i < base64Decoded.length; i++) {
                                    base64Decoded[i] ^= newMask.charAt(i % newMask.length());
                                }
                                parent.addToken(new String(base64Decoded));
                                //AdvancedSmashing.AddNewSmasher();
                            }
                        });

                    } catch (JSONException e) {
                        Log.println(Log.INFO, "Response", "Bad JSON");
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof ServerError) {
                    Log.println(Log.INFO, "Response", "500 error, retrying!");
                    queue.add(stringRequest);
                } else {
                    Log.println(Log.INFO, "Response", "That didn't work!");
                }
            }
        });
        this.queue.add(stringRequest);
    }
}