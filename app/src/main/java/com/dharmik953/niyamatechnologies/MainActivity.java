package com.dharmik953.niyamatechnologies;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 20 ;
    private static final UUID MY_UUID = UUID.randomUUID() ;
    private static final String STATE_CONNECTED = "device conneted";
    int SELECT_PICTURE = 200;

    BluetoothAdapter bluetoothAdapter;

    ImageView imageView;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button open = findViewById(R.id.open);
        Button print = findViewById(R.id.print);
        Button bluetooth = findViewById(R.id.bluetooth);
        EditText et = findViewById(R.id.et_note);
        tv = findViewById(R.id.tv);

        imageView = findViewById(R.id.imageView);

        open.setOnClickListener(v -> imageChooser());


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);


        bluetooth.setOnClickListener(v -> {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // Device doesn't support Bluetooth
                Toast.makeText(getApplicationContext(), "Your devise has no bluetooth", Toast.LENGTH_SHORT).show();
            }


            int requestCode = 1;
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent, requestCode);

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                }
            }

        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("my notification","my notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        print.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "     Printing Document     ", Toast.LENGTH_SHORT).show();
            tv.setText(et.getText().toString());
            et.setText("");
//*************************************************************************         Notification       *******************************************************************************************
            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "my notification");
            builder.setContentTitle("Printing");
            builder.setContentText("Your document is printing");
            builder.setSmallIcon(R.drawable.ic_baseline_print_24);
            builder.setAutoCancel(true);

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(MainActivity.this);
            managerCompat.notify(1,builder.build());

        });

    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
        ab.setTitle("Alert!");
        ab.setMessage("are you sure to exit?");
        ab.setPositiveButton("yes", (dialog, which) -> {
            dialog.dismiss();
            //if you want to kill app . from other then your main avtivity.(Launcher)
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

            //if you want to finish just current activity

            MainActivity.this.finish();
        });
        ab.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        ab.show();
    }

    void imageChooser() {

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_PICTURE) {

                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {

                    imageView.setImageURI(selectedImageUri);
                }
            }
        }
    }

    private class AcceptThread extends Thread {
        private static final String NAME = "Niyama_Technologies";
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e("TAG", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {

                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("TAG", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
//                    manageMyConnectedSocket(socket);

                    // server code


                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the connect socket", e);
            }
        }
    }

}
