// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to record timings of web requests
 * @author Joel Hockey
 */
public class Timer {
    private static final Log log = LogFactory.getLog(Timer.class);
    private long[] times = new long[4];
    private String[] descs = new String[4];
    private int len = 0;
    /** start timer */
    public void start() {
        times[0] = System.currentTimeMillis();
        len = 1;
    }

    /**
     * Save current time and desc to be printed at end
     * @desc description to be printed
     */
    public void mark(String desc) {
        if (len == times.length) {
            long[] tmpTimes = new long[times.length * 2];
            System.arraycopy(times, 0, tmpTimes, 0, times.length);
            times = tmpTimes;
            String[] tmpDescs = new String[descs.length * 2];
            System.arraycopy(descs, 0, tmpDescs, 0, descs.length);
            descs = tmpDescs;
        }
        times[len] = System.currentTimeMillis();
        descs[len++] = desc;
    }

    /**
     * Print total time and desc, as well as any marks
     * @desc description for final item
     */
    public void end(String desc) {
        if (!log.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(System.currentTimeMillis() - times[0]).append(": ").append(desc);
        for (int i = 1; i < len; i++) {
            sb.append(", ").append(times[i] - times[i - 1]).append(": ").append(descs[i]);
        }
        log.info(sb.toString());
    }
}
