package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.IndexProcessor;
import com.github.kassak.indexer.tokenizing.FilesProcessor;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;
import com.github.kassak.indexer.utils.Services;
import com.github.kassak.indexer.utils.ThreadService;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class IndexManager extends ThreadService implements IIndexManager {

    public IndexManager(@NotNull ITokenizerFactory tf, int queueSize, int fileThreads, int fileQueueSize) {
        tasks = new PriorityBlockingQueue<>();
        tasksSemaphore = new Semaphore(queueSize);
        wordsSemaphore = new Semaphore(queueSize);
        filesQueue = new ConcurrentLinkedDeque<>();
        filesProcessor = new FilesProcessor(this, fileThreads, fileQueueSize);
        indexProcessor = new IndexProcessor(this);
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
    }

    @Override
    public void removeFromIndex(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.REMOVE_WORDS, file, null));
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

    @Override
    public void submitFinishedProcessing(@NotNull Path file, long stamp, boolean valid) throws InterruptedException {
        tasks.put(new IndexManagerTask(valid ? IndexManagerTask.FILE_FINISHED_OK : IndexManagerTask.FILE_FINISHED_FAIL, file, stamp, null));
        tryProcessFiles();
    }

    @Override
    public @NotNull ITokenizer newTokenizer(@NotNull Path file) throws FileNotFoundException {
        return tokenizerFactory.create(new FileReader(file.toFile()));
    }

    @Override
    public void syncFile(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_FILE, file, null));
    }

    @Override
    public void syncDirectory(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_DIR, file, null));
    }

    @Override
    public void removeFile(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_FILE, file, null));
    }

    @Override
    public void removeDirectory(@NotNull Path file) throws InterruptedException {
        tasksSemaphore.acquire();
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_DIR, file, null));
    }

    @NotNull
    @Override
    public List<Map.Entry<String, Integer>> getFiles() {
        return indexProcessor.getFiles();
    }

    @Override
    public void processFile(@NotNull Path file){
        if(!filesProcessor.processFile(file)) {
            filesQueue.add(file);
        }
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            IndexManagerTask task;
            try {
                task = tasks.take();
            } catch (InterruptedException e) {
                break;
            }
            try {
                switch (task.task) {
                    case IndexManagerTask.DEL_DIR:
                        indexProcessor.removeDirectory(task.stamp, task.path);
                        break;
                    case IndexManagerTask.DEL_FILE:
                        indexProcessor.removeFile(task.stamp, task.path);
                        break;
                    case IndexManagerTask.SYNC_DIR:
                        indexProcessor.syncDirectory(task.stamp, task.path);
                        break;
                    case IndexManagerTask.SYNC_FILE:
                        indexProcessor.syncFile(task.stamp, task.path);
                        break;
                    case IndexManagerTask.ADD_WORD:
                        indexProcessor.addWord(task.stamp, task.path, task.word);
                        break;
                    case IndexManagerTask.REMOVE_WORDS:
                        indexProcessor.removeWords(task.stamp, task.path);
                        break;
                    case IndexManagerTask.FILE_FINISHED_OK:
                    case IndexManagerTask.FILE_FINISHED_FAIL:
                        indexProcessor.fileFinished(task.stamp, task.path, task.task == IndexManagerTask.FILE_FINISHED_OK);
                        break;
                }
            } finally {
                if(task.task != IndexManagerTask.FILE_FINISHED_FAIL && task.task != IndexManagerTask.FILE_FINISHED_OK) {
                    if(task.task == IndexManagerTask.ADD_WORD)
                        wordsSemaphore.release();
                    else
                        tasksSemaphore.release();
                }
            }
        }
    }

    private final IndexProcessor indexProcessor;
    private final BlockingQueue<IndexManagerTask> tasks;
    private final Semaphore tasksSemaphore;
    private final Semaphore wordsSemaphore;
    private final Deque<Path> filesQueue;
    private final ITokenizerFactory tokenizerFactory;
    private final IFilesProcessor filesProcessor;
}
