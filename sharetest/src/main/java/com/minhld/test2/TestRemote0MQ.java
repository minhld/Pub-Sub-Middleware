package com.minhld.test2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.minhld.pbsbmid.R;
import com.minhld.pbsbmid.UITools;
import com.minhld.pubsublib.Subscriber;
import com.minhld.wfd.Utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TestRemote0MQ extends AppCompatActivity {
    @BindView(R.id.to0mqBtn)
    Button to0mqBtn;

    @BindView(R.id.toActiveMqBtn)
    Button toActiveMqBtn;

    @BindView(R.id.toRabbitMqBtn)
    Button toRabbitMqBtn;

    @BindView(R.id.infoText)
    TextView infoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_remote0_mq);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        to0mqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Subscriber subscriber = new Subscriber(UITools.SERVER_IP, Utils.BROKER_XPUB_PORT, new String[] { "0MQ-test" });
                subscriber.setMessageListener(new Subscriber.MessageListener() {
                    @Override
                    public void msgReceived(String topic, final byte[] msg) {

                        new TestRemote0MQ.GetTimeServer(new TestRemote0MQ.TimeListener() {
                            @Override
                            public void timeReceived(long time) {
                                String[] timeData = new String(msg).trim().split(" ");
                                long serverTime = Long.parseLong(timeData[1]);
                                Utils.appendTestInfo(timeData[0] + "k_svr_package", "" + serverTime, time);

                                long diffTime = time - serverTime;
                                UITools.writeLog(TestRemote0MQ.this, infoText, "diff: " + diffTime);
                            }
                        });
                    }
                });
            }
        });

        toActiveMqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        toRabbitMqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    // TEST:
    class GetTimeServer extends Thread {
        TestRemote0MQ.TimeListener timeListener;

        public GetTimeServer(TestRemote0MQ.TimeListener _timeListener) {
            this.timeListener = _timeListener;
            this.start();
        }

        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                String timeServer = "http://129.123.7.172:3883/sm/getTime";
                HttpURLConnection conn = (HttpURLConnection) new URL(timeServer).openConnection();
                int code = conn.getResponseCode();
                if (code == 200) {
                    String lTime = IOUtils.toString(conn.getInputStream());
                    long time = Long.parseLong(lTime);
                    if (timeListener != null) {
                        timeListener.timeReceived(time);
                    }

                } else {
                    UITools.writeLog(TestRemote0MQ.this, infoText, "error: server unreached");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    interface TimeListener {
        public void timeReceived(long time);
    }

}
