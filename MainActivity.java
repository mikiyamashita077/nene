package app.lightbox.winofsql.jp.sensorkadai10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ListView list;
    private SensorManager mSensorManager;
    private TextView[] tv = new TextView[3];
    SensorData sd;
    private DownloadTask dt;
    int count;
    int savex = 0;
    float mean_x;
    private static final String TAG = "DbSample";
    SQLiteDatabase db;
    MyDbHelper dbHelper;
    private Button insbt;
    private Button delbt;
    private Button sendbt;
    SimpleAdapter simpleAdapter;
    private List<Map<String, String>> itmes;
    private int rowId = 0;
    private boolean delflg=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv[0] = (TextView)findViewById(R.id.data_x);
        tv[1] = (TextView)findViewById(R.id.data_y);
        tv[2] = (TextView)findViewById(R.id.data_z);
        list = (ListView)findViewById(R.id.list);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sd = new SensorData(mSensorManager, tv);
        insbt = (Button)findViewById(R.id.ins_bt);
        insbt.setText("insert");
        delbt = (Button)findViewById(R.id.del_bt);
        delbt.setText("delete");
        sendbt = (Button)findViewById(R.id.send_bt);
        sendbt.setText("send");
        itmes = new ArrayList<>();
        dbHelper = new MyDbHelper(this);
        db = dbHelper.getWritableDatabase();

        Log.d(TAG, "Activity: onCreate");

        sendbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = db.query(DataEntry.TABLE_NAME,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
                List<String> ulli = new ArrayList<>();
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
                    dt = new DownloadTask();
                    dt.execute(urlStr);
                }
                cursor.close();
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
    }

    public static class DelDialogFragment extends DialogFragment {
        SQLiteDatabase db;
        ListView lv;
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
                            simpleAdapter = (SimpleAdapter)lv.getAdapter();
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
                ListView lv, List items){
            this.db = db;
            this.simpleAdapter = simpleAdapter;
            this.items = items;
            this.lv = lv;
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
                new int[] {R.id.data_x, R.id.data_y, R.id.data_z});
        list.setAdapter(simpleAdapter);
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

//            String[] projection = {
//                    BaseColumns._ID,
//                    DataEntry.COLUMN_NAME,
//                    DataEntry.COLUMN_LAT,
//                    DataEntry.COLUMN_LON
//            };
//            String selection = DataEntry.COLUMN_NAME + " = ?";
//            String[] selectionArgs = { "魚津駅前" };
//            String sortOrder =
//                    DataEntry.COLUMN_NAME + "DESC";
//
//            Cursor cursor = db.query(
//                    DataEntry.TABLE_NAME,
//                    projection,
//                    selection,
//                    selectionArgs,
//                    null,
//                    null,
//                    sortOrder
//            );
        items.clear();
        while(cursor.moveToNext()){
            Map<String, String> dmap = new HashMap<>();
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DataEntry._ID));
            dmap.put("id", String.valueOf(id));
            String date = cursor.getString(
                    cursor.getColumnIndexOrThrow(DataEntry.DATA_TIME));
            dmap.put("name", date);
            double datax = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAX));
            dmap.put("lat", String.valueOf(datax));

            double datay = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAY));
            dmap.put("lon", String.valueOf(datay));

            double dataz = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(DataEntry.ACCELE_DATAZ));
            dmap.put("lon", String.valueOf(dataz));
            items.add(dmap);
            Log.d(TAG, dmap.get("id")+","+dmap.get("date")+","+dmap.get("datax")+","+dmap.get("datay")+","+dmap.get("dataz"));
        }
        cursor.close();
    }

    private void insert(){
        ContentValues values = new ContentValues();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        values.put(DataEntry.DATA_TIME, dateFormat.format(date));
        values.put(DataEntry.ACCELE_DATAX,36.82735459);
        values.put(DataEntry.ACCELE_DATAY, 137.408397);
        values.put(DataEntry.ACCELE_DATAZ, 137.408397);

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



    protected void onResume(){
        super.onResume();
        sd.startSensor();
    }

    @Override
    protected void onPause(){
        super.onPause();
        sd.stopSensor();
    }
}
