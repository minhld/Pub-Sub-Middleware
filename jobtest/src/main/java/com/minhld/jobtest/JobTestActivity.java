package com.minhld.jobtest;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pbsbjob.MidClient;
import com.minhld.pubsublib.Worker;
import com.minhld.utils.Utils;

import butterknife.BindView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobtest);
    }

    private void initSystem() {
        // start workers
        new ExMidWorker();
        new ExMidWorker();

        // start clients
        new ExMidClient();
    }

    class ExMidWorker extends Worker {
        @Override
        public byte[] resolveRequest(byte[] request) {
            // 1. receive the job request
            // 2. analyze the package -> job + data
            // 3. execute job on data and return result
            return new byte[0];
        }
    }

    class ExMidClient extends MidClient {
        public ExMidClient() {
            super(UITools.GO_IP);
        }

        @Override
        public boolean resReqResponse(byte[] response) {
            // whether to dispatch job package or not
            return false;
        }

        @Override
        public void send() {
            // define what to send to the broker
        }

        @Override
        public void resolveResult(byte[] result) {
            // when result comes
        }
    }
}
