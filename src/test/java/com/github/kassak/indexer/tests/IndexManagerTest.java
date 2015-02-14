package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.IndexManagerService;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.factories.IndexProcessorFactory;
import com.github.kassak.indexer.tests.util.IndexerTesting;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.factories.FilesProcessorServiceFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import com.github.kassak.indexer.tokenizing.factories.WhitespaceTokenizerFactory;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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

    private static class BazingaTokenizerFactory implements ITokenizerFactory {
        @Override
        public ITokenizer create(@NotNull Path file) throws IOException {
            return new ITokenizer() {
                @Override
                public void close() throws Exception {}

                @Override
                public boolean hasNext() {
                    return has;
                }

                @Override
                public String next() {
                    has = false;
                    return "Bazinga!";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Bazinga!");
                }
                private boolean has = true;
            };
        }
    }

    @Test
    public void addingFilesToIndex() throws InterruptedException, IService.FailureException {
        IndexManagerService im = new IndexManagerService(new WhitespaceTokenizerFactory()
                , new FilesProcessorServiceFactory(2, 10), new IndexProcessorFactory(), 10);
        im.startService();

        im.onFileChanged(FileSystems.getDefault().getPath("1/1/2"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/2/1"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/2/2"));
        im.onFileChanged(FileSystems.getDefault().getPath("1/1/1"));

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 4);

        im.onFileChanged(FileSystems.getDefault().getPath("1/3"));

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 5);

        im.onDirectoryRemoved(FileSystems.getDefault().getPath("1/1"));

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 3);

        im.onDirectoryRemoved(FileSystems.getDefault().getPath("1/2/"));

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.onFileRemoved(FileSystems.getDefault().getPath("1/3"));

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 0);

        Path dummy = FileSystems.getDefault().getPath("../qweqweqwe");
        im.onFileChanged(dummy);

        IndexerTesting.waitIdle(im);
        Assert.assertEquals(im.getFiles().size(), 1);

        im.onDirectoryChanged(FileSystems.getDefault().getPath(".."));

        IndexerTesting.waitIdle(im);
        Assert.assertFalse(statsContains(im.getFiles(), dummy.toString()));


        im.stopService();
        im.waitFinished(10, TimeUnit.SECONDS);
    }

    @Test
    public void thousandFiles() throws InterruptedException, IService.FailureException {
        IndexManagerService im = new IndexManagerService(new BazingaTokenizerFactory()
                , new FilesProcessorServiceFactory(2, 10), new IndexProcessorFactory(), 10);
        im.startService();

        for(int i = 0; i < 1000; ++i)
            im.onFileChanged(FileSystems.getDefault().getPath("file-" + i));

        IndexerTesting.waitIdle(im);

        Assert.assertEquals(im.search("Bazinga!").size(), 1000);

        Assert.assertEquals(im.getFiles().size(), 1000);
        for(FileStatistics fs : im.getFiles())
            Assert.assertEquals(fs.wordsNum, 1);

        im.stopService();
        im.waitFinished(10, TimeUnit.SECONDS);
    }
}
