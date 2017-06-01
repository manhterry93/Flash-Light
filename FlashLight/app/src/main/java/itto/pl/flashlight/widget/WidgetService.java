package itto.pl.flashlight.widget;

import android.annotation.TargetApi;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import itto.pl.flashlight.MainActivity;

/**
 * Created by PL_itto on 2/24/2017.
 */
public class WidgetService extends Service {
    private static final String TAG = "PL_itto.WidgetService";
    public static final String ACTION_BLINK = "itto.pl.flash.BLINK";
    private static Timer timer = null;
    private static CameraManager sCameraManager;
    private static String sCameraId = "";
    private static AppWidgetManager appWidgetManager;
    private static Handler handler = new Handler();
    private static IntentFilter mBlinkFilter;
    public static boolean newApi = Build.VERSION.SDK_INT >= 23;
    private static BroadcastReceiver mBlinkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_BLINK)) {
                if (newApi) {
                    boolean blink=intent.getBooleanExtra("isBlink",false);
                    changeIconWidgetNewApi(blink, context.getApplicationContext());
                }
            }
        }
    };
    /**
     * Low Api
     */
    Camera camera;
    Camera.Parameters paramenters;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();
//        Log.i(TAG, "onCreate: StartService");
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        if (newApi) {
            sCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            try {
                sCameraId = sCameraManager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                Log.e(TAG, "onCreate: ERROR -- " + e.toString());
            }
            sCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    super.onTorchModeChanged(cameraId, enabled);
                    if (!TorchWidgetProvider.isBlink) {
//                        Log.i(TAG,"update: "+enabled);
                        TorchWidgetProvider.updateAll(sCameraManager, getApplicationContext(), appWidgetManager, enabled);
                    }
                }
            }, handler);
            mBlinkFilter = new IntentFilter();
            mBlinkFilter.addAction(ACTION_BLINK);
            registerReceiver(mBlinkReceiver, mBlinkFilter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        initTimer();
        return START_STICKY;
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initTimer() {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "run: " + TorchWidgetProvider.isBlink);
                if (!TorchWidgetProvider.isBlink) {
                    if (newApi) {
                        TorchWidgetProvider.updateAll(sCameraManager, getApplicationContext(), appWidgetManager, handler);
                    }
                } else {
                    if (newApi) {
                        changeIconWidgetNewApi(true, getApplicationContext());
                    }
                }
            }
        };
        timer.schedule(task, 0, 1000);
    }

    @Override
    public void onDestroy() {
        Log.i("PL_itto_", "onDestroy: ");
//        cancelTimer();
        unregisterReceiver(mBlinkReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void changeIconWidgetNewApi(boolean enabled, Context context) {
        int[] allWidgetIds = TorchWidgetProvider.getAllWidgetId(context, appWidgetManager);
        for (int id : allWidgetIds) {
            TorchWidgetProvider.updateState(context, enabled, appWidgetManager, id);
        }
    }

}
