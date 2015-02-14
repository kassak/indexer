package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.IndexManagerService;
import com.github.kassak.indexer.storage.factories.IndexProcessorFactory;
import com.github.kassak.indexer.tokenizing.IFileProcessingResults;
import com.github.kassak.indexer.tokenizing.IFilesProcessorService;
import com.github.kassak.indexer.tokenizing.factories.IFilesProcessorServiceFactory;
import com.github.kassak.indexer.tokenizing.factories.WhitespaceTokenizerFactory;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OverflowTest {
    static {
        final Logger topLogger = Logger.getLogger("");
        Handler h = new ConsoleHandler();
        h.setLevel(Level.FINEST);
        topLogger.setLevel(Level.FINEST);
        topLogger.addHandler(h);
    }

    class DummyFilesProcessorFactory implements IFilesProcessorServiceFactory {
        @NotNull
        @Override
        public IFilesProcessorService create(@NotNull IFileProcessingResults im) {
            return new IFilesProcessorService() {
                @Override
                public boolean processFile(@NotNull Path f) {
                    return false;
                }

                @Override
                public void startService() throws FailureException {

                }

                @Override
                public void stopService() {

                }

                @Override
                public boolean isRunning() {
                    return true;
                }

                @Override
                public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
                    return true;
                }
            };
        }
    }

    class AngryThread implements Runnable {
        public AngryThread(IndexManagerService proc, boolean spamFiles) {
            this.proc = proc;
            lastSeen = System.currentTimeMillis();
            finished = false;
            this.spamFiles = spamFiles;
        }

        @Override
        public void run() {
            try {
                for(int i = 0; i < 1000000; ++i) {
                    lastSeen = System.currentTimeMillis();
                    if(spamFiles)
                        proc.onFileChanged(FileSystems.getDefault().getPath("file-" + i));
                    else
                        proc.addWordToIndex(FileSystems.getDefault().getPath("file-0"), "blah");
                }
                finished = true;
            } catch (InterruptedException e) {
            }
        }
        public volatile long lastSeen;
        public volatile boolean finished;
        private final IndexManagerService proc;
        private final boolean spamFiles;
    }

    static boolean isAngryThreadHang(AngryThread at) throws InterruptedException {
        boolean hang = false;
        while (true) {
            long HANG_TIMEOUT = 5000; //5 sec
            long ls = at.lastSeen;
            long tm = System.currentTimeMillis();
            if(tm - ls > HANG_TIMEOUT) {
                hang = !at.finished;
                break;
            }
            Thread.sleep(HANG_TIMEOUT - (tm - ls));
        }
        return hang;
    }

    @Test
    public void stopFilesProcessing() throws InterruptedException, IService.FailureException {
        IndexManagerService im = new IndexManagerService(new WhitespaceTokenizerFactory()
                , new DummyFilesProcessorFactory(), new IndexProcessorFactory(), 100);
        im.startService();

        { // overflow system
            AngryThread t = new AngryThread(im, true);
            Thread tt = new Thread(t);
            tt.start();
            boolean hang = isAngryThreadHang(t);
            tt.interrupt();
            Assert.assertTrue(hang);
        }

        { // check that IndexManagerService still running
            AngryThread t = new AngryThread(im, false);
            Thread tt = new Thread(t);
            tt.start();
            boolean hang = isAngryThreadHang(t);
            tt.interrupt();
            Assert.assertFalse(hang);
        }

        im.stopService();
        im.waitFinished(10, TimeUnit.SECONDS);
    }
}
