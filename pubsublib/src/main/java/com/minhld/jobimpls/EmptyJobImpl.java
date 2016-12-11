package com.minhld.jobimpls;

import com.minhld.jobex.Job;
import com.minhld.utils.Utils;

/**
 * Created by minhld on 12/10/2016.
 */
public class EmptyJobImpl implements Job {

	public Object exec(Object input) {
        // this will do nothing but waiting for very long - WAITING_TIME
        // int partNum = (Integer) input;
        // int partWaitTime = WAITING_TIME / partNum;
        try {
            Thread.sleep(Utils.WAITING_TIME);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "finished waiting!";
    }
}
