package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.groupmessenger1.provider";
    static final String URL = "content://"+PROVIDER_NAME;
    private static final Uri CONTENT_URI = Uri.parse(URL);
    private static final String TABLE_COL_KEY = "key";
    private static final String TABLE_COL_VAL = "value";
    static Integer messageCounter = -1;
    static Map<String, String> avdMap = new HashMap<String, String>();
    static String myPort = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.local_text_display);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * Initialize avd map with client ports
         */
        avdMap.put(REMOTE_PORT0,"avd0");
        avdMap.put(REMOTE_PORT1, "avd1");
        avdMap.put(REMOTE_PORT2, "avd2");
        avdMap.put(REMOTE_PORT3, "avd3");
        avdMap.put(REMOTE_PORT4, "avd4");
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


        try {
            Button sendButton = (Button) findViewById(R.id.button4);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText editText = (EditText) findViewById(R.id.editText1);
                    String message = editText.getText().toString();
                    editText.setText("");
                    if (message != null && message.length()>0) {
                        TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                        localTextView.append("\t"+avdMap.get(myPort) + ": " + "\n");
                        localTextView.append(message + "\n");
                        char[] spaces = new char[localTextView.getText().length()];
                        Arrays.fill(spaces, ' ');
                        TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                        remoteTextView.append(new String(spaces)+"\n");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message);
                    }
                }

                ;
            });
        } catch (Exception ex) {
            Log.e(TAG, "Exception in Send message");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            ContentValues values = new ContentValues();
            messageCounter = messageCounter+1;
            values.put(TABLE_COL_KEY, messageCounter.toString());
            values.put(TABLE_COL_VAL, msgs[0]);
            getContentResolver().insert(CONTENT_URI, values);
            for (String clientPort : avdMap.keySet()) {
                Socket socket = null;
                try {
                    if(!clientPort.equals(myPort)) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(clientPort));
                    }

                } catch (UnknownHostException ex) {
                    Log.e(TAG, "Client: " + clientPort + " is not up yet");
                } catch (IOException ex) {
                    Log.e(TAG, "Exception in connecting to client: " + clientPort);
                }

                if (socket != null && socket.isConnected()) {
                    Log.d(TAG, "Connected to client : " + clientPort);
                    String msgToSend = msgs[0];
                    Message multicastMessage = new Message(msgToSend);
                    multicastMessage.setAvd(avdMap.get(myPort));
                    multicastMessage.setFromPort(myPort);
                    new ClientThread(socket, multicastMessage).start();
                }

            }
            return null;
        }
    }


    private class ClientThread extends Thread {
        public Socket remoteSocket;
        public Message messageToSend;

        public ClientThread(Socket remoteSocket, Message messageToSend) {
            this.remoteSocket = remoteSocket;
            this.messageToSend = messageToSend;
        }

        @Override
        public void run() {
            try {
                OutputStream os = remoteSocket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(messageToSend);
                oos.flush();
                oos.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception in Sending Message to Client: " + remoteSocket.getPort());
            } finally {
                try {
                    remoteSocket.close();
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in closing socket");
                }


            }
        }
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while (true) {
                    socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(in);
                    Message multiCastMessage = (Message) ois.readObject();
                    if (multiCastMessage != null) {
                        messageCounter = messageCounter + 1;
                        String fromAvd = multiCastMessage.getAvd();
                        String message = multiCastMessage.getMessage();
                        ContentValues values = new ContentValues();
                        values.put(TABLE_COL_KEY, messageCounter);
                        values.put(TABLE_COL_VAL, message);
                        getContentResolver().insert(CONTENT_URI, values);
                        publishProgress(new String[]{fromAvd, message});
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception in Receiving messages " + ex);

            }
            return null;
        }


    protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
        String strReceived = strings[1];
        String fromAvd = strings[0];
        TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
        remoteTextView.append(fromAvd+": \t\n");
        remoteTextView.append(strReceived+"\t\n");
        char[] spaces = new char[remoteTextView.getText().length()];
        Arrays.fill(spaces, ' ');
        TextView localTextView = (TextView) findViewById(R.id.local_text_display);
        localTextView.append(new String(spaces)+"\n");


            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

        String filename = "SimpleMessengerOutput";
        String string = strReceived + "\n";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed");
        }

        return;
    }
}
}

