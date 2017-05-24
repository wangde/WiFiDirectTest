package com.example.android.wifidirect;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;


/**
 * Created by XiaoDe on 2016/9/14.
 */
public class FileService {

    private Context context;

    public FileService(Context context) {
        this.context = context;
    }

    public boolean saveContentFile(String fileName, byte[] data) {
        boolean flag = false;
        FileOutputStream outputStream = null;
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/"
                    +"testdata"+"/"+ context.getPackageName());

            if (!folder.exists()) {
                folder.mkdirs();
            }
            outputStream = new FileOutputStream(folder.getAbsolutePath()+"/"+fileName,true);
//            context.openFileOutput(fileName, mode);
            outputStream.write(data, 0, data.length);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

        }

        return flag;
    }
}
