
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ListView list;
    private SensorManager mSensorManager;
    private TextView[] tv = new TextView[4];
    SensorData sd;
    private DownloadTask dt;
    int count;
    float mean_x;
    private static final String TAG = "DbSample";
    SQLiteDatabase db;
    MyDbHelper dbHelper;
    private Button insbt;
    private Button delbt;
    private Button sendbt;
    private Button delAllbt;
    SimpleAdapter simpleAdapter;
    private List<Map<String, String>> itmes;
    private int rowId = 0;
    private boolean delflg=false;
    private Sensor mAccelerometer;
    float sensorX = 0;
    float sensorY = 0;
    float sensorZ = 0;
    float senX = 0;
    float senY = 0;
    float senZ = 0;
    float cnt = 0;
    float heikin = 0;

    //int[] savex = new int[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv[0] = (TextView)findViewById(R.id.data_x);
        tv[1] = (TextView)findViewById(R.id.data_y);
        tv[2] = (TextView)findViewById(R.id.data_z);
        tv[3] = (TextView)findViewById(R.id.date);
        list = (ListView)findViewById(R.id.list);
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Date date = new Date();
        String strTime = "日時: " + dateFormat.format(date);
        tv[3].setText(strTime);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        insbt = (Button)findViewById(R.id.ins_bt);
        insbt.setText("insert");
        delbt = (Button)findViewById(R.id.del_bt);
        delbt.setText("delete");
        delAllbt = (Button)findViewById(R.id.delall_bt);
        delbt.setText("delete ALL");
        sendbt = (Button)findViewById(R.id.send_bt);
        sendbt.setText("send");
        itmes = new ArrayList<>();
        dbHelper = new MyDbHelper(this);
        db = dbHelper.getWritableDatabase();

        Log.d(TAG, "Activity: onCreate");

        sendbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upload(db);

            }
        });
        insbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insert();
                simpleAdapter = (SimpleAdapter)list.getAdapter();
                simpleAdapter.notifyDataSetChanged();
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long l) {
                Map map = itmes.get(index);
                String id = map.get("id").toString();
                Toast.makeText(getApplicationContext(), id, Toast.LENGTH_SHORT).show();
                rowId = Integer.valueOf(id);
            }
        });

        delbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DelDialogFragment delFragment = new DelDialogFragment(delflg);
                delFragment.setMessage(String.valueOf(rowId));
                delFragment.setObject(db, simpleAdapter, list, itmes);

                delFragment.show(
                        getSupportFragmentManager(), "deletedialog");

            }
        });
        //データの全削除
        delAllbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.delete("sensor", null, null);
            }
        });
    }

    public static class DelDialogFragment extends DialogFragment {
        SQLiteDatabase db;
        ListView list;
        List items;
        SimpleAdapter simpleAdapter;
        String dataId = "";
        boolean delflg;

        public DelDialogFragment(boolean delflg){
            this.delflg = delflg;
        }

        @Override
        public Dialog onCreateDialog(Bundle saveInstanceState){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Delete_ID:" + dataId)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            delflg = true;
                            int i = MainActivity.delete(dataId, db);
                            Log.d(TAG, "delete:" + i);
                            querry(db,items);
                            simpleAdapter = (SimpleAdapter)list.getAdapter();
                            simpleAdapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    delflg = false;
                                }
                            });
            return builder.create();
        }

        void setMessage(String str){
            this.dataId = str;
        }

        void setObject(
                SQLiteDatabase db,
                SimpleAdapter simpleAdapter,
                ListView list, List items){
            this.db = db;
            this.simpleAdapter = simpleAdapter;
            this.items = items;
            this.list  = list;
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        querry(db,itmes);
        Log.d(TAG, "size:"+itmes.size());

        simpleAdapter = new SimpleAdapter(
                getApplicationContext(),
                (List<? extends Map<String, ?>>)itmes,
                R.layout.rowdata,
                new String[] { "date", "datax", "datay", "dataz"},
                new int[] {R.id.date,R.id.data_x, R.id.data_y, R.id.data_z});
        list.setAdapter(simpleAdapter);
        Log.d(TAG, String.valueOf(simpleAdapter));
    }

    public static void querry(SQLiteDatabase db, List items) {
        Cursor cursor = db.query(DataEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );


        items.clear();
        while(cursor.moveToNext()){
            Map<String, String> dmap = new HashMap<>();
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DataEntry._ID));
            dmap.put("id", String.valueOf(id));
            String date = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataEntry.DATA_TIME));
            dmap.put("date", date);
            double datax = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAX));
            dmap.put("datx", String.valueOf(datax));

            double datay = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAY));
            dmap.put("datay", String.valueOf(datay));

            double dataz = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAZ));
            dmap.put("dataz", String.valueOf(dataz));
            items.add(dmap);
            Log.d(TAG, dmap.get("id")+","+dmap.get("date")+","+dmap.get("datax")+","+dmap.get("datay")+","+dmap.get("dataz"));
        }
        cursor.close();
    }

    private  void upload(SQLiteDatabase db){
        Cursor cursor = db.query(DataEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        List<String> ullist = new ArrayList<>();
        while(cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DataEntry._ID));

            String date = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataEntry.DATA_TIME));
            double datax = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAX));

            double datay = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAY));
            double dataz = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAZ));

            String urlStr = "http://ipaddress/insert_test.php?date=" + date + "&datax=" + datax + "&datay=" + datay + "&dataz=" + dataz;
            Log.d("url:", urlStr);
            ullist.add(urlStr);
        }
        dt = new DownloadTask();
        String[] urls = ullist.toArray(new String[ullist.size()]);
        dt.execute(urls);
    }

    private void insert(){
        ContentValues values = new ContentValues();
        values.put(DataEntry.DATA_TIME, getNow());
        heikin = heikinSensor(sensorX, (float) 5.0);
        Log.d("heikin", String.valueOf(heikin));
        values.put(DataEntry.ACCELE_DATAX, heikin);
        values.put(DataEntry.ACCELE_DATAY, sensorY);
        values.put(DataEntry.ACCELE_DATAZ, sensorZ);

        long newRowId = db.insert(DataEntry.TABLE_NAME, null, values);
        Log.d(TAG, "newRowId:" + newRowId);

        querry(db, itmes);

    }

    public static int delete(String id, SQLiteDatabase db){
        String selection = DataEntry._ID + " LIKE ?";
        String[] selectionArgs = { id };
        int deletedRows = db.delete(
                DataEntry.TABLE_NAME,
                selection,
                selectionArgs);
        return deletedRows;
    }

  /*  @Override
    protected void onDestroy(){
        dbHelper.close();
        super.onDestroy();
    }*/
