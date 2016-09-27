package ictproject.vivi.isecure;

/**
 * Created by ajou on 9/27/2016.
 */

        import java.lang.ref.WeakReference;
        import java.util.Calendar;
        import java.util.Date;
        import java.util.Enumeration;
        import java.util.Hashtable;

        import android.app.AlarmManager;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.Service;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.SharedPreferences;
        import android.net.ConnectivityManager;
        import android.os.Binder;
        import android.os.IBinder;
        import android.os.PowerManager;
        import android.os.PowerManager.WakeLock;
        import android.provider.Settings;
        import android.provider.Settings.Secure;
        import android.util.Log;

        import com.ibm.mqtt.IMqttClient;
        import com.ibm.mqtt.MqttClient;
        import com.ibm.mqtt.MqttException;
        import com.ibm.mqtt.MqttNotConnectedException;
        import com.ibm.mqtt.MqttPersistence;
        import com.ibm.mqtt.MqttPersistenceException;
        import com.ibm.mqtt.MqttSimpleCallback;

/**
 * Created by VISCAR team on 3/26/2016.
 */

public class MQTTService extends Service implements MqttSimpleCallback {
    /************************************************************************/
    /*	CONSTANTS														 */
    /************************************************************************/
    //   application preferences
    public static final String APP_ID = "com.vivi.viscar";

    // constants used to notify the Activity UI of received messages
    public static final String MQTT_MSG_RECEIVED_INTENT = "org.mosquitto.android.mqtt.MSGRECVD";
    public static final String MQTT_MSG_RECEIVED_TOPIC = "org.mosquitto.android.mqtt.MSGRECVD_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG = "org.mosquitto.android.mqtt.MSGRECVD_MSGBODY";

    // constants used to tell the Activity UI the connection status
    public static final String MQTT_STATUS_INTENT = "org.mosquitto.android.mqtt.STATUS";
    public static final String MQTT_STATUS_MSG = "org.mosquitto.android.mqtt.STATUS_MSG";

    // constant used internally to schedule the next ping event
    public static final String MQTT_PING_ACTION = "org.mosquitto.android.mqtt.PING";

    // constants used by status bar notifications
    public static final int MQTT_NOTIFICATION_ONGOING = 1;
    public static final int MQTT_NOTIFICATION_UPDATE = 2;

    // constants used to define MQTT connection status
    public enum MQTTConnectionStatus {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user has disabled data access
        NOTCONNECTED_UNKNOWNREASON          // failed to connect for some reason
    }

    // MQTT constants
    public static final int MAX_MQTT_CLIENTID_LENGTH = 22;

    /************************************************************************/
    /*	VARIABLES used to maintain state								  */
    /************************************************************************/

    // status of MQTT client connection
    private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;

    /************************************************************************/
	/*	VARIABLES used to configure MQTT connection					   */
    /************************************************************************/
    private String brokerHostName = "";
    private String topicName = "";

    // defaults - this sample uses very basic defaults for it's interactions with message brokers
    private int brokerPortNumber = 1883;
    private MqttPersistence usePersistence = null;
    private boolean cleanStart = false;
    private int[] qualitiesOfService = {0};
    private short keepAliveSeconds = 20 * 60;
    private String mqttClientId = null;

    /************************************************************************/
	/*	VARIABLES  - other local variables								*/
    /************************************************************************/
    // connection to the message broker
    private IMqttClient mqttClient = null;

    // receiver that notifies the Service when the phone gets data connection
    private NetworkConnectionIntentReceiver netConnReceiver;

    // receiver that notifies the Service when the user changes data use preferences
    private BackgroundDataChangeIntentReceiver dataEnabledReceiver;

    // receiver that wakes the Service up when it's time to ping the server
    private PingSender pingSender;

    /************************************************************************/
	/*	METHODS - core Service lifecycle methods						  */

