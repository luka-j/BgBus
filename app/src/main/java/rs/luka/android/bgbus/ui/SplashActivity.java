package rs.luka.android.bgbus.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import rs.luka.android.bgbus.R;
import rs.luka.android.bgbus.logic.Base;

/**
 * Activity displaying splash screen and loading data in the background.
 * Created by luka on 6.11.15.
 * @see Base#load(Context)
 */
public class SplashActivity extends AppCompatActivity {

    private final AsyncTask<Void, Void, Void> DataLoader = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            Base.getInstance().load(getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SplashActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        DataLoader.execute(null, null, null);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
