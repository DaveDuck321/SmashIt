package tk.smashr.smashit;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.MessageFormat;

public class Smashing extends AppCompatActivity {
    int count = 0;
    boolean hasStarted = false;
    boolean pinNeeded = false;
    boolean staying = false;
    //Alert btn
    AlertDialog pinWrong;
    AlertDialog needVerification;
    private int gamePin;
    private LinearLayout layout;
    private Boolean legacyMode = true;
    private WebView webViews[];
    private boolean hasJoined[];
    private boolean verified[];
    private String combination = "";
    private Handler handler = new Handler();
    //UI
    private TextView smashingReadout;
    private TextView verifiedReadouts;
    private TableLayout allBtn;
    private Button redButton;
    private Button yellowButton;
    private Button greenButton;
    private Button blueButton;
    private boolean hasEnded = false;

    private Runnable testSmashingNumber = new Runnable() {
        @Override
        public void run() {
            if (!hasEnded) {
                for (Integer i = 0; i < SmashingLogic.numberOfKahoots; i++) {
                    webViews[i].evaluateJavascript("isSmashing('" + i.toString() + "');", value -> {
                        value = value.substring(1, value.length() - 1);
                        if (value.charAt(0) == 'g') {
                            if (value.charAt(1) == 'v') {
                                verified[Integer.parseInt(value.substring(2))] = true;
                                hasJoined[Integer.parseInt(value.substring(2))] = true;
                            } else {
                                verified[Integer.parseInt(value.substring(1))] = false;
                                hasJoined[Integer.parseInt(value.substring(1))] = true;
                            }
                        } else {
                            verified[Integer.parseInt(value)] = false;
                            hasJoined[Integer.parseInt(value)] = false;
                        }

                        updateReadout();
                    });
                }
                handler.postDelayed(testSmashingNumber, 3000);
            }
        }
    };

