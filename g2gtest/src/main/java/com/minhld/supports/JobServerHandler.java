package com.minhld.supports;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobServerHandler extends Handler {

    Activity parent;
    Handler mainUiHandler;
    Object finalObject;

    public JobServerHandler(Activity parent, Handler uiHandler) {
        this.parent = parent;
        this.mainUiHandler = uiHandler;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Utils.MESSAGE_READ_CLIENT: {

                break;
            }
            case Utils.MESSAGE_READ_SERVER: {

                break;
            }
            case Utils.MESSAGE_READ_JOB_SENT: {

                break;
            }
            case Utils.JOB_FAILED: {
                String exStr = (String) msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, exStr).sendToTarget();
                break;
            }
            case Utils.MESSAGE_INFO: {
                // self instruction, don't care
                Object obj = msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, msg.obj + "").sendToTarget();
                break;
            }

        }
    }

}
