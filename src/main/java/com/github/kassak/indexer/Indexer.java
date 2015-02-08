package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.FSProcessor;
import com.github.kassak.indexer.fs.FSWatcher;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Collection;

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
        indexManagerThread = new Thread(indexManager);
        indexManagerThread.start();
        fsWatcherThread = new Thread(fsWatcher);
        fsWatcherThread.start();
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
        fsWatcherThread.interrupt();
        fsWatcherThread.join();
    }

    private final FSWatcher fsWatcher;
    private final Thread fsWatcherThread;
    private final ITokenizerFactory tf;
    private final IIndexManager indexManager;
    private final Thread indexManagerThread;
}
