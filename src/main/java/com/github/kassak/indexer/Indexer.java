package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.FSProcessor;
import com.github.kassak.indexer.fs.FSWatcher;
import com.github.kassak.indexer.fs.IFSWatcher;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;
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
    Indexer(ITokenizerFactory tf, int queueSize, int fileThreads, int fileQueueSize) {
        indexManager = new IndexManager(tf, queueSize, fileThreads, fileQueueSize);
        fsWatcher = new FSWatcher(new FSProcessor(indexManager));
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

    private final IFSWatcher fsWatcher;
    private final IIndexManager indexManager;
}
