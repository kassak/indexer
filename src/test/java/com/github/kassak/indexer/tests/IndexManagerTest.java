package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.IndexManagerService;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.factories.IndexProcessorFactory;
import com.github.kassak.indexer.tokenizing.factories.FilesProcessorServiceFactory;
import com.github.kassak.indexer.tokenizing.factories.WhitespaceTokenizerFactory;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
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

    private static boolean statsContains(List<FileStatistics> fs, String s) {
        for(FileStatistics f : fs)
            if(s.equals(f.name))
                return true;
        return false;
    }

    @Test
    public void  processing() throws InterruptedException {
        IndexManagerService im = new IndexManagerService(new WhitespaceTokenizerFactory()
                , new FilesProcessorServiceFactory(10, 100), new IndexProcessorFactory(), 100);
        try {
            im.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        im.onFileChanged(FileSystems.getDefault().getPath("1/1/2"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/2/1"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/2/2"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/1/1"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 4);

        im.onFileChanged(FileSystems.getDefault().getPath("1/3"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 5);

        im.onDirectoryRemoved(FileSystems.getDefault().getPath("1/1"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 3);

        im.onDirectoryRemoved(FileSystems.getDefault().getPath("1/2/"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.onFileRemoved(FileSystems.getDefault().getPath("1/3"));

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 0);

        Path dummy = FileSystems.getDefault().getPath("../qweqweqwe");
        im.onFileChanged(dummy);

        Thread.sleep(1000);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.onDirectoryChanged(FileSystems.getDefault().getPath(".."));

        Thread.sleep(1000);
        Assert.assertFalse(statsContains(im.getFiles(), dummy.toString()));


        try {
            im.stopService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        im.waitFinished(10, TimeUnit.SECONDS);
    }
}
