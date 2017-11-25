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
    public void fileHelper_tests() throws Exception {
        String fileName = "testFile";
        String data = "test Data\n second line\n";
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        FileHelper fileHelper = new FileHelper(appContext);
        fileHelper.writeToFile(fileName, data);

        String s = fileHelper.readFromFile(fileName);
        assertEquals(data, s);

    }
}
