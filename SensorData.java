package app.lightbox.winofsql.jp.sensorkadai10;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SensorData implements SensorEventListener {
    private final static String TAG = "MySensor";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private TextView[] tv;
    private ListView list;
    int count;
    int[] savex = new int[3];
    float mean_x;

    SensorData(SensorManager sensorManager, TextView[] tv){
        this.mSensorManager = sensorManager;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.tv = tv;

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] fdata = new float[3];
        for(int i = 0; i < 3; i++){
            fdata[i] = sensorEvent.values[i];

            String s = "";
            switch (i){
                case 0:
                    s = "x =";
                    break;
                case 1:
                    s = "y =";
                    break;
                case 2:
                    s = "z =";
                    break;
            }
            s=s+String.valueOf(fdata[i]);
            //List<String> arraySensor = new ArrayList<>();
            //arraySensor.add(s[i]);
            tv[i].setText(s);
            savex[i] += fdata[i];
            for(count = 1; count <= 5; count++) {
                if (count == 5) {
                    mean_x = savex[count] / 5;
                    Log.d("mean_x", String.valueOf(mean_x));
                    break;
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startSensor(){
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG,"start SensorEventListener");
    }

    public void stopSensor(){
        mSensorManager.unregisterListener(this);
        Log.d(TAG,"stop Listener");
    }
}
