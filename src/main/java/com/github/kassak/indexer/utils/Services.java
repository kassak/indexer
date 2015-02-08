package com.github.kassak.indexer.utils;

public class Services {
    public static void stopServices(IService ... ss) throws Exception {
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
