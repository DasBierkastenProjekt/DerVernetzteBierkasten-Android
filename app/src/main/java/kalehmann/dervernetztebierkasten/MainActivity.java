package kalehmann.dervernetztebierkasten;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Runnable {

    private Button but_connect;
    private ImageView iv_flasche_leer;
    private ImageView iv_flasche;
    private Bitmap flasche_voll;
    private Bitmap flasche_leer;
    private Button bReset;
    private EditText kastenUrl;
    private SharedPreferences mPrefs;

    private final int REQUEST_IMAGE_CAPTURE = 1;
    public static final String FILE_NAME_VOLL = "voll.png";
    public static final String FILE_NAME_LEER = "leer.png";
    public static final String INTENT_EXTRA_FILE_NAME = "f_name";
    public static final String PREFERENCE_URL = "kalehmann.dervernetztebierkasten.bierkasten_url";

    @Override
    public void run() {
        int width = findViewById(R.id.preview_images).getWidth();
        width = Math.round(width * 0.9f / 2);
        iv_flasche.setImageBitmap(Bitmap.createScaledBitmap(flasche_voll, width, width, false));
        iv_flasche_leer.setImageBitmap(Bitmap.createScaledBitmap(flasche_leer,
                width, width, false));
    }

    @Override
    public void onClick(View view) {
        if (view == but_connect) {
            try {
                String uri = kastenUrl.getText().toString();
                if (!uri.startsWith("http://")) {
                    uri = "http://" + uri;
                }
                URL url = new URL(uri);
                SharedPreferences.Editor edit = mPrefs.edit();
                edit.putString(PREFERENCE_URL, uri);
                edit.commit();

                Intent intent = new Intent(this, BierkastenActivity.class);
                startActivity(intent);
            } catch (MalformedURLException e) {
                new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setTitle("Achtung")
                        .setMessage("Die Adresse is fehlerhaft :(")
                        .show();
            }


        } else if (view == bReset) {
            try {
                deleteFile(FILE_NAME_VOLL);
                deleteFile(FILE_NAME_LEER);
                flasche_leer = BitmapFactory.decodeResource(getResources(),
                        R.drawable.clipart_bierflasche_leer);
                flasche_voll = BitmapFactory.decodeResource(getResources(),
                        R.drawable.clipart_bierflasche);
                this.run();
            } catch (Exception e) { }
            kastenUrl.setText("");
            SharedPreferences.Editor edit = mPrefs.edit();
            edit.putString(PREFERENCE_URL, "");
            edit.commit();
        } else {
            String request = "";
            if (view == iv_flasche) {
                request = FILE_NAME_VOLL;
            } else if (view == iv_flasche_leer) {
                request = FILE_NAME_LEER;
            }

            Intent takePictureIntent = new Intent(this, CameraActivity.class);
            takePictureIntent.putExtra(INTENT_EXTRA_FILE_NAME, request);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        but_connect = (Button) findViewById(R.id.but_connect);
        iv_flasche = (ImageView) findViewById(R.id.iv_flasche);
        iv_flasche_leer = (ImageView) findViewById(R.id.iv_flasche_leer);

        but_connect.setOnClickListener(this);
        iv_flasche.setOnClickListener(this);
        iv_flasche_leer.setOnClickListener(this);

        flasche_voll = null;
        flasche_leer = null;

        try {
            FileInputStream is = this.openFileInput(MainActivity.FILE_NAME_LEER);
            flasche_leer = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            flasche_leer = BitmapFactory.decodeResource(getResources(),
                    R.drawable.clipart_bierflasche_leer);
        }
        try {
            FileInputStream is = this.openFileInput(MainActivity.FILE_NAME_VOLL);
            flasche_voll = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            flasche_voll = BitmapFactory.decodeResource(getResources(),
                    R.drawable.clipart_bierflasche);
        }

        findViewById(R.id.preview_images).post(this);

        bReset = (Button) findViewById(R.id.but_reset);
        bReset.setOnClickListener(this);

        kastenUrl = (EditText) findViewById(R.id.kasten_url);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        kastenUrl.setText(mPrefs.getString(PREFERENCE_URL, ""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap bmp = null;
                String f_name = data.getStringExtra(INTENT_EXTRA_FILE_NAME);
                try {
                    FileInputStream is = this.openFileInput(f_name);
                    bmp = BitmapFactory.decodeStream(is);
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (f_name.equals(FILE_NAME_VOLL)) {
                    flasche_voll.recycle();
                    flasche_voll = bmp;
                } else if (f_name.equals(FILE_NAME_LEER)) {
                    flasche_leer.recycle();
                    flasche_leer = bmp;
                }
                this.run();
            }
        }
    }
}
