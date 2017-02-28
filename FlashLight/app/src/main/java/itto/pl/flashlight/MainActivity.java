package itto.pl.flashlight;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import itto.pl.flashlight.about.InfoActivity;
import itto.pl.flashlight.widget.TorchWidgetProvider;
import itto.pl.flashlight.widget.WidgetService;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "PL_itto_MainActivity";
    public final static String ACTION_BLINK_OFF = "itto.pl.flash.BLINK_OFF";
    public static boolean newApi = Build.VERSION.SDK_INT >= 23;

    String android_id = "";
//    public static boolean newApi = false;
    /**
     * parameter for camera new API
     */
    CameraManager cameraManager;
    String cameraId;
    /**
     * parameter for camera low API
     */
    public static Camera camera = null;
    Camera.Parameters parameters;
    /**
     * parameter for Application
     */

    AdView adview;
    AdRequest request;
    ImageButton btn;
    RelativeLayout layout_push;
    ToggleButton btn_sound_enable;
    TextView txt_state;
    RelativeLayout layout_blink_time;
    EditText edit_interval;
    static boolean hasFlash = false;
    private boolean isTurnOn = false, on_light = false;
    static boolean grantPermission, current_sound = true;
    static final String[] perrmission = {Manifest.permission.CAMERA};

    static Timer timer = null;
    public static List<String> list_mode = new ArrayList<>();
    Spinner spinner;
    SpinAdapter adapter;
    static MediaPlayer sPlayer;
    Intent mBlinkIntent;
    private static int interval = 300;

    public enum Mode {
        NONE,
        NORMAL, BLINK,
    }

    static MainActivity instance;

    Mode sCurrentMode = Mode.NORMAL;


    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        instance = this;
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
//        blinkIntent = new Intent();
//        blinkIntent.setAction(WidgetService.ACTION_BLINK);


        //Init View
        setContentView(R.layout.activity_main);

        //Check if device has Flash or not
        checkFlashExist();
        if (hasFlash) {
            if (newApi) {
                initListMode();
                initSound();
                if (checkPermisisons()) {
                    grantPermission = true;
                    initCamera();
                } else {
                    Log.i(TAG, "Request Permission");
                    ActivityCompat.requestPermissions(this, perrmission, 100);
                }
            } else {
                grantPermission = true;
                initListMode();
                initSound();
                initCamera();
            }

        }
//        MobileAds.initialize(this);
        txt_state = (TextView) findViewById(R.id.txt_state);
        layout_push = (RelativeLayout) findViewById(R.id.layout_push);
        btn = (ImageButton) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashHandle();
            }
        });

        spinner = (Spinner) findViewById(R.id.spin_mode);
        adapter = new SpinAdapter(getApplicationContext(), list_mode);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        sCurrentMode = Mode.NORMAL;
                        layout_blink_time.setVisibility(View.GONE);
                        break;
                    case 1:
                        layout_blink_time.setVisibility(View.VISIBLE);
                        sCurrentMode = Mode.BLINK;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sCurrentMode = Mode.NONE;
            }
        });
        btn_sound_enable = (ToggleButton) findViewById(R.id.switch_sound);
        btn_sound_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    current_sound = true;
                } else current_sound = false;
            }
        });


        layout_blink_time = (RelativeLayout) findViewById(R.id.layout_blink_time);
        edit_interval = (EditText) findViewById(R.id.edit_interval);
        edit_interval.clearFocus();


        //register a broadcast to turn off blink mode