    private void runJS(Integer i) {
        if (!hasEnded) {
            String tempCombanation = "";
            if (combination.length() == 4) {
                tempCombanation = combination;
            }
            webViews[i].evaluateJavascript("takeNextStep(" + gamePin + ",'" + SmashingLogic.generateName(i) + "','" + "ABCD".charAt((int) (Math.random() * 4)) + "'," + i + ",'" + tempCombanation + "')", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    value = value.substring(1, value.length() - 1);

                    if (value.charAt(0) == 'n') {
                        runJS(Integer.parseInt(value.substring(1)));
                    } else if (value.charAt(0) == 'v') {
                        if (!pinNeeded) {
                            verifiedReadouts.setVisibility(View.VISIBLE);
                            allBtn.setVisibility(View.VISIBLE);
                            pinNeeded = true;
                            needVerification.show();
                        }
                        runJS(Integer.parseInt(value.substring(1)));
                    } else if (value.charAt(0) == 'i') {
                        resetCombo();
                        runJS(Integer.parseInt(value.substring(1)));
                    } else if (value.charAt(0) == 'p') {

                        if (!staying) {
                            pinWrong.show();
                            staying = true;
                        }
                        runJS(Integer.parseInt(value.substring(1)));
                    } else {
                        Log.println(Log.ERROR, "Ran", "nooo: " + value);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        hasEnded = true;
        handler.removeCallbacks(testSmashingNumber);
        for (int i = 0; i < SmashingLogic.numberOfKahoots; i++) {
            layout.removeView(webViews[i]);
            webViews[i] = null;
        }
        Intent startSmash = new Intent(Smashing.this, GamePinActivity.class);
        startActivity(startSmash);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smashing);

        AlertDialog.Builder badPin = new AlertDialog.Builder(this);
        badPin.setPositiveButton(getString(R.string.contin), null).setNegativeButton(getString(R.string.abort), (dialog, which) -> onBackPressed()).setMessage(getString(R.string.contiuneVerify)).setTitle(getString(R.string.invalidPin));
        pinWrong = badPin.create();

        AlertDialog.Builder gameCode = new AlertDialog.Builder(this);
        gameCode.setPositiveButton(getString(R.string.contin), null).setMessage(getString(R.string.enterGameCode)).setTitle(getString(R.string.twoStepVerify));
        needVerification = gameCode.create();

        layout = findViewById(R.id.layout);
        LinearLayout.LayoutParams layoutp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        SmashingLogic.readAndInterpretSettings(getApplicationContext());
        hasJoined = new boolean[SmashingLogic.numberOfKahoots];
        verified = new boolean[SmashingLogic.numberOfKahoots];
        webViews = new WebView[SmashingLogic.numberOfKahoots];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && SmashingLogic.smashingMode != 2) {
            legacyMode = false;
        }

        Bundle b = getIntent().getExtras();
        if (b != null) {
            gamePin = b.getInt("gamePin");
        }
        for (int i = 0; i < SmashingLogic.numberOfKahoots; i++) {
            webViews[i] = new WebView(this);
            webViews[i].getSettings().setJavaScriptEnabled(true);
            webViews[i].setVisibility(View.GONE);
            webViews[i].loadUrl("https://kahoot.it");
            webViews[i].getSettings().setBlockNetworkImage(true);
            webViews[i].setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    view.getSettings().setBlockNetworkImage(false);
                    count++;
                    Log.println(Log.ERROR, "Ran", "count: " + count);
                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && count == SmashingLogic.numberOfKahoots * 2) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && count == SmashingLogic.numberOfKahoots)) {
                        handler.postDelayed(() -> {
                            hasStarted = true;
                            for (int i1 = 0; i1 < webViews.length; i1++) {
                                injectJS(webViews[i1]);
                                if (!legacyMode) {
                                    runJS(i1);
                                    handler.postDelayed(testSmashingNumber, 3000);
                                }
                            }
                            updateReadout();
                        }, 3000);
                    }
                    super.onPageFinished(view, url);
                }
            });

            layout.addView(webViews[i], layoutp);
        }

        smashingReadout = findViewById(R.id.Progress);
        verifiedReadouts = findViewById(R.id.Verified);
        verifiedReadouts.setVisibility(View.INVISIBLE);

        redButton = findViewById(R.id.redBtn);
        yellowButton = findViewById(R.id.yellowBtn);
        blueButton = findViewById(R.id.BlueBtn);
        greenButton = findViewById(R.id.GreenBtn);
        allBtn = findViewById(R.id.verifyBtns);
        View.OnClickListener orderBtn = v -> {
            if (v == redButton) {
                redButton.setEnabled(false);
                redButton.setBackgroundResource(R.drawable.red_answer_pressed);
                ButtonClicked(0);
            }
            if (v == yellowButton) {
                yellowButton.setEnabled(false);
                yellowButton.setBackgroundResource(R.drawable.yellow_answer_pressed);
                ButtonClicked(2);
            }
            if (v == greenButton) {
                greenButton.setEnabled(false);
                greenButton.setBackgroundResource(R.drawable.green_answer_pressed);
                ButtonClicked(3);
            }
            if (v == blueButton) {
                blueButton.setEnabled(false);
                blueButton.setBackgroundResource(R.drawable.blue_answer_pressed);
                ButtonClicked(1);
            }
        };
        allBtn.setVisibility(View.INVISIBLE);

        redButton.setOnClickListener(orderBtn);
        yellowButton.setOnClickListener(orderBtn);
        blueButton.setOnClickListener(orderBtn);
        greenButton.setOnClickListener(orderBtn);
        updateReadout();
    }

    private void ButtonClicked(Integer number) {
        if (combination.length() == 4) {
            return;
        }
        combination += number.toString();
        if (combination.length() == 3) {
            for (int i = 0; i < 4; i++) {
                if (!combination.contains(Integer.toString(i))) {
                    combination += Integer.toString(i);
                    break;
                }
            }
        }
    }

    private void resetCombo() {
        combination = "";

        redButton.setEnabled(true);
        redButton.setBackgroundResource(R.drawable.red_answer);

        greenButton.setEnabled(true);
        greenButton.setBackgroundResource(R.drawable.green_answer);

        yellowButton.setEnabled(true);
        yellowButton.setBackgroundResource(R.drawable.yellow_answer);

        blueButton.setEnabled(true);
        blueButton.setBackgroundResource(R.drawable.blue_answer);
    }

    private void updateReadout() {
        if (!hasStarted) {
            smashingReadout.setText(getString(R.string.loadingSmashers));
            return;
        }
        if (legacyMode) {
            smashingReadout.setText(getString(R.string.legacySmashers));
            return;
        }
        Integer numberSmashing = 0;
        for (int i = 0; i < SmashingLogic.numberOfKahoots; i++) {
            if (hasJoined[i]) {
                numberSmashing++;
            }
        }
        if (pinNeeded) {
            verifiedReadouts.setVisibility(View.VISIBLE);
            allBtn.setVisibility(View.VISIBLE);
            Integer numberVerified = 0;
            for (int i = 0; i < SmashingLogic.numberOfKahoots; i++) {
                if (verified[i]) {
                    numberVerified++;
                }
            }
            if (numberVerified.equals(SmashingLogic.numberOfKahoots)) {
                allBtn.setVisibility(View.INVISIBLE);
                verifiedReadouts.setText(getString(R.string.allVerified));
            } else {
                verifiedReadouts.setText(MessageFormat.format("{0} {1}/{2}", getString(R.string.verified), numberVerified, SmashingLogic.numberOfKahoots));
            }
        }
        if (numberSmashing.equals(SmashingLogic.numberOfKahoots)) {
            smashingReadout.setText(getString(R.string.smashingProgress));
        } else {
            smashingReadout.setText(MessageFormat.format("{0} {1}/{2}", getString(R.string.joined), numberSmashing, SmashingLogic.numberOfKahoots));
        }
    }

    private void injectJSOld(WebView webView) {
        try {
            Log.println(Log.ERROR, "Ran", "GOOD");
            webView.loadUrl("javascript:" +
                    "var linksLength = document.head.getElementsByTagName('link').length;" +
                    "for(var i=0; i<linksLength;i++){document.head.getElementsByTagName('link')[0].remove()}" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    "script.textContent = \"var namesExample=['Ben Dover','Eileen Dover','Not in ur class','Stephanie','Sportacus','Robbie Rotten','Ziggy','L0kesh;)','RealPerson.mp4','ur search history','Cael Cooper:)','Kim-Jong Uno','Sernie Banders','lorcant','Not A Bot','setup.exe','admin1'];function randomCaps(baseName){var newName = '';for(var i=0; i< baseName.length; i++){if(Math.random()>0.5){newName+=baseName[i].toUpperCase();}else{newName+=baseName[i].toLowerCase();}}return newName;};function generateRandomLetter(length){var randomLetters = '';var letters= 'qwertyuiopasdfghjklzxcvbnm1234567890';for(var i=0; i<length; i++){randomLetters += letters[Math.floor(Math.random()*letters.length)];}return randomLetters;};function generateName(mode,base){var name='';switch(mode){case '0':name = randomCaps(namesExample[Math.floor(Math.random()*namesExample.length)]);break;case '1':name = (base.substr(0,11) +'.' +generateRandomLetter(5)).substr(0,16);break;case '2':if(base.length<7){name = randomCaps(base) + '.' +generateRandomLetter(4);break;}else{name = randomCaps(base);break;}default:name = 'Smasher'+generateRandomLetter(5);}return name;};setInterval(function(){if($('#inputSession').length==1){$('#inputSession').val('" + gamePin + "');$('#inputSession').trigger('change');setTimeout(function(){$('#inputSession').submit()},10);}else if($('#username').length == 1){$('#username').val(generateName( '" + SmashingLogic.namingMethod + "', '" + SmashingLogic.baseName + "' ));$('#username').trigger('change');setTimeout(function(){$('#username').submit()},10);}else if($('.answer').length!=0){var possibleAnswers = 'ABCD';var highestAvalable = $('.answer').length;var stringToPress = '.answer.answer';stringToPress +=possibleAnswers[Math.floor(Math.random()*highestAvalable)];$(stringToPress).mousedown();}},500);\";" +
                    "document.body.appendChild(script);" +
                    "eval(script.textContent);");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void injectJS(WebView webView) {
        if (legacyMode) {
            injectJSOld(webView);
            return;
        }
        try {
            webView.loadUrl("javascript:" +
                    "var linksLength = document.head.getElementsByTagName('link').length;" +
                    "for(var i=0; i<linksLength;i++){document.head.getElementsByTagName('link')[0].remove();}" +
                    "var isInGame = false;" +
                    "var isVerified = false;" +
                    "function isSmashing(returnNumber){if(isInGame){if(isVerified){return 'gv'+returnNumber;}else{return 'g'+returnNumber;}}return returnNumber;}" +
                    "function takeNextStep(gamePin,name,answer,returnVal,verify){if($('#inputSession').length==1){isVerified=false;isInGame=false;$('#inputSession').val(gamePin);$('#inputSession').trigger('change');setTimeout(function(){$('#inputSession').submit()},10);}else if($('#username').length == 1){isInGame = true;$('#username').val(name);$('#username').trigger('change');setTimeout(function(){$('#username').submit()},10);}else if($('.two-factor-auth-sequence__card.two-factor-auth-sequence__card--0').length!=0){isVerified=false;if(verify==''){return 'v'+returnVal;}else if($('.two-factor-auth__sub-heading.ng-binding').html() != 'Match the pattern on screen'){return 'i'+returnVal;}else{for(var i=0;i<4;i++){$('.two-factor-auth-sequence__card.two-factor-auth-sequence__card--' + verify[i]).mousedown();}}} else if($('.answer').length!=0){isVerified=true;$('.answer.answer'+answer).mousedown()}else{isVerified=true;}return 'n'+returnVal;}");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}