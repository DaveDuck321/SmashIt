package tk.smashr.smashit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;

public class AdvancedSmashing extends AppCompatActivity {
    public List<Boolean> rightAnswer = new ArrayList<>();
    public List<Integer> ranks = new ArrayList<>();
    public List<Integer> answers = new ArrayList<>();
    public List<Boolean> loggedIn = new ArrayList<>();

    RequestQueue queue;
    WebView kahootConsole;
    boolean queuedAnswer = false;
    boolean answerPossible = false;
    boolean launched = false;
    int gamePin;
    int currentId = -1;
    int selectedAnswer = -1;
    List<KahootHandle> kahootSmashers = new ArrayList<>();
    List<OkHttpClient> httpClients = new ArrayList<>();

    List<KahootChallenge> challenges = new ArrayList<>();

    //UI components
    Button redBtn, blueBtn, yellowBtn, greenBtn;
    TextView redText, blueText, yellowText, greenText;

    CheckBox isRandom;
    TextView joined, answered;

    Handler interval = new Handler();
    Runnable updateStatsRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_smashing);

        // Set up the toolbar immediately after inflating with content
        Toolbar toolbar = findViewById(R.id.toolbar_advanced_smashing);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.title_activity_smashing);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get UI
        redBtn = findViewById(R.id.redButton);
        blueBtn = findViewById(R.id.blueButton);
        yellowBtn = findViewById(R.id.yellowButton);
        greenBtn = findViewById(R.id.greenButton);
        redText = findViewById(R.id.redText);
        yellowText = findViewById(R.id.yellowText);
        blueText = findViewById(R.id.blueText);
        greenText = findViewById(R.id.greenText);
        isRandom = findViewById(R.id.isRandom);
        joined = findViewById(R.id.joined);
        answered = findViewById(R.id.answered);

        redBtn.setOnClickListener(v -> buttonClicked(0));
        blueBtn.setOnClickListener(v -> buttonClicked(1));
        yellowBtn.setOnClickListener(v -> buttonClicked(2));
        greenBtn.setOnClickListener(v -> buttonClicked(3));

        isRandom.setOnClickListener(v -> {
            if (queuedAnswer) {
                new Handler().post(() -> {
                    for (int i = 0; i < kahootSmashers.size(); i++) {
                        kahootSmashers.get(i).AnswerQuestion(4);
                    }
                });
            }
        });

        UpdateStats();

        updateStatsRun = () -> {
            UpdateStats();
            UpdateSelectedAnswer();
            interval.postDelayed(updateStatsRun, 30);
        };

        Bundle b = getIntent().getExtras();
        if (b != null) {
            gamePin = b.getInt("gamePin");
        }
        SmashingLogic.readAndInterpretSettings(getApplicationContext());

        queue = Volley.newRequestQueue(this);
        kahootConsole = new WebView(this);

        WebSettings settings = kahootConsole.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(true);
        settings.setDomStorageEnabled(true);

        kahootConsole.setVisibility(View.GONE);
        kahootConsole.loadUrl("https://kahoot.it");

        kahootConsole.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!launched) {
                    for (int i = 0; i < SmashingLogic.numberOfKahoots; i++) {
                        challenges.add(new KahootChallenge(gamePin, kahootConsole, queue, AdvancedSmashing.this));
                    }
                }
                launched = true;
            }
        });
        interval.postDelayed(updateStatsRun, 30);
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < kahootSmashers.size(); i++) {
            kahootSmashers.get(i).disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        for (int i = 0; i < kahootSmashers.size(); i++) {
            kahootSmashers.get(i).disconnect();
        }

        Intent startSmash = new Intent(AdvancedSmashing.this, GamePinActivity.class);
        startActivity(startSmash);
        finish();
    }

    public void buttonClicked(int index) {
        if (!answerPossible) {
            return;
        }
        isRandom.setChecked(false);
        if (selectedAnswer == index) {
            selectedAnswer = -1;
        } else {
            selectedAnswer = index;
        }
        UpdateSelectedAnswer();
        if (queuedAnswer) {
            new Handler().post(() -> {
                for (int i = 0; i < kahootSmashers.size(); i++) {
                    kahootSmashers.get(i).AnswerQuestion(4);
                }
            });
        }
    }

    private void UpdateSelectedAnswer() {
        if (answerPossible && !isRandom.isChecked()) {
            redBtn.setBackgroundResource(selectedAnswer == 0 ? R.drawable.red_answer_pressed : R.drawable.red_answer);
            blueBtn.setBackgroundResource(selectedAnswer == 1 ? R.drawable.blue_answer_pressed : R.drawable.blue_answer);
            yellowBtn.setBackgroundResource(selectedAnswer == 2 ? R.drawable.yellow_answer_pressed : R.drawable.yellow_answer);
            greenBtn.setBackgroundResource(selectedAnswer == 3 ? R.drawable.green_answer_pressed : R.drawable.green_answer);
        } else {
            redBtn.setBackgroundResource(R.drawable.red_answer_pressed);
            blueBtn.setBackgroundResource(R.drawable.blue_answer_pressed);
            yellowBtn.setBackgroundResource(R.drawable.yellow_answer_pressed);
            greenBtn.setBackgroundResource(R.drawable.green_answer_pressed);
        }
    }

    public void addToken(String token) {
        currentId++;
        if (currentId % 5 == 0) {
            httpClients.add(new OkHttpClient());
        }
        rightAnswer.add(false);
        ranks.add(100);
        answers.add(-1);
        loggedIn.add(false);
        kahootSmashers.add(new KahootHandle(gamePin, currentId, httpClients.get(httpClients.size() - 1), this, token));
    }

    public String GetName(int index) {
        return SmashingLogic.generateName(index);
    }

    public void LoggedIn(int id, boolean loggedInB) {
        loggedIn.set(id, loggedInB);
    }

    public Integer GetHighestRank() {
        int highest = 0;
        for (int i = 1; i < ranks.size(); i++) {
            if (ranks.get(i) < ranks.get(highest)) {
                highest = i;
            }
        }
        return ranks.get(highest);
    }

    public Integer GetResponse(int choices, int index) {
        if (isRandom.isChecked()) {
            int choice = (int) (Math.random() * choices);
            answers.set(index, choice);
            return choice;
        }
        if (selectedAnswer != -1) {
            answers.set(index, selectedAnswer);
            return selectedAnswer;
        }
        answers.set(index, -1);
        queuedAnswer = true;
        return -1;
    }

    public void addQuestionResult(int index, boolean correct, int rank) {
        rightAnswer.set(index, correct);
        ranks.set(index, rank);
        queuedAnswer = false;
        answerPossible = false;
        selectedAnswer = -1;
    }

    private void UpdateStats() {
        if (loggedIn.size() == 0) {
            redText.setText("0/0");
            greenText.setText("0/0");
            blueText.setText("0/0");
            yellowText.setText("0/0");
            joined.setText(String.format("%s N/A", getString(R.string.joined)));
            answered.setText(String.format("%s N/A", getString(R.string.answered)));
            return;
        }
        int loggedInCount = 0;
        for (int i = 0; i < loggedIn.size(); i++) {
            if (loggedIn.get(i)) {
                loggedInCount++;
            }
        }
        int counts[] = {0, 0, 0, 0};
        int total = 0;
        for (int i = 0; i < answers.size(); i++) {
            int answer = answers.get(i);
            if(answer!=-1) {
                counts[answer]++;
                total++;
            }
        }
        joined.setText(MessageFormat.format("{0} {1}/{2}", getString(R.string.joined), loggedInCount, loggedIn.size()));
        answered.setText(MessageFormat.format("{0} {1}/{2}", getString(R.string.answered), total, loggedIn.size()));
        redText.setText(MessageFormat.format("{0}/{1}", counts[0], loggedIn.size()));
        blueText.setText(MessageFormat.format("{0}/{1}", counts[1], loggedIn.size()));
        yellowText.setText(MessageFormat.format("{0}/{1}", counts[2], loggedIn.size()));
        greenText.setText(MessageFormat.format("{0}/{1}", counts[3], loggedIn.size()));
    }

    public void makeAnswerPossible() {
        answerPossible = true;
    }
}
