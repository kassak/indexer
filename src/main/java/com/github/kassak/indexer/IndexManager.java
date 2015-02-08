package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.IndexProcessor;
import com.github.kassak.indexer.tokenizing.FilesProcessor;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;
import com.github.kassak.indexer.utils.Services;
import com.github.kassak.indexer.utils.ThreadService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class IndexManager extends ThreadService implements IIndexManager {

    public IndexManager(ITokenizerFactory tf, int queueSize, int fileThreads, int fileQueueSize) {
        tasks = new ArrayBlockingQueue<IndexManagerTask>(queueSize);
        filesProcessor = new FilesProcessor(this, fileThreads, fileQueueSize);
        indexProcessor = new IndexProcessor(this);
        tokenizerFactory = tf;
    }

    @Override
    public void startService() throws Exception {
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

    @Override
    public Collection<FileEntry> search(String word) {
        return indexProcessor.search(word);
    }

    @Override
    public void addWordToIndex(Path file, String word) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.ADD_WORD, file, word));
    }

    @Override
    public void removeFromIndex(Path file) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.REMOVE_WORDS, file, null));
    }

    @Override
    public void submitFinishedProcessing(Path file, long stamp, boolean valid) throws InterruptedException {
        tasks.put(new IndexManagerTask(valid ? IndexManagerTask.FILE_FINISHED_OK : IndexManagerTask.FILE_FINISHED_FAIL, file, stamp, null));
    }

    @Override
    public ITokenizer newTokenizer(Path file) throws FileNotFoundException {
        return tokenizerFactory.create(new FileReader(file.toFile()));
    }

    @Override
    public void syncFile(Path file) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_FILE, file, null));
    }

    @Override
    public void syncDirectory(Path file) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.SYNC_DIR, file, null));
    }

    @Override
    public void removeFile(Path file) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_FILE, file, null));
    }

    @Override
    public void removeDirectory(Path file) throws InterruptedException {
        tasks.put(new IndexManagerTask(IndexManagerTask.DEL_DIR, file, null));
    }

    @Override
    public Collection<String> getFiles() {
        return indexProcessor.getFiles();
    }

    @Override
    public void processFile(Path file) throws InterruptedException {
        filesProcessor.processFile(file);
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
            switch(task.task) {
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
                    indexProcessor.fileFinished(task.stamp, task.path, true);
                    break;
            }
        }
    }

    private final IndexProcessor indexProcessor;
    private final BlockingQueue<IndexManagerTask> tasks;
    private final ITokenizerFactory tokenizerFactory;
    private final IFilesProcessor filesProcessor;
}
