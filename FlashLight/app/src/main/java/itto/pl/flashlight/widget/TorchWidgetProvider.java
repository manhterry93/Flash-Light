package itto.pl.flashlight.widget;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import itto.pl.flashlight.MainActivity;
import itto.pl.flashlight.R;

/**
 * Created by PL_itto on 2/24/2017.
 */
public class TorchWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "PL_itto.TorchWidgetProvider";
    private static CameraManager sCameraManager;
    private static String sCameraId = "";
    final public static boolean newApi = Build.VERSION.SDK_INT >= 23;
    private static final String action_click = "itto.pl.flash.Click";
    //    public static boolean newApi = false;
    public static boolean isBlink = false;

    /**
     * Low Api
     */
    Camera camera;

    @Override
    public void onReceive(Context context, Intent intent) {

//        Log.i(TAG, "onReceive: " + intent.getAction());
//        Log.i(TAG, "onReceive: Service is still running: "+isMyServiceRunning(WidgetService.class,context));
        super.onReceive(context, intent);
        if(!isMyServiceRunning(WidgetService.class,context)){
            if (newApi) {
                Intent intent1 = new Intent(context.getApplicationContext(), WidgetService.class);
                context.startService(intent1);
            }
        }
        if (intent.getAction().equals(action_click)) {
            if (newApi) {
                if (!isBlink) {
                    sCameraManager = (CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                    toggleFlashNewApi(sCameraManager, context);
                } else {
                    MainActivity.getInstance().flashHandle();
                }
            } else {
                camera = Camera.open();
                if (!isBlink) {
                    toggleFlashOldApi(camera);
                }
            }
        }
    }


    @Override
    public void onEnabled(Context context) {
//        Log.i(TAG, "onEnabled: ");
        super.onEnabled(context);
        if (newApi) {
            Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
            context.startService(intent);
        }
    }

    @Override
    public void onDisabled(Context context) {
//        Log.i(TAG, "onDisabled: ");
//        Toast.makeText(context, "Disable Service", Toast.LENGTH_SHORT).show();
        super.onDisabled(context);
        Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
        context.stopService(intent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
//        Log.i(TAG,"onUpdate");
        //Setup for new View
        if(!isMyServiceRunning(WidgetService.class,context)){
            if (newApi) {
                Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
                context.startService(intent);
            }
        }

        int[] allWidgetIds = getAllWidgetId(context, appWidgetManager);
//        Log.i(TAG, "onUpdate: " + allWidgetIds.length);
        for (int id : allWidgetIds) {
//            Log.w(TAG, "onUpdate: " + id + " " + context.getPackageName());
            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            Intent intent = new Intent(context, TorchWidgetProvider.class);
            intent.setAction(action_click);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            view.setOnClickPendingIntent(R.id.btn_switch, pendingIntent);
            appWidgetManager.updateAppWidget(id, view);
        }
        if (newApi) {
            sCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            try {
                sCameraId = sCameraManager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            updateAll(sCameraManager, context, appWidgetManager, null);
        }

    }

    /**
     * Update All for new Api
     *
     * @param cameraManager
     * @param context
     * @param appWidgetManager
     * @param handler
     */
    public static void updateAll(final CameraManager cameraManager, final Context context, final AppWidgetManager appWidgetManager, Handler handler) {
        cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                cameraManager.unregisterTorchCallback(this);
                int[] allAppIds = getAllWidgetId(context, appWidgetManager);
                for (int i : allAppIds)
                    updateState(context, enabled, appWidgetManager, i);

            }
        }, handler);
    }

    public static void updateAll(final CameraManager cameraManager, final Context context, final AppWidgetManager appWidgetManager, boolean enabled) {
        int[] allAppIds = getAllWidgetId(context, appWidgetManager);
        for (int i : allAppIds)
            updateState(context, enabled, appWidgetManager, i);

    }

    //unused
    public static void updateAll(Camera camera, Context context, final AppWidgetManager appWidgetManager) {
        Camera.Parameters parameters = camera.getParameters();
        int[] allAppIds = getAllWidgetId(context, appWidgetManager);
        boolean enabled = torchOn(camera);
        for (int id : allAppIds) {
            updateState(context, enabled, appWidgetManager, id);
        }
        Log.i(TAG, "updateAll: Flash_MODE: " + parameters.getFlashMode());
    }

    public static void updateState(Context context, boolean enabled, AppWidgetManager manager, int id) {
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
//        Log.i(TAG,"updateState: "+enabled);
        if (enabled) view.setImageViewResource(R.id.btn_switch, R.drawable.light_on);
        else view.setImageViewResource(R.id.btn_switch, R.drawable.light_off);
        manager.updateAppWidget(id, view);
    }

    public static int[] getAllWidgetId(Context context, AppWidgetManager manager) {
        ComponentName componentName = new ComponentName(context, TorchWidgetProvider.class);
        int[] allAppIds = manager.getAppWidgetIds(componentName);
        return allAppIds;
    }

    /**
     * Toggle flash on new APi
     *
     * @param manager
     * @param context
     */
    synchronized public static void toggleFlashNewApi(final CameraManager manager, final Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        try {
            sCameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        manager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
//                Log.i(TAG, "onTorchModeChanged: " + enabled);
                try {
                    if (enabled) enabled = false;
                    else enabled = true;
                    manager.unregisterTorchCallback(this);
                    manager.setTorchMode(sCameraId, enabled);
//                    updateAll(manager, context, appWidgetManager, null);
                } catch (Exception e) {
//                    Log.e(TAG, "onTorchModeChanged: " + e.toString());
                    e.printStackTrace();
                }
            }
        }, null);
    }

    synchronized public static void toggleFlashOldApi(Camera camera) {
//        Log.i(TAG, "toggleFlashOldApi: ");
        Camera.Parameters parameters = camera.getParameters();
        String mode = parameters.getFlashMode();
        if (mode == Camera.Parameters.FLASH_MODE_TORCH) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.stopPreview();
        } else if (mode == Camera.Parameters.FLASH_MODE_OFF) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.startPreview();
        }
    }

    public static boolean torchOn(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        String mode = parameters.getFlashMode();
        if (mode == Camera.Parameters.FLASH_MODE_TORCH) {
            return true;
        } else {
            if (mode == Camera.Parameters.FLASH_MODE_OFF) {
                return false;
            }
        }

        return false;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

