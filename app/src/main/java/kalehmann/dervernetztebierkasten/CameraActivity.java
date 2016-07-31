package kalehmann.dervernetztebierkasten;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import java.io.FileOutputStream;

public class CameraActivity extends Activity implements Runnable, Camera.PictureCallback,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private Camera mCamera;
    private CameraPreview mPreview;
    private String f_name;
    private Button bCapture;
    private float circle_size = 0.8f;
    private SeekBar mSeekbar;

    @Override
    public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
        circle_size = progress / 100.0f;
        this.generateOverlay();
    }

    @Override
    public void onStopTrackingTouch (SeekBar seekBar) {

    }

    @Override
    public void onStartTrackingTouch (SeekBar seekBar) {

    }

    @Override
    public void onClick(View view) {
        if (view == bCapture) {
            mCamera.takePicture(null, null, this);
        }
    }

    protected Bitmap getSubBitmap(Bitmap bm) {
        /* This method returns a squared bitmap from the center of bm. The size of the new bitmap
        *  will be the shorter side of the old bitmap * circle_size. */

        int[] old_size = {bm.getWidth(), bm.getHeight()};
        // Image is in landscape at this point
        int[] new_size;
        if (old_size[0] > old_size[1]) {
            // Landscape
            new_size = new int[] {Math.round(old_size[1] * circle_size),
                    Math.round(old_size[1] * circle_size)};
        } else {
            // Portrait
            new_size = new int[] {Math.round(old_size[0] * circle_size),
                    Math.round(old_size[0] * circle_size)};
        }
        int[] offset = {(old_size[0] - new_size[0]) / 2, (old_size[1] - new_size[1]) / 2};

        return Bitmap.createBitmap(bm, offset[0], offset[1], new_size[0], new_size[1]);
    }

    protected Bitmap getCircleImage(Bitmap bm) {
        /* This method returns a circled picture from a square picture */
        int size = bm.getWidth();
        Bitmap circled_picture = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circled_picture);
        int color = Color.RED;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bm, rect, rect, paint);

        return circled_picture;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        int size = 256;

        Bitmap raw_picture = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap sub_picture = this.getSubBitmap(raw_picture);
        Bitmap small_picture = Bitmap.createScaledBitmap(sub_picture, size, size, false);
        Bitmap circled_picture = getCircleImage(small_picture);

        // Rotate the image by 90 degrees, because the camera always takes landscape pictures.
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap final_img = Bitmap.createBitmap(circled_picture, 0, 0, size, size, matrix, true);

        // Recycle all bitmaps
        circled_picture.recycle();
        small_picture.recycle();
        sub_picture.recycle();
        raw_picture.recycle();

        try {
            //Write file
            FileOutputStream stream = this.openFileOutput(f_name, Context.MODE_PRIVATE);
            final_img.compress(Bitmap.CompressFormat.PNG, 100, stream);

            //Cleanup
            stream.close();
            final_img.recycle();

            //Pop intent
            Intent intent = new Intent();
            intent.putExtra(MainActivity.INTENT_EXTRA_FILE_NAME, f_name);
            setResult(RESULT_OK, intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void generateOverlay() {
        int x = mPreview.getWidth();
        int y = mPreview.getHeight();

        Bitmap overlay = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        overlay.eraseColor(Color.argb(100, 255, 255, 255));

        float radius = x * circle_size / 2.0f;
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        Canvas c = new Canvas(overlay);
        c.drawCircle(x / 2.0f, y / 2.0f, radius, clearPaint);

        ((ImageView) findViewById(R.id.overlay)).setImageBitmap(overlay);
    }


    @Override
    public void run() {
        generateOverlay();
    }

    public Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Intent intent = getIntent();
        f_name = intent.getStringExtra("f_name");

        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        Camera.Parameters mCameraParameters = mCamera.getParameters();
        mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(mCameraParameters);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        mPreview.post(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        bCapture = (Button) findViewById(R.id.button_capture);
        bCapture.setOnClickListener(this);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mSeekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.release();
        }
    }

}
