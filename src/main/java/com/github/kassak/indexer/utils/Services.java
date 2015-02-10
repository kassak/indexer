package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Services {
    public static boolean waitServicesFinished(long timeout, @NotNull TimeUnit unit, @NotNull IService ... ss)
            throws InterruptedException {
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        for(IService s : ss) {
            long dur = end - System.currentTimeMillis();
            if(dur <= 0)
                dur = 0;
            if(!s.waitFinished(unit.convert(dur, TimeUnit.MILLISECONDS), unit))
                return false;
        }
        return true;
    }
    public static boolean isServicesRunning(@NotNull IService ... ss) {
        for(IService s : ss)
            if(!s.isRunning())
                return false;
        return true;
    }
    public static void startServices(@NotNull IService ... ss) throws Exception {
        for(int i = 0; i < ss.length; ++i) {
            try {
                ss[i].startService();
            } catch (Exception e) {
                stopServices(Arrays.copyOfRange(ss, 0, i));
                throw e;
            }
        }
    }
    public static void stopServices(@NotNull IService ... ss) throws Exception {
        Exception ex = null;
        for(IService s : ss) {
            try {
                s.stopService();
            } catch (Exception e) {
                ex = e;
            }
        }
        if(ex != null)
            throw ex;
    }
}
