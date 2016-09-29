package ictproject.vivi.isecure;

        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.ProgressDialog;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.SharedPreferences;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.media.AudioManager;
        import android.media.MediaPlayer;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.app.Activity;
        import android.support.v7.app.NotificationCompat;
        import android.text.method.CharacterPickerDialog;
        import android.util.Base64;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.widget.Button;
        import android.widget.CompoundButton;
        import android.widget.EditText;
        import android.widget.ImageView;
        import android.widget.Switch;
        import android.widget.TextView;

        import org.eclipse.paho.client.mqttv3.MqttClient;
        import org.eclipse.paho.client.mqttv3.MqttMessage;
        import org.eclipse.paho.client.mqttv3.MqttTopic;
        import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.BufferedReader;
        import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private MQTTMessageReceiver messageIntentReceiver;
    String imageString="";
    TextView response;
    Button btnStartCCTV, btnStopCCTV, btnReset;
    Switch awayCondition;
    sendToRPI sendToRPI;
    ImageView imageContent;
    Boolean isPlaying = false;
    Boolean stopCCTV = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        response = (TextView) findViewById(R.id.receivedText);
        isPlaying = false;
        btnStartCCTV = (Button)findViewById(R.id.buttonStartCCTV);
        btnStopCCTV = (Button)findViewById(R.id.buttonStopCCTV);
        awayCondition = (Switch)findViewById(R.id.awayCondition);
        imageContent = (ImageView)findViewById(R.id.imageContent);
//        btnReset = (Button)findViewById(R.id.btnReset);


        SharedPreferences settings = getSharedPreferences(MQTTService.APP_ID, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("broker","192.168.0.77");
        //editor.putString("broker", "172.20.10.2"); //MODIFY
        editor.putString("topic", "toAndroid"); //MODIFY
        editor.commit();

        messageIntentReceiver = new MQTTMessageReceiver();
        IntentFilter intentCFilter = new IntentFilter(MQTTService.MQTT_MSG_RECEIVED_INTENT);
        registerReceiver(messageIntentReceiver, intentCFilter);

        Intent svc = new Intent(this, MQTTService.class);
        startService(svc);
        System.out.println("Service started");

        awayCondition.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (awayCondition.isChecked()) {
                    System.out.println("AWAY!");
                    btnStartCCTV.setEnabled(true);
                    sendToRPI = new sendToRPI(MainActivity.this, "away");
                    sendToRPI.execute();
                } else {
                    btnStopCCTV.setEnabled(false);
                    btnStartCCTV.setEnabled(false);
                    imageContent.setImageResource(R.drawable.logo);
                    sendToRPI = new sendToRPI(MainActivity.this, "home");
                    sendToRPI.execute();
                }
            }
        });

        btnStartCCTV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopCCTV.setEnabled(true);
                btnStartCCTV.setEnabled(false);
                stopCCTV = false;
                sendToRPI = new sendToRPI(MainActivity.this, "cctvON");
                sendToRPI.execute();
            }
        });

        btnStopCCTV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopCCTV.setEnabled(false);
                btnStartCCTV.setEnabled(true);
                isPlaying = false;
                stopCCTV = true;
                imageContent.setImageResource(R.drawable.logo);
                sendToRPI = new sendToRPI(MainActivity.this, "cctvOFF");
                sendToRPI.execute();
            }
        });

//        btnReset.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent mIntent = new Intent(MainActivity.this, MainActivity.class);
//                finish();
//                startActivity(mIntent);
//            }
//        });


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
            System.out.println(newData);
            System.out.println("isPlaying"+isPlaying);
//            response.setText(newData);
            if(newData.equals("intruder")){
                //if(!isPlaying){
                    showNotification("INTRUDER","INTRUDER detected. Watch out!");
                    imageContent.setImageResource(R.drawable.intruder);
                //}

            }else if(newData.equals("lamp")){
                //if(!isPlaying){
                    showNotification("LAMP","LAMP is ON!\nTurn off the lamp while you are away");
                    imageContent.setImageResource(R.drawable.lamp);
                //}



            }else{
                if(!stopCCTV){
                    try {
                        JSONObject objNew = new JSONObject(newData);
                        if(objNew.getString("end").equals("0")){
                            if(objNew.getString("pos").equals("0")){
                                System.out.println("data pertama");
                                imageString="";
                                imageString = objNew.getString("data");
                            }else{
                                System.out.println("bukan data pertama");
                                imageString = imageString.concat(objNew.getString("data"));
                            }
                        }else{
                            System.out.println(imageString);
//                    imageString="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";
                            byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imageContent.setImageBitmap(decodedByte);
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }


        }
    }

    private void showNotification(String title, String content){
        MediaPlayer mPlayer = MediaPlayer.create(this, R.raw.ringnotif);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.start();
        isPlaying = true;

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                isPlaying = false;
            }
        });

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        // Notification Icon
        builder.setSmallIcon(R.drawable.warning);
        // Notification Title
        builder.setContentTitle(title);
        //Notification Text
        builder.setContentText(content);
        builder.setAutoCancel(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent); // start activity when the user clicks the notification text
        NotificationManager NM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NM.notify(0, builder.build());
    }
    private class sendToRPI extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progDialog;
        private String message;

        public sendToRPI(MainActivity activity, String msg) {
            progDialog = new ProgressDialog(activity);
            this.message = msg;
        }

        @Override
        protected void onPreExecute() {
            progDialog.setMessage("Syncing...");
            progDialog.setIndeterminate(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDialog.setCancelable(true);
            progDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (progDialog.isShowing()) {
                progDialog.dismiss();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                System.out.println("kirim ke RPI");
                String cacheDir = getCacheDir().getAbsolutePath();
                MqttClient client = new MqttClient("tcp://192.168.0.77:1883", "toRPI", new MqttDefaultFilePersistence(cacheDir));

//                MqttClient client = new MqttClient("tcp://172.20.10.2:1883", "toRPI", new MqttDefaultFilePersistence(cacheDir));
                MqttTopic mqttTopic = client.getTopic("toRPI");
                client.connect();
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1);
                mqttTopic.publish(mqttMessage);
                client.disconnect();
                client.close();



            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
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
