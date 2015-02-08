package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.FSProcessor;
import com.github.kassak.indexer.fs.FSWatcher;
import com.github.kassak.indexer.fs.IFSWatcher;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class Indexer implements AutoCloseable {
    public static class IndexerException extends Exception {
        IndexerException(Exception e) {
            super(e);
        }
    }

    Indexer(ITokenizerFactory tf, int queueSize, int fileThreads, int fileQueueSize) throws IndexerException {
        this.tf = tf;
        try {
            indexManager = new IndexManager(tf, queueSize, fileThreads, fileQueueSize);
            fsWatcher = new FSWatcher(new FSProcessor(indexManager));
        } catch (IOException e) {
            throw new IndexerException(e);
        } catch (UnsupportedOperationException e) {
            throw new IndexerException(e);
        }
        indexManager.startService();
        fsWatcher.startService();
    }

    void add(String path) throws IOException {
        fsWatcher.registerRoot(FileSystems.getDefault().getPath(path));
    }

    void remove(String path) throws IOException {
        fsWatcher.unregisterRoot(FileSystems.getDefault().getPath(path));
    }

    Collection<FileEntry> search(String word) {
        return indexManager.search(word);
    }

    public void close() throws Exception {
        fsWatcher.stopService();
        fsWatcher.waitFinished(10, TimeUnit.DAYS);
        indexManager.stopService();
        indexManager.waitFinished(10, TimeUnit.DAYS);
    }

    private final IFSWatcher fsWatcher;
    private final ITokenizerFactory tf;
    private final IIndexManager indexManager;
}
