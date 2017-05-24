/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device, mydevice, itemdevice;
    private WifiP2pInfo info;
    private String clip;
    private static Boolean flag = false;
    private String myname;
    Handler handler;
    ProgressDialog progressDialog = null;
    HashMap<String, String> map = new HashMap<String, String>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String filepath = Environment.getExternalStorageDirectory() + "/"
                        + "com.example.android.wifidirect";
                WifiP2pConfig config = new WifiP2pConfig();
                GOIntent goIntent = new GOIntent(filepath);
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent= goIntent.getGOIntent();
                Log.i(WiFiDirectActivity.TAG, "------------->" +goIntent.getGOIntent());
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                FileService service = new FileService(getActivity());
                String datastring;
                boolean flag = false;
                long  starttime;
                starttime = System.currentTimeMillis();
                datastring = "Connect, 目标MAC" + config.deviceAddress +
                        ",开始时间: " + starttime + ",";
                flag = service.saveContentFile("connect.txt", datastring
                        .getBytes());
                Log.i(WiFiDirectActivity.TAG, "---->" + flag);
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                        intent.setType("image/*");
                        intent.setType("video/;image/*  *");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);

                    }
                });

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundle = msg.getData();
                switch (msg.what) {
                    case 0x000:
                        TextView status = (TextView) mContentView.findViewById(R.id.status_text);
                        status.setText("File copied - " + bundle.getString("status"));
                        break;
                    case 0x111:
//                        clip = bundle.getString("ip");
                        map.put(bundle.getString("name"), bundle.getString("ip"));
                        Toast.makeText(getActivity(), "收到来自：" + bundle.getString("name")
                                        + "  ip:" + bundle.getString("ip") + "的消息",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        };

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
//        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
//                "192.168.49.1");
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_PORT, 8988);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_CLIENT_ADDRESS, clip);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER, flag);
        serviceIntent.putExtra(FileTransferService.EXTRAS_HOST_NAME,myname);
        getActivity().startService(serviceIntent);

    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));
        flag = info.isGroupOwner;

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {

            //GO接收client端发来的线程
            new GoUdpReceive(handler).start();
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();



        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.

            new ClUdpBroadcast(myname, info.groupOwnerAddress.getHostAddress().toString()).start();

            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }


    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    public void getMyDevice(WifiP2pDevice device) {
        this.mydevice = device;
        myname = device.deviceName;
    }

    public void getItemDevice(WifiP2pDevice device) {
        this.itemdevice = device;
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            if (key.equals(itemdevice.deviceName)) {
                clip = val;
                break;
            }
        }
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
//        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
//        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;


        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;

        }

        @Override
        protected String doInBackground(Void... params) {
            while (true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(8988);
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                    Socket client = serverSocket.accept();
                    //将开始接收时间写入文件
                    // client.getLocalAddress().getHostName() 本地ip
                    // getInetAddress().getHostAddress() 对方ip
                    FileService service = new FileService(context);
                    String datastring;
                    boolean flag = false;
                    long starttime, finishtime;
                    starttime = System.currentTimeMillis();
                    datastring = "R," +
                            "本地ip: " + client.getLocalAddress().getHostName() + "," +
                            "对方ip: " + client.getInetAddress().getHostAddress().toString() + "," +
                            "开始时间: " + starttime + ",";
                    flag = service.saveContentFile("receive.txt", datastring
                            .getBytes());
                    Log.i(WiFiDirectActivity.TAG, "---->" + flag);
                    Log.d(WiFiDirectActivity.TAG, "Server: connection done");


                    BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
                    byte[] info = new byte[256];
                    bis.read(info);
                    String file_name = new String(info).trim();
                    final File f = new File(Environment.getExternalStorageDirectory() + "/"
                            + context.getPackageName() + "/" + file_name);


                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                    copyFile(bis, new FileOutputStream(f));
                    //将结束时间写入文件
                    finishtime = System.currentTimeMillis();
                    long time = finishtime - starttime;
                    datastring = "结束时间: " + finishtime + ",用时: " + time + "ms,文件名称: "
                            + file_name + ",文件大小: " + f.length() + "字节" + "\n";
                    flag = service.saveContentFile("receive.txt", datastring
                            .getBytes());
                    Log.i(WiFiDirectActivity.TAG, "---->" + flag);
                    serverSocket.close();
                    return f.getAbsolutePath();

                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    return null;
                } finally {

                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}
