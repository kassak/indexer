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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class IndexManagerService extends ThreadService
        implements IIndexManagerService, IFileProcessingResults, IFilesProcessor, IFSEventsProcessor {

    public IndexManagerService(@NotNull ITokenizerFactory tf, @NotNull IFilesProcessorServiceFactory fpf
            , @NotNull IIndexProcessorFactory ipf, int queueSize) {
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
    public void startService() throws Exception {
        //TODO
        filesProcessor.startService();
        super.startService();
    }

    @Override
    public void stopService() throws Exception {
        try {
            super.stopService();
        } finally {
            Services.stopServices(filesProcessor);
        }
    }

    @NotNull
    @Override
    public Collection<FileEntry> search(@NotNull String word) {
        return indexProcessor.search(word);
    }

    @Override
    public void addWordToIndex(@NotNull Path file, @NotNull String word) throws InterruptedException {
        wordsSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.ADD_WORD, file, word));
        notifyTasks();
    }

    @Override
    public void removeFromIndex(@NotNull Path file) throws InterruptedException {
        wordsSemaphore.acquire();
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
        tasks.put(new IndexManagerTask(valid ? IndexManagerTask.FILE_FINISHED_OK : IndexManagerTask.FILE_FINISHED_FAIL, file, stamp, null));
        tryProcessFiles();
        notifyTasks();
    }

    @Override
    public @NotNull ITokenizer newTokenizer(@NotNull Path file) throws FileNotFoundException {
        return tokenizerFactory.create(new FileReader(file.toFile()));
    }

    @Override
    public void onFileChanged(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_FILE, file, null));
    }

    @Override
    public void onDirectoryChanged(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_DIR, file, null));
    }

    @Override
    public void onFileRemoved(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_FILE, file, null));
    }

    @Override
    public void onDirectoryRemoved(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_DIR, file, null));
    }

    @NotNull
    @Override
    public List<FileStatistics> getFiles() {
        return indexProcessor.getFiles();
    }

    @Override
    public boolean processFile(@NotNull Path file){
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
    public void run() {
        while(!Thread.interrupted()) {
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
}
