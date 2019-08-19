package tk.smashr.smashit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.text.MessageFormat;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    Spinner namingMethod;
    Spinner smashingMethod;
    TextView exampleName;
    EditText baseName;
    TextView title5;
    EditText number;
    RelativeLayout numberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar toolbar = Objects.requireNonNull(getSupportActionBar());
        toolbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setDisplayShowHomeEnabled(true);

        numberInput = findViewById(R.id.numberLayout);

        namingMethod = findViewById(R.id.namingMethod);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.NamingMethods, android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        namingMethod.setAdapter(adapter2);


        smashingMethod = findViewById(R.id.smashingMode);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.SmashingMethods, android.R.layout.simple_spinner_dropdown_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        smashingMethod.setAdapter(adapter3);


        namingMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SmashingLogic.namingMethod = (int) id;
                baseNameVisible(SmashingLogic.namingMethod == 0);
                updateExample();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                SmashingLogic.namingMethod = 0;
                baseNameVisible(true);
                namingMethod.setSelection(0);
                updateExample();
            }
        });

        smashingMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SmashingLogic.smashingMode = (int) id;
                updateNumber();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                SmashingLogic.smashingMode = 0;
                smashingMethod.setSelection(0);
                updateNumber();
            }
        });

        Button newExample = findViewById(R.id.newExample);
        newExample.setOnClickListener(view -> {
            VibrationUtils.shortVibrate(SettingsActivity.this);
            updateExample();
        });

        exampleName = findViewById(R.id.exampleName);
        SmashingLogic.readAndInterpretSettings(getApplicationContext());

        title5 = findViewById(R.id.textView5);
        baseName = findViewById(R.id.namingStart);
        baseName.setText(SmashingLogic.baseName);
        baseNameVisible(SmashingLogic.namingMethod != 0);
        baseName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                SmashingLogic.baseName = s.toString();
                updateExample();
            }
        });

        number = findViewById(R.id.number);
        number.setText(MessageFormat.format("{0}", SmashingLogic.numberOfKahoots));
        number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //if(!canSmash&&!number.getText().toString().isEmpty() && Integer.parseInt(number.getText().toString())>35) {
                //    tooMany.show();
                //}
                //if(!number.getText().toString().isEmpty() && Integer.parseInt(number.getText().toString())>200)
                //{
                //    number.setText("200");
                //    maxReached.show();
                //}
                limitNumberInput();
            }
        });
        namingMethod.setSelection(SmashingLogic.namingMethod);
        smashingMethod.setSelection(SmashingLogic.smashingMode);
        updateExample();
    }

    private void updateExample() {
        exampleName.setText(String.format("%s %s", getString(R.string.example), SmashingLogic.generateName((int) (Math.random() * 100))));
    }

    private void updateNumber() {
        //if (SmashingLogic.smashingMode==1)
        //{
        //    numberInput.setVisibility(View.GONE);
        //}
        //else
        //{
        //    numberInput.setVisibility(View.VISIBLE);
        //}
        limitNumberInput();
    }

    private void limitNumberInput() {
        if (number.getText().toString().isEmpty()) {
            return;
        }
        switch (SmashingLogic.smashingMode) {
            //CHANGE THIS TO INCREASE SMASHER NUMBER
            case 0:
                if (Integer.parseInt(number.getText().toString()) > 100) {
                    number.setText("100");
                    //Warning, I limited it to 100 kahoots for a reason
                }
                break;
            default:
                if (Integer.parseInt(number.getText().toString()) > 50) {
                    number.setText("50");
                }
        }
    }

    private void baseNameVisible(Boolean hide) {
        title5.setVisibility(hide ? View.GONE : View.VISIBLE);
        baseName.setVisibility(hide ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (number.getText().length() == 0) {
            number.setText("50");
        }
        SmashingLogic.numberOfKahoots = Integer.parseInt(number.getText().toString());
        SmashingLogic.saveToFile(getApplicationContext());
        super.onBackPressed();
    }
}