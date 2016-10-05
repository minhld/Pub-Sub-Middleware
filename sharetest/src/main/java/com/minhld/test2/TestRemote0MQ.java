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
import com.minhld.utils.Utils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

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
                // TEST:
                // create PubSub client to listen to report from server

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
                // TEST:
                // create RabbitMQ client to listen to its server
                new RabbitClient().start();
            }
        });
    }



    // TEST: RABBIT implementation
    private final String SERVER_IP = "129.123.7.172";
    private static final String RABBIT_EXCHANGE_NAME = "logs";

    class RabbitClient extends Thread {
        public RabbitClient() {
            UITools.writeLog(TestRemote0MQ.this, infoText, "Rabbit client started");
        }

        public void run() {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(SERVER_IP);
                factory.setUsername("minhld");
                factory.setPassword("minh@123");

                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.exchangeDeclare(RABBIT_EXCHANGE_NAME, "fanout");
                String queueName = channel.queueDeclare().getQueue();
                channel.queueBind(queueName, RABBIT_EXCHANGE_NAME, "");

                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, true, consumer);

                while (true) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    final String message = new String(delivery.getBody());

                    new TestRemote0MQ.GetTimeServer(new TestRemote0MQ.TimeListener() {
                        @Override
                        public void timeReceived(long time) {
                            String[] msgParts = message.trim().split(" ");
                            long startProcessTime = Long.parseLong(new String(msgParts[1]).trim());
                            Utils.appendTestInfo(msgParts[0] + "k_rabbit", "" + startProcessTime, time);

                            long diffTime = time - startProcessTime;
                            UITools.writeLog(TestRemote0MQ.this, infoText, "diff: " + diffTime);
                        }
                    });

//                    UITools.writeLog(TestRemote0MQ.this, infoText, "msg: " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get time from server 129.123.7.172
     */
    class GetTimeServer extends Thread {
        TestRemote0MQ.TimeListener timeListener;

        public GetTimeServer(TestRemote0MQ.TimeListener _timeListener) {
            this.timeListener = _timeListener;
            this.start();
        }

        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                String timeServer = "http://" + SERVER_IP + ":3883/sm/getTime";
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
