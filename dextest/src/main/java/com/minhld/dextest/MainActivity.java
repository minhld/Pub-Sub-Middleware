package com.minhld.dextest;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.minhld.jobex.JobDataParser;
import com.minhld.utils.Utils;

import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);

        loadJarAsync();
    }

    Bitmap loadedBmp;

    private void loadJarAsync() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    loadedBmp = loadJar();
                    publishProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Object[] values) {
                // display the bitmap
                imageView.setImageBitmap(loadedBmp);
            }
        }.execute();
    }

    private Bitmap loadJar() throws Exception {
        // save the job data to file
        String objectPath = Utils.getDownloadPath() + "/Job.jar";

        //
        String dexDir = getDir("dex", 0).getAbsolutePath();
        ClassLoader parent  = getClass().getClassLoader();
        DexClassLoader loader = new DexClassLoader(objectPath, dexDir, null, parent);
        Class dataParserClass = loader.loadClass(Utils.PARSER_CLASS_NAME);
        JobDataParser dataParser = (JobDataParser) dataParserClass.newInstance();

        String imagePath = Utils.getDownloadPath() + "/mars.jpg";
        return (Bitmap) dataParser.loadObject(imagePath);
    }
}
