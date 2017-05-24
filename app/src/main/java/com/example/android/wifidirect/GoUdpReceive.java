package com.example.android.wifidirect;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by XiaoDe on 2016/9/11.
 */
public class GoUdpReceive extends Thread {

    DatagramSocket receiveds = null;
    DatagramPacket receivedp;
    byte[] receivedata = new byte[1024];
    Handler handler = new Handler();
    Message msg;

    GoUdpReceive(Handler handler){
        this.handler=handler;
    }
    @Override
    public void run() {
        try {
            receiveds = new DatagramSocket(6789);

        }catch (IOException e){
            e.printStackTrace();
        }
        while (true) {
            try {
                receivedp = new DatagramPacket(receivedata, receivedata.length);
                if (receiveds != null)
                    receiveds.receive(receivedp);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (receivedp.getAddress() != null) {
                final String quest_ip = receivedp.getAddress().toString();
                final String codeString = new String(receivedata, 0, receivedp.getLength());

                //从消息池中获取消息，如果没有消息，创建一个消息，如果有取出来消息携带数据，又handler发送
                // 不要用new Message()方法容易内存溢出
                msg = Message.obtain();
                msg.what = 0x111;
                Bundle msgdata = new Bundle();
                msgdata.putString("name",codeString);
                msgdata.putString("ip",receivedp.getAddress().toString().substring(1));
                msg.setData(msgdata);
                handler.sendMessage(msg);
                break;
            }
        }
                super.run();
    }
}
