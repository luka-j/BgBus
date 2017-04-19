package rs.luka.android.bgbus.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;
import rs.luka.android.bgbus.logic.Pathfinder;

/*
 * Original entry point. Unused.
 */

public class MainActivity extends AppCompatActivity {

    private final AsyncTask<Void, Void, Void> DataLoader = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            Base.getInstance().load(getApplicationContext());
            return null;
        }
    };

    private Toolbar  toolbar;
    private EditText start;
    private EditText goal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataLoader.execute(null, null, null);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        start = (EditText) findViewById(R.id.start_input);
        start.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (start.getText().toString().isEmpty())
                        Base.getInstance().clearStart();
                    else
                        Base.getInstance().setStart(Integer.parseInt(start.getText().toString()));
                }
            }
        });
        goal  = (EditText) findViewById(R.id.goal_input);
        goal.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    startActivity(new Intent(MainActivity.this, ResultsActivity.class));
                    return true;
                }
                return false;
            }
        });
        goal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                Pathfinder.setGoal(s.toString(), getApplicationContext());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(start!=null && !start.getText().toString().isEmpty()) {
            Base.getInstance().setStart(Integer.parseInt(start.getText().toString()));
        }
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if(id==R.id.action_maps) {
            startActivity(new Intent(this, MapsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}
