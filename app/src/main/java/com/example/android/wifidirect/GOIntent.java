package com.example.android.wifidirect;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by XiaoDe on 2016/9/23.
 */

public class GOIntent {
    String filepath;
    private List<String> items = null;//存放名称
    private List<String> paths = null;//存放路径
    private List<Long> time = null;
    private static int SIZE=20;
    int count=0;

    GOIntent(String filepath) {
        this.filepath = filepath;
    }

    public void getFilelist() {
        try {
            items = new ArrayList<String>();
            paths = new ArrayList<String>();
            time = new ArrayList<Long>();
            File f = new File(filepath);
            File[] files = f.listFiles();
            // 将所有文件存入list中
            if (files != null) {
                count = files.length;// 文件个数
                for (int i = 0; i < count; i++) {
                    File file = files[i];
                    items.add(file.getName());
                    paths.add(file.getPath());
                    time.add(file.lastModified());
                }
                Collections.sort(time);
            }

        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }
    public int getGOIntent(){
        int intent;
        int recentFileCount= 0 ;
        int timeIntent ;
        getFilelist();
        for(int i = 0 ;i<SIZE&&i<time.size();i++){
            if(System.currentTimeMillis()-time.get(i)<300000){
                recentFileCount++;
            }
        }
        timeIntent = recentFileCount+7;
        intent=(timeIntent>15)?15:timeIntent;
        return intent;
    }


}
