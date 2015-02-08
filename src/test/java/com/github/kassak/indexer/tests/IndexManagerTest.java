package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.IndexManager;
import com.github.kassak.indexer.tokenizing.WhitespaceTokenizerFactory;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexManagerTest {
    static {
        final Logger topLogger = Logger.getLogger("");
        Handler h = new ConsoleHandler();
        h.setLevel(Level.FINEST);
        topLogger.setLevel(Level.FINEST);
        topLogger.addHandler(h);
    }

    @Test
    public void  processing() throws InterruptedException {
        IIndexManager im = new IndexManager(new WhitespaceTokenizerFactory(), 10, 10, 100);
        try {
            im.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        im.syncFile(FileSystems.getDefault().getPath("1/1/1"));
        im.syncFile(FileSystems.getDefault().getPath("1/1/2"));
        im.syncFile(FileSystems.getDefault().getPath("1/2/1"));
        im.syncFile(FileSystems.getDefault().getPath("1/2/2"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 4);

        im.syncFile(FileSystems.getDefault().getPath("1/3"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 5);

        im.removeDirectory(FileSystems.getDefault().getPath("1/1"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 3);

        im.removeDirectory(FileSystems.getDefault().getPath("1/2/"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.removeFile(FileSystems.getDefault().getPath("1/3"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 0);

        Path dummy = FileSystems.getDefault().getPath("../qweqweqwe");
        im.syncFile(dummy);

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.syncDirectory(FileSystems.getDefault().getPath(".."));

        Thread.sleep(1000);
        Assert.assertFalse(im.getFiles().contains(dummy.toString()));


        try {
            im.stopService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        im.waitFinished(10, TimeUnit.SECONDS);
    }
}
