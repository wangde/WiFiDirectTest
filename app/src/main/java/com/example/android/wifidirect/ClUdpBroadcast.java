package com.example.android.wifidirect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by XiaoDe on 2016/9/9.
 * using udp broadcast the ip
 */
public class ClUdpBroadcast extends Thread {
    DatagramSocket sender = null;
    DatagramPacket dj = null;
    InetAddress group = null;

    byte[] data = new byte[1024]; //data <= 1024
    String ip;

    public ClUdpBroadcast(String dataString,String ip) {
        this.data = dataString.getBytes();
        this.ip = ip;
    }


    @Override
    public void run() {
        try {
            sender = new DatagramSocket();
            group = InetAddress.getByName(ip);
            dj = new DatagramPacket(data,data.length,group,6789);
            sender.send(dj);
            sender.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


}
