package tk.smashr.smashit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Objects;


public class GamePinActivity extends AppCompatActivity {
    EditText gamePin;
    RequestQueue queue;
    AlertDialog pinWrong;
    AlertDialog oldSmashing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AlertDialog.Builder badPin = new AlertDialog.Builder(this);
        badPin.setPositiveButton(getString(R.string.contin), null).setMessage(getString(R.string.badPinBody)).setTitle(getString(R.string.badPin));
        pinWrong = badPin.create();

        AlertDialog.Builder oldSmashBuilder = new AlertDialog.Builder(this);
        oldSmashBuilder.setPositiveButton(getString(R.string.contin), (dialog, which) -> {
            Intent startSmash = new Intent(GamePinActivity.this, Smashing.class);
            Bundle b = new Bundle();
            b.putInt("gamePin", Integer.parseInt(gamePin.getText().toString()));
            startSmash.putExtras(b);
            startActivity(startSmash);
        }).setNegativeButton("Settings", (dialog, which) -> {
            Intent startSmash = new Intent(GamePinActivity.this, SettingsActivity.class);
            startActivity(startSmash);
        }).setTitle(getString(R.string.oldSmash)).setMessage(getString(R.string.oldSmashInfo));
        oldSmashing = oldSmashBuilder.create();

        SmashingLogic.readAndInterpretSettings(getApplicationContext());

        queue = Volley.newRequestQueue(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_pin);

        // Set up the toolbar immediately after inflating with content
        Toolbar toolbar = findViewById(R.id.toolbar_game_pin);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);

        gamePin = findViewById(R.id.game_pin_input);

        Button enterBtn = findViewById(R.id.enter);
        enterBtn.setOnClickListener(view -> {
            if (!gamePin.getText().toString().isEmpty()) {
                StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://kahoot.it/reserve/session/" + gamePin.getText().toString(),
                        response -> {
                            VibrationUtils.shortVibrate(GamePinActivity.this);
                            switch (SmashingLogic.smashingMode) {
                                case 1:
                                case 2:
                                    //Warning of the retro
                                    oldSmashing.show();
                                    break;
                                case 0:
                                default:
                                    Intent startSmash = new Intent(GamePinActivity.this, AdvancedSmashing.class);
                                    Bundle b = new Bundle();
                                    b.putInt("gamePin", Integer.parseInt(gamePin.getText().toString()));
                                    startSmash.putExtras(b);
                                    startActivity(startSmash);
                            }
                        }, error -> {
                            //Alert
                            Log.println(Log.ERROR, "Pin", "Bad pin");
                            VibrationUtils.vibrate(50, GamePinActivity.this);
                            pinWrong.show();
                        });
                queue.add(stringRequest);
            } else {
                gamePin.setError("Please enter a Game PIN first.");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()){
            case R.id.action_settings:
                VibrationUtils.shortVibrate(this);
                startActivity(new Intent(GamePinActivity.this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
