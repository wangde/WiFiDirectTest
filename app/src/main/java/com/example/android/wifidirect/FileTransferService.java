package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_CLIENT_ADDRESS = "cl_host";
    public static final String EXTRAS_GROUP_PORT = "port";
    public static final String EXTRAS_GROUP_OWNER = "flag";
    public static final String EXTRAS_HOST_NAME = "my_name";


    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            Socket socket = new Socket();
            String host;
            String host_name,peer_name;
            host_name = intent.getExtras().getString(EXTRAS_HOST_NAME);
            Boolean flag = intent.getExtras().getBoolean(EXTRAS_GROUP_OWNER);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_PORT);

            if (flag) {
                host = intent.getExtras().getString(EXTRAS_GROUP_CLIENT_ADDRESS);



            } else {
                host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
                host_name = intent.getExtras().getString(EXTRAS_HOST_NAME);

            }

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening  socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Toast.makeText(this, "connect", Toast.LENGTH_LONG).show();

                Log.d(WiFiDirectActivity.TAG, " socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                FileInputStream fs;
                ByteArrayInputStream bais;
                SequenceInputStream sis = null;
                String file_path = getRealFilePath(context, Uri.parse(fileUri));
                long file_size = 0;
                String file_name = null;
                try {
                    File file = new File(file_path);
                    file_size = file.length();
                    file_name = file.getName();
                    fs = new FileInputStream(file);
                    byte[] b = file.getName().getBytes();
                    byte[] info = Arrays.copyOf(b, 256);
                    bais = new ByteArrayInputStream(info);
                    sis = new SequenceInputStream(bais, fs);
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                //将开始时间写入文件
                FileService service = new FileService(context);
                String datastring;
                boolean sendflag = false;
                long starttime, finishtime;
                starttime = System.currentTimeMillis();
                datastring = "S," +
                        "本机: "+host_name+","+
                        "本地IP: " + socket.getLocalAddress() + "," +
                        "目的IP: " + host + "," +
                        "开始时间: " + starttime + ",";
                sendflag = service.saveContentFile("send.txt", datastring
                        .getBytes());
                Log.i(WiFiDirectActivity.TAG, "---->" + sendflag);

                DeviceDetailFragment.copyFile(sis, stream);
                //将结束时间写入文件
                finishtime = System.currentTimeMillis();
                long time = finishtime - starttime;
                datastring = "结束时间: " + finishtime + ",用时: " + time + "ms,文件名称: " + file_name
                        + ",文件大小: " + file_size + "字节"+"\n";
                sendflag = service.saveContentFile("send.txt", datastring
                        .getBytes());
                Log.i(WiFiDirectActivity.TAG, "---->" + sendflag);
                Log.d(WiFiDirectActivity.TAG, " Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {

                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{
                    MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
}
