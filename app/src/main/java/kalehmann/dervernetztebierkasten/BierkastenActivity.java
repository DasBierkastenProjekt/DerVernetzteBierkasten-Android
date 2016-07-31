package kalehmann.dervernetztebierkasten;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class BierkastenActivity extends AppCompatActivity implements Runnable {

    private ArrayList<ImageView> flaschen_ivs = new ArrayList<>();
    private TableLayout tLayout;
    private RelativeLayout bierkastenContainer;
    private Bitmap imageLeer = null;
    private Bitmap imageVoll = null;
    private BackgroundThread backgroundThread;
    private int imageSize = 0;
    private SharedPreferences mPrefs;
    private MyReceiver receiver;
    private TextView tv_connection;
    public static final String INTENT_BIERDATA = "kalehmann.dervernetztebierkasten.INTENT_BIERDATA";
    public static final String EXTRA_BIERSTATE = "INTENT_STATE";
    public static final String EXTRA_BIERDATA = "EXTRA_BIERDATA";
    public static final int STATE_ERROR = -2;
    public static final int STATE_SUCCESS = 1;

    public void setBierData(String data) {
        for (int i=0; i<20; i++) {
            if (data.charAt(i) == '1') {
                flaschen_ivs.get(i).setImageBitmap(imageVoll);
            } else {
                flaschen_ivs.get(i).setImageBitmap(imageLeer);
            }
        }
    }

    public void setSuccess() {
        tv_connection.setText("Verbunden");
        tv_connection.setTextColor(Color.GREEN);
    }

    public void setError() {
        tv_connection.setText("Verbindungsfehler");
        tv_connection.setTextColor(Color.RED);
    }

    @Override
    public void run() {
        int width = bierkastenContainer.getMeasuredWidth();
        int height = bierkastenContainer.getMeasuredHeight();
        if (height > width) {
            imageSize = width / 5;
        } else {
            imageSize = height / 5;
        }

        try {
            FileInputStream is = this.openFileInput(MainActivity.FILE_NAME_LEER);
            imageLeer = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is),
                    imageSize, imageSize, false);
            is.close();
        } catch (Exception e) {
            imageLeer = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                    R.drawable.clipart_bierflasche_leer), imageSize, imageSize, false);
        }
        try {
            FileInputStream is = this.openFileInput(MainActivity.FILE_NAME_VOLL);
            imageVoll = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(is),
                    imageSize, imageSize, false);
            is.close();
        } catch (Exception e) {
            imageVoll = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),
                    R.drawable.clipart_bierflasche), imageSize, imageSize, false);
        }

        for (int i = 0; i<20 ; i++) {
            flaschen_ivs.get(i).setImageBitmap(imageVoll);
        }
    }

    private void createTable() {
        tLayout = (TableLayout) findViewById(R.id.bierkasten_table);
        bierkastenContainer = (RelativeLayout) findViewById(R.id.bierkasten_container);
        TableLayout.LayoutParams tParms = new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rParms = new TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (int i=0; i<4; i++) {
            TableRow tRow = new TableRow(this);
            tRow.setLayoutParams(tParms);
            for (int j=0; j<5; j++) {
                ImageView iv = new ImageView(this);
                flaschen_ivs.add(iv);
                iv.setLayoutParams(rParms);
                tRow.addView(iv);
            }
            tLayout.addView(tRow);
        }
        tLayout.post(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bierkasten);
        try {
            createTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String url = mPrefs.getString(MainActivity.PREFERENCE_URL, "");
        backgroundThread = new BackgroundThread(url, this);
        receiver = new MyReceiver(this);
        tv_connection = (TextView) findViewById(R.id.tv_connection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundThread = new BackgroundThread(mPrefs.getString(MainActivity.PREFERENCE_URL, ""),
                this);
        backgroundThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        backgroundThread.interrupt();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(INTENT_BIERDATA));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    private class BackgroundThread extends Thread {

        private Boolean running = true;
        private URL url;
        private LocalBroadcastManager broadcaster;

        public BackgroundThread(String uri, Context ctx) {
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            uri = uri + ":6000/get_bier_data";
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {

            }
            broadcaster = LocalBroadcastManager.getInstance(ctx);
        }

        public void run() {
            Log.w("Thread", "started");
            while (running) {
                Intent intent = new Intent(INTENT_BIERDATA);
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(500);
                    urlConnection.connect();
                    InputStream is = urlConnection.getInputStream();
                    Reader reader = new InputStreamReader(is, "UTF-8");
                    char[] buffer = new char[32];
                    reader.read(buffer);
                    String data = new String(buffer).substring(0, 20);
                    Log.w("Thread", data);
                    intent.putExtra(EXTRA_BIERSTATE, STATE_SUCCESS);
                    intent.putExtra(EXTRA_BIERDATA, data);
                } catch (IOException e) {
                    intent.putExtra(EXTRA_BIERSTATE, STATE_ERROR);
                }
                broadcaster.sendBroadcast(intent);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
            Log.w("Thread", "finished");
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        private BierkastenActivity activity;

        public MyReceiver(BierkastenActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(EXTRA_BIERSTATE, STATE_ERROR) == STATE_SUCCESS) {
                String data = intent.getStringExtra(EXTRA_BIERDATA);
                activity.setBierData(data);
                activity.setSuccess();
            } else {
                activity.setError();
            }
        }
    }
}
