package com.github.kassak.indexer;

import com.github.kassak.indexer.fs.IFSEventsProcessor;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.IIndexProcessor;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.factories.IIndexProcessorFactory;
import com.github.kassak.indexer.tokenizing.IFileProcessingResults;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import com.github.kassak.indexer.tokenizing.IFilesProcessorService;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.factories.IFilesProcessorServiceFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import com.github.kassak.indexer.utils.Services;
import com.github.kassak.indexer.utils.ThreadService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class IndexManagerService implements Runnable, IIndexManagerService
        , IFileProcessingResults, IFilesProcessor, IFSEventsProcessor {

    public IndexManagerService(@NotNull ITokenizerFactory tf, @NotNull IFilesProcessorServiceFactory fpf
            , @NotNull IIndexProcessorFactory ipf, int queueSize) {
        currentService = new ThreadService(this);
        tasks = new PriorityBlockingQueue<>();
        this.queueSize = queueSize;
        tasksSemaphore = new Semaphore(queueSize);
        wordsSemaphore = new Semaphore(queueSize);
        filesQueue = new ConcurrentLinkedDeque<>();
        filesProcessor = fpf.create(this);
        indexProcessor = ipf.create(this);
        tokenizerFactory = tf;
    }

    @Override
    public void startService() throws FailureException {
        Services.startServices(filesProcessor, currentService);
    }

    @Override
    public void stopService() {
        Services.stopServices(currentService, filesProcessor);
        tasks.clear();
        //wake up threads, putting to queue
        wordsSemaphore.release((int)queueSize);
        tasksSemaphore.release((int)queueSize);
    }

    @Override
    public boolean isRunning() {
        return Services.isServicesRunning(filesProcessor, currentService);
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return Services.waitServicesFinished(timeout, unit, filesProcessor, currentService);
    }

    @NotNull
    @Override
    public Collection<FileEntry> search(@NotNull String word) {
        return indexProcessor.search(word);
    }

    private boolean acquireSemaphoreAndRunning(@NotNull Semaphore sem) throws InterruptedException {
        if(!isRunning()) {
            return false;
        }
        sem.acquire();
        if(!isRunning()) {
            sem.release();
            return false;
        }
        return true;
    }

    @Override
    public void addWordToIndex(@NotNull Path file, @NotNull String word) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(wordsSemaphore)) {
            log.fine("Word received while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.ADD_WORD, file, word));
        notifyTasks();
    }

    @Override
    public void removeFromIndex(@NotNull Path file) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(wordsSemaphore)) {
            log.fine("Remove file while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.REMOVE_WORDS, file, null));
        notifyTasks();
    }

    private void tryProcessFiles() {
        while(!filesQueue.isEmpty()) {
            Path next = filesQueue.pollFirst();
            if(next != null)
                if(!filesProcessor.processFile(next)) {
                    filesQueue.addFirst(next);
                    break;
                }
        }
    }

    private void notifyTasks() {
        synchronized (tasks){
            tasks.notifyAll();
        }
    }

    @Override
    public void submitFinishedProcessing(@NotNull Path file, long stamp, boolean valid) throws InterruptedException {
        if(!isRunning()) {
            log.fine("Received result while not running. Sending it to black hole");
            return;
        }
        tasks.put(new IndexManagerTask(valid ? IndexManagerTask.FILE_FINISHED_OK : IndexManagerTask.FILE_FINISHED_FAIL, file, stamp, null));
        tryProcessFiles();
        notifyTasks();
    }

    @Override
    public @Nullable ITokenizer newTokenizer(@NotNull Path file) throws IOException {
        return tokenizerFactory.create(file);
    }

    @Override
    public void onFileChanged(@NotNull Path file) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(tasksSemaphore)) {
            log.fine("Change received while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_FILE, file, null));
    }

    @Override
    public void onDirectoryChanged(@NotNull Path file) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(tasksSemaphore)) {
            log.fine("Change received while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_DIR, file, null));
    }

    @Override
    public void onFileRemoved(@NotNull Path file) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(tasksSemaphore)) {
            log.fine("Remove received while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_FILE, file, null));
    }

    @Override
    public void onDirectoryRemoved(@NotNull Path file) throws InterruptedException {
        if(!acquireSemaphoreAndRunning(tasksSemaphore)) {
            log.fine("Remove received while not running. Annihilating");
            return;
        }
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_DIR, file, null));
    }

    @NotNull
    @Override
    public List<FileStatistics> getFiles() {
        return indexProcessor.getFiles();
    }

    @Override
    public boolean processFile(@NotNull Path file){
        if(!isRunning()) {
            log.fine("Received message while not running. Sending it to black hole");
            return false;
        }
        filesQueue.add(file);
        tryProcessFiles();
        return true;
    }

    private IndexManagerTask extractRightPriorityTask() throws InterruptedException {
        if (filesQueue.size() < queueSize)
            return tasks.take();
        while(true) {
            IndexManagerTask task = tasks.take();
            synchronized(tasks) {
                if(IndexManagerTask.isProcessingTask(task.task) || filesQueue.size() < queueSize) {
                    return task;
                }
                tasks.add(task);
                if(IndexManagerTask.isProcessingTask(tasks.peek().task) || filesQueue.size() < queueSize) {
                    return tasks.remove();
                }
                tasks.wait();
            }
        }
    }

    @Override
    @TestOnly
    public boolean isIdle() {
        return tasks.isEmpty();
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            IndexManagerTask task;
            try {
                task = extractRightPriorityTask();
            } catch (InterruptedException e) {
                break;
            }
            try {
                switch (task.task) {
                    case IndexManagerTask.DEL_DIR:
                        indexProcessor.removeDirectory(task.path);
                        break;
                    case IndexManagerTask.DEL_FILE:
                        indexProcessor.removeFile(task.path);
                        break;
                    case IndexManagerTask.SYNC_DIR:
                        indexProcessor.syncDirectory(task.stamp, task.path);
                        break;
                    case IndexManagerTask.SYNC_FILE:
                        indexProcessor.syncFile(task.stamp, task.path);
                        break;
                    case IndexManagerTask.ADD_WORD:
                        assert task.word != null;
                        indexProcessor.addWord(task.path, task.word);
                        break;
                    case IndexManagerTask.REMOVE_WORDS:
                        indexProcessor.removeWords(task.path);
                        break;
                    case IndexManagerTask.FILE_FINISHED_OK:
                    case IndexManagerTask.FILE_FINISHED_FAIL:
                        indexProcessor.fileFinished(task.stamp, task.path, task.task == IndexManagerTask.FILE_FINISHED_OK);
                        break;
                }
            } finally {
                if(task.task != IndexManagerTask.FILE_FINISHED_FAIL && task.task != IndexManagerTask.FILE_FINISHED_OK) {
                    if(task.task == IndexManagerTask.ADD_WORD || task.task == IndexManagerTask.REMOVE_WORDS)
                        wordsSemaphore.release();
                    else
                        tasksSemaphore.release();
                }
            }
        }
    }

    private final long queueSize;
    private final IIndexProcessor indexProcessor;
    private final BlockingQueue<IndexManagerTask> tasks;
    private final Semaphore tasksSemaphore;
    private final Semaphore wordsSemaphore;
    private final Deque<Path> filesQueue;
    private final ITokenizerFactory tokenizerFactory;
    private final IFilesProcessorService filesProcessor;
    private final ThreadService currentService;
    private static final Logger log = Logger.getLogger(IndexManagerService.class.getName());
}
