package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.FSEventsProcessor;
import com.github.kassak.indexer.fs.FSWatcherService;
import com.github.kassak.indexer.fs.IFSWatcherService;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.factories.IndexProcessorFactory;
import com.github.kassak.indexer.tokenizing.factories.FilesProcessorFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import com.github.kassak.indexer.utils.IService;
import com.github.kassak.indexer.utils.Services;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Indexer implements IService {
    Indexer(ITokenizerFactory tf, int regQueueSize, int queueSize, int fileThreads, int fileQueueSize) {
        indexManager = new IndexManager(tf, new FilesProcessorFactory(queueSize, fileThreads)
                , new IndexProcessorFactory(), fileQueueSize);
        fsWatcher = new FSWatcherService(new FSEventsProcessor(indexManager), regQueueSize);
    }

    @Override
    public void startService() throws Exception {
        Services.startServices(indexManager, fsWatcher);
    }

    @Override
    public void stopService() throws Exception {
        Services.stopServices(fsWatcher, indexManager);
    }

    @Override
    public boolean isRunning() {
        return Services.isServicesRunning(fsWatcher, indexManager);
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return Services.waitServicesFinished(timeout, unit, fsWatcher, indexManager);
    }

    void add(@NotNull String path) throws IOException {
        fsWatcher.registerRoot(FileSystems.getDefault().getPath(path));
    }

    void remove(@NotNull String path) throws IOException {
        fsWatcher.unregisterRoot(FileSystems.getDefault().getPath(path));
    }

    @NotNull Collection<FileEntry> search(@NotNull String word) {
        return indexManager.search(word);
    }

    public @NotNull List<Map.Entry<String, Integer>> getFiles() {
        return indexManager.getFiles();
    }

    private final IFSWatcherService fsWatcher;
    private final IIndexManager indexManager;
}
