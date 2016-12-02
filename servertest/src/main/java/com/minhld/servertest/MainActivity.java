package com.minhld.servertest;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.minhld.jobex.JobDataParser;
import com.minhld.jobex.JobPackage;
import com.minhld.pbsbjob.MidWorker;
import com.minhld.pubsublib.Broker;
import com.minhld.pubsublib.Client;
import com.minhld.pubsublib.Worker;
import com.minhld.utils.Utils;

public class MainActivity extends AppCompatActivity {

    Button sendJobBtn;
    ImageView imageView;
    TextView infoText;

    long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoText = (TextView) findViewById(R.id.infoText);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        imageView = (ImageView) findViewById(R.id.imageView);

        sendJobBtn = (Button) findViewById(R.id.sendJobBtn);
        sendJobBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initClient();
            }
        });
    }

    private void initClient() {
        Client client = new Client("129.123.7.172") {
            @Override
            public void clientStarted(String clientId) {
                // print out
                UITools.writeLog(MainActivity.this, infoText, "client [" + clientId + "] started");
            }

            @Override
            public void send() {
                // dispatch jobs to clients
                String dataPath = Utils.getDownloadPath() + "/mars.jpg";
                String jobPath = Utils.getDownloadPath() + "/job.jar";

                startTime = System.currentTimeMillis();

                try {
                    JobSupporter.initDataParser(MainActivity.this, jobPath);
                    byte[] jobData = JobSupporter.getData(dataPath);

                    JobPackage job = new JobPackage(0, this.clientId, jobData, jobPath);
                    byte[] jobPkg = job.toByteArray();

                    // print out
                    UITools.writeLog(MainActivity.this, infoText, "client sent: " + jobPkg.length);

                    this.sendMessage(jobPkg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void resolveResult(byte[] result) {
                final long durr = System.currentTimeMillis() - startTime;

                UITools.writeLog(MainActivity.this, infoText, "client received result: " + result.length + " bytes");
                JobDataParser parser = new WordDataParserImpl();
                try {
                    final String resultStr = (String) parser.parseBytesToObject(result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UITools.writeLog(MainActivity.this, infoText, resultStr);
                            UITools.writeLog(MainActivity.this, infoText, "total time: " + durr + "ms");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

    }
}
