package com.minhld.jobtest;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pbsbjob.MidWorker;
import com.minhld.pubsublib.Broker;
import com.minhld.pubsublib.Client;
import com.minhld.pubsublib.Worker;
import com.minhld.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class JobTestActivity extends AppCompatActivity {
    @BindView(R.id.createGroupBtn)
    Button createGroupBtn;

    @BindView(R.id.discoverBtn)
    Button discoverBtn;

    @BindView(R.id.pubBtn)
    Button pubBtn;

    @BindView(R.id.subBtn)
    Button subBtn;

    @BindView(R.id.deviceList)
    ListView deviceList;

    @BindView(R.id.infoText)
    TextView infoText;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(JobTestActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    MidSupporter midSupporter;
    WifiPeerListAdapter deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobtest);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        midSupporter = new MidSupporter(this, mainUiHandler);

        deviceList.setAdapter(midSupporter.getDeviceListAdapter());

        pubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSystem();
            }
        });

        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initClient();
            }
        });
    }

    /**
     * init the basic infrastructure including broker and workers
     */
    private void initSystem() {
        String brokerIp = "*";
        // start server (broker)
        new Broker(brokerIp);

        // start workers
        new MidWorker(this, brokerIp);
        new MidWorker(this, brokerIp);

    }

    /**
     * init client - on client devices
     */
    private void initClient() {
        new Client(Utils.BROKER_SPECIFIC_IP) {

            @Override
            public void send() {
                // dispatch jobs to clients
                String dataPath = Utils.getDownloadPath() + "/mars.jpg";
                String jobPath = Utils.getDownloadPath() + "/Job.jar";
                //jobHandler.dispatchJob(useCluster, dataPath, jobPath);
                //this.sendMessage();
                try {
                    JobSupporter.initDataParser(JobTestActivity.this, jobPath);
                    byte[] jobData = JobSupporter.getData(dataPath);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void resolveResult(byte[] result) {
                //
            }
        };
    }
}
