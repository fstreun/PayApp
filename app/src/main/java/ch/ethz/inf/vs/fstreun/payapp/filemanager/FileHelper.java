package ch.ethz.inf.vs.fstreun.payapp.filemanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
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

    private Context context;

    public FileHelper(Context context){
        if (context == null){
            throw new IllegalArgumentException("context argument can't be null");
        }
        this.context = context;
    }

    public boolean writeToFile(@Nullable String path, String fileName, String data){
        try {
            return toFile(path, fileName, false, data);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean appendToFile(String path, String fileName, String data){
        try {
            return toFile(path, fileName, true, data);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean toFile(@Nullable String path, String fileName, boolean append, String data) throws IOException {
        if (path == null){
            path = "";
        }

        File directory = new File(context.getFilesDir(), path);
        //Creating an internal dir;
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                return false;
            }
        }
        File file = new File(directory, fileName); //Getting a file within the dir.
        FileOutputStream outputStream = new FileOutputStream(file, append); //Use the stream as usual to write into the file.
        outputStream.write(data.getBytes());
        outputStream.close();
        return true;
    }

    public String readFromFile(@Nullable String path, String fileName) throws FileNotFoundException {
        if (path == null){
            path = "";
        }
        File directory = new File(context.getFilesDir(), path); //Creating an internal dir;
        if (!directory.exists()){
            throw new FileNotFoundException("Path does not exist: " + path);
        }
        File file = new File(directory, fileName); //Getting a file within the dir.
        if (!file.exists()){
            throw new FileNotFoundException("File does not exist: " + path + "/" + fileName);
        }

        FileInputStream inputStream = new FileInputStream(file);

        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            // read file by lines will cause a end line at the end of file...
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public boolean removeFile(@Nullable String path, String fileName){

        File directory = new File(context.getFilesDir(), path);
        //Creating an internal dir;
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                return false;
            }
        }
        File file = new File(directory, fileName); //Getting a file within the dir.
        if(!file.exists()){
            return false;
        }

        boolean deleted = file.delete();

        return deleted;
    }

}
