package ictproject.vivi.isecure;

        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.SharedPreferences;
        import android.os.Bundle;
        import android.app.Activity;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.TextView;

        import org.json.JSONException;
        import org.json.JSONObject;

public class MainActivity extends Activity {
    private MQTTMessageReceiver messageIntentReceiver;

    TextView response;
    EditText editTextAddress, editTextPort;
    Button buttonConnect, buttonClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        response = (TextView) findViewById(R.id.receivedText);

        SharedPreferences settings = getSharedPreferences(MQTTService.APP_ID, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("broker", "192.168.0.69");
        editor.putString("topic", "contoh");
        editor.commit();

        messageIntentReceiver = new MQTTMessageReceiver();
        IntentFilter intentCFilter = new IntentFilter(MQTTService.MQTT_MSG_RECEIVED_INTENT);
        registerReceiver(messageIntentReceiver, intentCFilter);

        Intent svc = new Intent(this, MQTTService.class);
        startService(svc);
        System.out.println("Service started");

    }

    public class MQTTMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            System.out.println("Data received");
            Bundle notificationData = intent.getExtras();
            /* The topic of this message. */
            String newTopic = notificationData.getString(MQTTService.MQTT_MSG_RECEIVED_TOPIC);
            /* The message payload. */
            String newData = notificationData.getString(MQTTService.MQTT_MSG_RECEIVED_MSG);
            response.setText(newData);
        }
    }

    protected void onDestroy() {
        System.out.println("Service stopped");
        super.onDestroy();
        Intent svc = new Intent(this, MQTTService.class);
        stopService(svc);
        unregisterReceiver(messageIntentReceiver);
    }


}