//        registerReceiver(blinkOffReceiver, new IntentFilter(ACTION_BLINK_OFF)
//        );

        adview = (AdView) findViewById(R.id.ads_banner);
        request = new AdRequest.Builder().build();

        adview.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                Log.i(TAG, "onAdClosed: ");
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int i) {
                Log.i(TAG, "onAdFailedToLoad: " + i);
                super.onAdFailedToLoad(i);
            }

            @Override
            public void onAdLeftApplication() {
                Log.i(TAG, "onAdLeftApplication: ");
                super.onAdLeftApplication();
            }

            @Override
            public void onAdOpened() {
                Log.i(TAG, "onAdOpened: ");
                super.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                Log.i(TAG, "onAdLoaded: ");
                super.onAdLoaded();
                Log.i(TAG, "onAdLoaded: adview is shown: " + adview.isShown());
            }
        });
        adview.loadAd(request);

    }

    public void initCamera() {
        if (newApi) {

            if (hasFlash) {
                //Init Camera
                cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    cameraId = cameraManager.getCameraIdList()[0];
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            camera = Camera.open();
//            openCamera(camera);
        }


    }

    public void initListMode() {
        list_mode.clear();
        String[] list = getResources().getStringArray(R.array.modes);
        for (String i : list)
            list_mode.add(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    grantPermission = true;
//                    initListMode();
//                    initSound();
                    initCamera();
                } else {
                    grantPermission = false;
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show();
                }
        }
    }

    public boolean checkPermisisons() {
        for (String per : perrmission) {
            if (ContextCompat.checkSelfPermission(this, per) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }


    void turnOnFlash() {
        if (newApi) {
            try {
                cameraManager.setTorchMode(cameraId, true);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
            camera.startPreview();
        }
    }

    void turnOffFlash() {
        if (newApi) {
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
            camera.stopPreview();
        }
    }

    void checkFlashExist() {
        hasFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash)
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.camera_not_support), Toast.LENGTH_SHORT).show();
    }

    public void stopBlink() {
        TorchWidgetProvider.isBlink = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        turnOffFlash();
    }

    public void startBlink() {
        TorchWidgetProvider.isBlink = true;
        String text = edit_interval.getText().toString();
        if (!text.equals("")) {
            interval = Integer.parseInt(edit_interval.getText().toString());
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.interval_require), Toast.LENGTH_SHORT).show();
            interval = 300;
            edit_interval.setText("300");
        }

        timer = new Timer();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                if (!on_light) {
//                    on_light = true;
//                    MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            turnOnFlash();
//                        }
//                    });
//                } else {
//                    on_light = false;
//                    MainActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            turnOffFlash();
//                        }
//                    });
//                }
//
//            }
//        };
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!on_light) {
                    on_light = true;
                    MainActivity.getInstance().turnOnFlash();
                } else {
                    on_light = false;
                    MainActivity.getInstance().turnOffFlash();
                }

            }
        };
        timer.schedule(task, 100, interval);

    }

    public void initSound() {
        try {
            sPlayer = new MediaPlayer();
            sPlayer.reset();
            AssetFileDescriptor descriptor = getAssets().openFd("switch_flick.mp3");
            sPlayer.setDataSource(descriptor.getFileDescriptor());
            descriptor.close();
            sPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Play sound every change state
    synchronized public void playSound() {
        if (sPlayer.isPlaying()) {
            sPlayer.pause();
            sPlayer.seekTo(0);
        }
        sPlayer.start();
    }

    @Override
    protected void onDestroy() {
        // All flash when exit
        if (sPlayer.isPlaying()) {
            sPlayer.stop();
        }
        sPlayer.release();
        if (sCurrentMode == Mode.BLINK) stopBlink();
        else turnOffFlash();

        if (!newApi) camera.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (sCurrentMode != Mode.BLINK) {
            if (newApi && hasFlash && grantPermission) {
                cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
//                    super.onTorchModeChanged(cameraId, enabled);
                        cameraManager.unregisterTorchCallback(this);
                        if (enabled) {
                            isTurnOn = true;
                            txt_state.setText(getResources().getString(R.string.on));
                            btn.setImageResource(R.drawable.light_on);
                        } else {
                            isTurnOn = false;
                            txt_state.setText(getResources().getString(R.string.off));
                            btn.setImageResource(R.drawable.light_off);
                        }

                    }
                }, new Handler());
            }


        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            Intent i = new Intent(this, InfoActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    public void flashHandle() {
        long time = System.currentTimeMillis();
        if (hasFlash) {
            if (grantPermission) {
                if (isTurnOn) {
                    switch (sCurrentMode) {
                        case NORMAL:
                            turnOffFlash();
                            break;
                        case BLINK:
                            stopBlink();
//                            sendBlinkBroadcast(false);
                            break;
                    }
                    isTurnOn = false;
                    btn.setImageResource(R.drawable.light_off);
                    txt_state.setText(getResources().getString(R.string.off));
                    if (current_sound) playSound();
                } else {
                    switch (sCurrentMode) {
                        case NORMAL:
                            turnOnFlash();
                            break;
                        case BLINK:
                            startBlink();
                            sendBlinkBroadcast(true);
                            break;
                    }
                    isTurnOn = true;
                    btn.setImageResource(R.drawable.light_on);
                    txt_state.setText(getResources().getString(R.string.on));
                    if (current_sound) playSound();
                }
            } else {
                Toast.makeText(getApplicationContext(), "This App required Camera Permission to work properly", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Your device does not support Flash", Toast.LENGTH_SHORT).show();
        }

        Log.i(TAG, "flashHandle: " + (System.currentTimeMillis() - time));
    }

    public static void openCamera(Camera camera) {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        try {
            camera = Camera.open(0);
        } catch (Exception e) {
            Log.e(TAG, "openCamera: " + e.toString());
        }
    }

    private void sendBlinkBroadcast(boolean isBlink) {
        mBlinkIntent = new Intent();
        mBlinkIntent.setAction(WidgetService.ACTION_BLINK);
        mBlinkIntent.putExtra("isBlink", isBlink);
        sendBroadcast(mBlinkIntent);
    }
}
