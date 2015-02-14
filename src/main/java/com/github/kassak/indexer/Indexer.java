package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.FSWatcherService;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.factories.IndexProcessorFactory;
import com.github.kassak.indexer.tokenizing.factories.FilesProcessorServiceFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import com.github.kassak.indexer.utils.IService;
import com.github.kassak.indexer.utils.Services;
import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Indexer implements IService {
    public Indexer(ITokenizerFactory tf, int regQueueSize, int queueSize, int fileThreads, int fileQueueSize) {
        IndexManagerService im = new IndexManagerService(tf, new FilesProcessorServiceFactory(fileThreads, fileQueueSize)
                , new IndexProcessorFactory(), queueSize);
        indexManager = im;
        fsWatcher = new FSWatcherService(im, regQueueSize);
    }

    @Override
    public void startService() throws FailureException {
        Services.startServices(indexManager, fsWatcher);
    }

    @Override
    public void stopService() {
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

    public Future<Void> add(@NotNull String path) {
        return fsWatcher.registerRoot(FileSystems.getDefault().getPath(path));
    }

    public Future<Void> remove(@NotNull String path) {
        return fsWatcher.unregisterRoot(FileSystems.getDefault().getPath(path));
    }

    public @NotNull Collection<FileEntry> search(@NotNull String word) {
        return indexManager.search(word);
    }

    public @NotNull List<FileStatistics> getFiles() {
        return indexManager.getFiles();
    }

    private final FSWatcherService fsWatcher;
    private final IIndexManagerService indexManager;
}
