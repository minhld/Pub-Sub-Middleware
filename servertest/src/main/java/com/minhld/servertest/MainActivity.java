package com.minhld.servertest;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoText = (TextView) findViewById(R.id.infoText);
        imageView = (ImageView) findViewById(R.id.imageView);

        sendJobBtn = (Button) findViewById(R.id.sendJobBtn);
        sendJobBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initClient() {
        Client client = new Client(Utils.BROKER_SPECIFIC_IP) {
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
                UITools.writeLog(MainActivity.this, infoText, "client received result: " + result.length + " bytes");
                JobDataParserImpl parser = new JobDataParserImpl();
                try {
                    final Bitmap bmpRes = (Bitmap) parser.parseBytesToObject(result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bmpRes);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

    }
}