    /************************************************************************/
    @Override
    public void onCreate() {
        super.onCreate();

        // reset status variable to initial state
        connectionStatus = MQTTConnectionStatus.INITIAL;

        // create a binder that will let the Activity UI send commands to the Service
        mBinder = new LocalBinder<MQTTService>(this);
        SharedPreferences settings = getSharedPreferences(APP_ID, MODE_PRIVATE);
        brokerHostName = settings.getString("broker", "");
        topicName = settings.getString("topic", "");
        dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
        registerReceiver(dataEnabledReceiver,
                new IntentFilter(ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
        defineConnectionToBroker(brokerHostName);
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        }, "MQTTservice").start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        }, "MQTTservice").start();
        return START_STICKY;
    }

    synchronized void handleStart(Intent intent, int startId) {
        if (mqttClient == null) {
            // unable to define the MQTT client connection, so stop immediately
            stopSelf();
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getBackgroundDataSetting() == false) // respect the user's request not to use data!
        {
            // user has disabled background data
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;

            // update the app to show that the connection has been disabled
            broadcastServiceStatus("Not connected - background data disabled");
            return;
        }
        rebroadcastStatus();
        rebroadcastReceivedMessages();

        // if the Service was already running and already connected - no need to do anything
        if (isAlreadyConnected() == false) {
            // set the status to show we're trying to connect
            connectionStatus = MQTTConnectionStatus.CONNECTING;
            Intent notificationIntent = new Intent(this, MQTTService.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //check if the phone has a working data connection
            if (isOnline()) {
                //  try to connect to the message broker
                if (connectToBroker()) {
                    // subscribe to a topic
                    subscribeToTopic(topicName);
                }
            } else {
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
                // inform the app that we are not connected
                broadcastServiceStatus("Waiting for network connection");
            }
        }

        if (netConnReceiver == null) {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        }

        // creates the intents that are used to wake up the phone when it is time to ping the server
        if (pingSender == null) {
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // disconnect immediately
        disconnectFromBroker();
        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
        // try not to leak the listener
        if (dataEnabledReceiver != null) {
            unregisterReceiver(dataEnabledReceiver);
            dataEnabledReceiver = null;
        }
        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }
    }

    /************************************************************************/
	/*	METHODS - broadcasts and notifications							*/

    /************************************************************************/
    private void broadcastServiceStatus(String statusDescription) {
        // inform the app (for times when the Activity UI is running /active) of the current MQTT connection status so that it can update the UI accordingly
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, String message) {
        // pass a message received from the MQTT server on to the Activity UI (for times when it is running / active) so that it can be displayed in the app GUI
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
        sendBroadcast(broadcastIntent);
    }

    private void notifyUser(String alert, String title, String body) {
        Intent notificationIntent = new Intent(this, MQTTService.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /************************************************************************/
	/*	METHODS - binding that allows access from the Actitivy			*/
    /************************************************************************/
    // trying to do local binding while minimizing leaks
    private LocalBinder<MQTTService> mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder<S> extends Binder {
        private WeakReference<S> mService;

        public LocalBinder(S service) {
            mService = new WeakReference<S>(service);
        }

        public S getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }

    public MQTTConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void rebroadcastStatus() {
        String status = "";

        switch (connectionStatus) {
            case INITIAL:
                status = "Please wait";
                break;
            case CONNECTING:
                status = "Connecting...";
                break;
            case CONNECTED:
                status = "Connected";
                break;
            case NOTCONNECTED_UNKNOWNREASON:
                status = "Not connected - waiting for network connection";
                break;
            case NOTCONNECTED_USERDISCONNECT:
                status = "Disconnected";
                break;
            case NOTCONNECTED_DATADISABLED:
                status = "Not connected - background data disabled";
                break;
            case NOTCONNECTED_WAITINGFORINTERNET:
                status = "Unable to connect";
                break;
        }
        // inform the app that the Service has successfully connected
        broadcastServiceStatus(status);
    }

    public void disconnect() {
        disconnectFromBroker();
        // set status
        connectionStatus = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;
        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
    }

    /************************************************************************/
	/*	METHODS - MQTT methods inherited from MQTT classes				*/

    /************************************************************************/
    public void connectionLost() throws Exception {
        // protect against the phone switching off while doing this by requesting a wake lock - request the minimum possible wake lock - just enough to keep the CPU running until finish
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (isOnline() == false) {
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - no network connection");
            // inform the user (for times when the Activity UI isn't running) that we are no longer able to receive messages
            notifyUser("Connection lost - no network connection",
                    "MQTT", "Connection lost - no network connection");
        } else {
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - reconnecting...");
            // try to reconnect
            if (connectToBroker()) {
                subscribeToTopic(topicName);
            }
        }
        // if the phone is switched off, it's okay for the CPU to sleep now
        wl.release();
    }

    //callback - called when we receive a message from the server
    public void publishArrived(String topic, byte[] payloadbytes, int qos, boolean retained) {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();
        String messageBody = new String(payloadbytes);
        if (addReceivedMessageToStore(topic, messageBody)) {
            broadcastReceivedMessage(topic, messageBody);
            notifyUser("New data received", topic, messageBody);
        } else {
            broadcastReceivedMessage(topic, messageBody);
            notifyUser("New data received", topic, messageBody);
        }
        scheduleNextPing();
        wl.release();
    }

    /************************************************************************/
	/*	METHODS - wrappers for some of the MQTT methods that we use	   */

    /************************************************************************/
    //Create a client connection object that defines our connection to a message broker server
    private void defineConnectionToBroker(String brokerHostName) {
        String mqttConnSpec = "tcp://" + brokerHostName + "@" + brokerPortNumber;

        try {
            // define the connection to the broker
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, usePersistence);
            // register this client app has being able to receive messages
            mqttClient.registerSimpleHandler(this);
        } catch (MqttException e) {
            mqttClient = null;
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
            broadcastServiceStatus("Invalid connection parameters");
            notifyUser("Unable to connect", "MQTT", "Unable to connect");
        }
    }

    //(Re-)connect to the message broker
    private boolean connectToBroker() {
        try {
            // try to connect
            mqttClient.connect(generateClientId(), cleanStart, keepAliveSeconds);
            // inform the app that the app has successfully connected
            broadcastServiceStatus("Connected");
            // we are connected
            connectionStatus = MQTTConnectionStatus.CONNECTED;
            scheduleNextPing();

            return true;
        } catch (MqttException e) {
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;
            broadcastServiceStatus("Unable to connect");
            notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");
            scheduleNextPing();
            return false;
        }
    }

    // Send a request to the message broker to be sent messages published with the specified topic name. Wildcards are allowed.
    private void subscribeToTopic(String topicName) {
        boolean subscribed = false;

        if (isAlreadyConnected() == false) {
            Log.e("mqtt", "Unable to subscribe as we are not connected");
        } else {
            try {
                String[] topics = {topicName};
                mqttClient.subscribe(topics, qualitiesOfService);
                subscribed = true;
            } catch (MqttNotConnectedException e) {
                Log.e("mqtt", "subscribe failed - MQTT not connected", e);
            } catch (IllegalArgumentException e) {
                Log.e("mqtt", "subscribe failed - illegal argument", e);
            } catch (MqttException e) {
                Log.e("mqtt", "subscribe failed - MQTT exception", e);
            }
        }

        if (subscribed == false) {
            broadcastServiceStatus("Unable to subscribe");
            notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
        }
    }


    // Terminates a connection to the message broker.
    private void disconnectFromBroker() {
        try {
            if (netConnReceiver != null) {
                unregisterReceiver(netConnReceiver);
                netConnReceiver = null;
            }
            if (pingSender != null) {
                unregisterReceiver(pingSender);
                pingSender = null;
            }
        } catch (Exception eee) {
            Log.e("mqtt", "unregister failed", eee);
        }
        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
        } catch (MqttPersistenceException e) {
            Log.e("mqtt", "disconnect failed - persistence exception", e);
        } finally {
            mqttClient = null;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
    }


    // Checks if the MQTT client thinks it has an active connection
    private boolean isAlreadyConnected() {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }

    private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm.getBackgroundDataSetting()) {
                defineConnectionToBroker(brokerHostName);
                handleStart(intent, 0);
            } else {
                // user has disabled background data
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;
                // update the app to show that the connection has been disabled
                broadcastServiceStatus("Not connected - background data disabled");
                // disconnect from the broker
                disconnectFromBroker();
            }
            wl.release();
        }
    }


    // Called in response to a change in network connection - after losing a
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();
            if (isOnline()) {
                if (connectToBroker()) {
                    subscribeToTopic(topicName);
                }
            }
            wl.release();
        }
    }

    // Schedule the next time the phone to wake up and ping the message broker server

    private void scheduleNextPing() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }

    // Used to implement a keep-alive protocol at this Service level - it sends a PING message to the server, then schedules another ping after an interval defined by keepAliveSeconds
    public class PingSender extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                mqttClient.ping();
            } catch (MqttException e) {
                Log.e("mqtt", "ping failed - MQTT exception", e);
                try {
                    mqttClient.disconnect();
                } catch (MqttPersistenceException e1) {
                    Log.e("mqtt", "disconnect failed - persistence exception", e1);
                }
                if (connectToBroker()) {
                    subscribeToTopic(topicName);
                }
            }
            scheduleNextPing();
        }
    }

    /************************************************************************/
	/*   APP SPECIFIC - stuff that would vary for different uses of MQTT	*/
    /************************************************************************/
    private Hashtable<String, String> dataCache = new Hashtable<String, String>();

    private boolean addReceivedMessageToStore(String key, String value) {
        String previousValue = null;

        if (value.length() == 0) {
            previousValue = dataCache.remove(key);
        } else {
            previousValue = dataCache.put(key, value);
        }
        return ((previousValue == null) ||
                (previousValue.equals(value) == false));
    }

    // provide a public interface, so Activities that bind to the Service can request access to previously received messages
    public void rebroadcastReceivedMessages() {
        Enumeration<String> e = dataCache.keys();
        while (e.hasMoreElements()) {
            String nextKey = e.nextElement();
            String nextValue = dataCache.get(nextKey);
            broadcastReceivedMessage(nextKey, nextValue);
        }
    }

    /************************************************************************/
	/*	METHODS - internal utility methods								*/

    /************************************************************************/

    private String generateClientId() {
        if (mqttClientId == null) {
            String timestamp = "" + (new Date()).getTime();
            String android_id = Settings.System.getString(getContentResolver(),
                    Secure.ANDROID_ID);
            mqttClientId = timestamp + android_id;
            // truncate - MQTT spec doesn't allow client ids longer than 23 chars
            if (mqttClientId.length() > MAX_MQTT_CLIENTID_LENGTH) {
                mqttClientId = mqttClientId.substring(0, MAX_MQTT_CLIENTID_LENGTH);
            }
        }

        return mqttClientId;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected()) {
            return true;
        }
        return false;
    }

}


