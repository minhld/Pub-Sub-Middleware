package com.minhld.jobimpls;

import com.minhld.jobex.Job;

/**
 * Created by minhld on 12/10/2016.
 */
public class EmptyJobImpl implements Job {
    public static final int WAITING_TIME = 30000;

	public Object exec(Object input) {
        // this will do nothing but waiting for very long - WAITING_TIME
        // int partNum = (Integer) input;
        // int partWaitTime = WAITING_TIME / partNum;
        try {
            Thread.sleep(WAITING_TIME);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "finished waiting!";
    }
}
