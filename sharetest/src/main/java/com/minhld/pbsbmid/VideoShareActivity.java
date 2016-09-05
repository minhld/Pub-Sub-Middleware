package com.minhld.pbsbmid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.minhld.pubsublib.Publisher;
import com.minhld.pubsublib.Subscriber;
import com.minhld.wfd.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class VideoShareActivity extends AppCompatActivity {
    @BindView(R.id.toActiveMqBtn)
    Button createGroupBtn;

    @BindView(R.id.to0mqBtn)
    Button discoverBtn;

    @BindView(R.id.toRabbitMqBtn)
    Button pubBtn;

    @BindView(R.id.subBtn)
    Button subBtn;

    @BindView(R.id.deviceList)
    ListView deviceList;

    @BindView(R.id.infoText)
    TextView infoText;

    @BindView(R.id.imageView)
    ImageView imager;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MESSAGE_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(VideoShareActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    MidSupporter midSupporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_share);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        midSupporter = new MidSupporter(this, mainUiHandler);

        deviceList.setAdapter(midSupporter.getDeviceListAdapter());

        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                midSupporter.createGroup();
            }
        });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                midSupporter.discoverPeers();
            }
        });

        pubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ExPublisher();
            }
        });

        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Subscriber subscriber = new Subscriber(UITools.GO_IP, Utils.BROKER_XPUB_PORT, new String[] { "video_frame" });
                subscriber.setMessageListener(new Subscriber.MessageListener() {
                    @Override
                    public void msgReceived(String topic, final byte[] msg) {
                        // msg - bitmap data
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap frame = BitmapFactory.decodeByteArray(msg, 0, msg.length);
                                imager.setImageBitmap(frame);
                            }
                        });

                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        midSupporter.actOnPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        midSupporter.actOnResume();
    }

    public class ExPublisher extends Publisher {
        Bitmap currentFrame;
        FFmpegMediaMetadataRetriever retriever;
        int frmIndex = 1;

        public ExPublisher() {
            super(UITools.GO_IP);
            this.setSendInterval(10);
        }

        @Override
        protected void prepare() {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/selena.mp4");
            retriever = new FFmpegMediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
        }

        @Override
        public void send() {
            try {
                currentFrame = retriever.getFrameAtTime(100000 * frmIndex, MediaMetadataRetriever.OPTION_CLOSEST);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                currentFrame.compress(Bitmap.CompressFormat.PNG, 80, stream);
                sendFrame("video_frame", stream.toByteArray());
                UITools.writeLog(VideoShareActivity.this, infoText, "frame sent: " + stream.size());
                stream.close();
                frmIndex++;
            } catch (Exception e) {
                e.printStackTrace();
            }
//            finally {
//                try {
//                    retriever.release();
//                } catch (RuntimeException ex) {
//                }
//            }
        };
    }

}