/*  private void upload(){

  }*/


    @Override
    protected void onResume() {
        super.onResume();
        // Listenerの登録
        Sensor accel = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);

    }

    // 解除するコードも入れる!
    @Override
    protected void onPause() {
        super.onPause();
        // Listenerを解除
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //float[] sensorX = new float[5];
        //float[] sensorY = new float[5];
        //float[] sensorZ = new float[5];

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            for(int i = 0; i < 3; i++) {
                switch (i){
                    case 0:
                        sensorX = event.values[0];
                        senX++;
                        cnt++;
                        String sx = String.valueOf(sensorX);
                        tv[0].setText(sx);
                        break;
                    case 1:
                        sensorY = event.values[1];
                        senY++;
                        cnt++;
                        String sy = String.valueOf(sensorY);
                        tv[1].setText(sy);
                        break;
                    case 2:
                        sensorZ = event.values[2];
                        senZ++;
                        cnt++;
                        String sz = String.valueOf(sensorZ);
                        tv[2].setText(sz);
                        break;

                }
            }
        }
    }

    public float heikinSensor(float sen, float cnt){
        heikin = sen / cnt;
        Log.d("heikinSensor["+ cnt+ "]", String.valueOf(heikin));
        return heikin;
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

    private String getNow(){
        // set the format to sql date time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
