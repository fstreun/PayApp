package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

import static org.junit.Assert.assertEquals;

/**
 * Created by fabio on 11/25/17.
 * Testing FileHelper class
 */

@RunWith(AndroidJUnit4.class)
public class FileHelperTest {

    @Test
    public void fileHelper_test() throws Exception {
        String fileName = "testFile";
        String data = "test Data\n second line\n";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);
        assertTrue(fileHelper.writeToFile(null, fileName, data));

        String s = fileHelper.readFromFile(null, fileName);
        assertEquals(data, s);

    }

    @Test
    public void fileHelper_overwrite() throws Exception {

        String fileName = "testFile";
        String data1 = "test Data\n second line\n";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);
        assertTrue(fileHelper.writeToFile(null, fileName, data1));

        String data2 = "test Data2\n";
        assertTrue(fileHelper.writeToFile(null, fileName, data2));

        String s = fileHelper.readFromFile(null, fileName);
        assertEquals(data2, s);

    }

    @Test
    public void fileHelper_append() throws Exception {

        String fileName = "testFile";
        String data1 = "test Data\n second line\n";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);
        assertTrue(fileHelper.writeToFile(null, fileName, data1));

        String data2 = "test Data2\n";
        assertTrue(fileHelper.appendToFile(null, fileName, data2));

        String s = fileHelper.readFromFile(null, fileName);
        assertEquals(data1 + data2, s);

    }

    @Test
    public void fileHelper_path() throws Exception {

        String path = "test/path";
        String fileName = "testFile";
        String data1 = "test Data\n second line\n";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);
        assertTrue(fileHelper.writeToFile(path, fileName, data1));

        String s = fileHelper.readFromFile(path, fileName);
        assertEquals(data1, s);

    }

    @Test(expected = FileNotFoundException.class)
    public void fileHelper_wrongFileName() throws Exception {

        String path = null;
        String fileName = "FileNotExisting";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);

        String s = fileHelper.readFromFile(path, fileName);
    }

    @Test(expected = FileNotFoundException.class)
    public void fileHelper_wrongPath() throws Exception {

        String path = "Path/Not/Existing";
        String fileName = "filename";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);

        String s = fileHelper.readFromFile(path, fileName);
    }
}
