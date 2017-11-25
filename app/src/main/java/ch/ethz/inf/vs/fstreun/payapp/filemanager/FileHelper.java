package ch.ethz.inf.vs.fstreun.payapp.filemanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by fabio on 11/25/17.
 */

public class FileHelper {

    Context context;

    public FileHelper(Context context){
        if (context == null){
            throw new IllegalArgumentException("context argument can't be null");
        }
        this.context = context;
    }

    public void writeToFile(String fileName, String data){
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            // Not able to create a file
            // If it can be created it will create a file!
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFromFile(String fileName) throws FileNotFoundException {
        //TODO: read file by lines will cause a end line at the end of file...
        FileInputStream inputStream;

        inputStream = context.openFileInput(fileName);

        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

}
