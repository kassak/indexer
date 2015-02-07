package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Iterator;

public class Indexer implements AutoCloseable {
    public static class IndexerException extends Exception {
        IndexerException(Exception e) {
            super(e);
        }
    }

    Indexer(ITokenizerFactory tf, IVocabularyFactory vf) throws IndexerException {
        this.tf = tf;
        this.vf = vf;
        try {
            fsWatcher = new FSWatcher(new FSProcessor(new IndexManager()));
        } catch (IOException e) {
            throw new IndexerException(e);
        } catch (UnsupportedOperationException e) {
            throw new IndexerException(e);
        }
        fsWatcherThread = new Thread(fsWatcher);
        fsWatcherThread.start();
    }

    void add(String path) throws IOException {
        fsWatcher.registerRoot(FileSystems.getDefault().getPath(path));
    }

    Iterator<String> search(String word) {
        return null;
    }

    public void close() throws Exception {
        fsWatcherThread.interrupt();
        fsWatcherThread.join();
    }

    private FSWatcher fsWatcher;
    private Thread fsWatcherThread;
    private IVocabulary<WordData> index;
    private ITokenizerFactory tf;
    private IVocabularyFactory vf;
}
