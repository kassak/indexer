package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.IndexProcessor;
import com.github.kassak.indexer.tokenizing.FilesProcessor;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class IndexManager implements IIndexManager {
    private static class IndexTask {
        static public final int SYNC_FILE = 0;
        static public final int SYNC_DIR = 1;
        static public final int DEL_FILE = 2;
        static public final int DEL_DIR = 3;
        static public final int ADD_WORD = 4;
        static public final int REMOVE_WORDS = 5;
        static public final int FILE_FINISHED_OK = 6;
        static public final int FILE_FINISHED_FAIL = 7;

        public IndexTask(int task, Path path, String word) {
            this.task = task;
            this.path = path;
            this.word = word;
            stamp = System.currentTimeMillis();
        }

        public IndexTask(int task, Path path, long stamp, String word) {
            this.task = task;
            this.path = path;
            this.word = word;
            this.stamp = stamp;
        }

        public final int task;
        public final Path path;
        public final String word;
        public final long stamp;
    }

    public IndexManager(ITokenizerFactory tf, int queueSize, int fileThreads, int fileQueueSize) {
        tasks = new ArrayBlockingQueue<IndexTask>(queueSize);
        filesProcessor = new FilesProcessor(this, fileThreads, fileQueueSize);
        processor = new IndexProcessor(this);
        tokenizerFactory = tf;
    }

    @Override
    public Collection<FileEntry> search(String word) {
        return processor.search(word);
    }

    @Override
    public void addWordToIndex(Path file, String word) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.ADD_WORD, file, word));
    }

    @Override
    public void removeFromIndex(Path file) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.REMOVE_WORDS, file, null));
    }

    @Override
    public void submitFinishedProcessing(Path file, long stamp, boolean valid) throws InterruptedException {
        tasks.put(new IndexTask(valid ? IndexTask.FILE_FINISHED_OK : IndexTask.FILE_FINISHED_FAIL, file, stamp, null));
    }

    @Override
    public ITokenizer newTokenizer(Path file) throws FileNotFoundException {
        return tokenizerFactory.create(new FileReader(file.toFile()));
    }

    @Override
    public void syncFile(Path file) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.SYNC_FILE, file, null));
    }

    @Override
    public void syncDirectory(Path file) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.SYNC_DIR, file, null));
    }

    @Override
    public void removeFile(Path file) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.DEL_FILE, file, null));
    }

    @Override
    public void removeDirectory(Path file) throws InterruptedException {
        tasks.put(new IndexTask(IndexTask.DEL_DIR, file, null));
    }

    @Override
    public Collection<String> getFiles() {
        return processor.getFiles();
    }

    @Override
    public void processFile(Path file) throws InterruptedException {
        filesProcessor.processFile(file);
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            IndexTask task;
            try {
                task = tasks.take();
            } catch (InterruptedException e) {
                break;
            }
            switch(task.task) {
                case IndexTask.DEL_DIR:
                    processor.removeDirectory(task.stamp, task.path);
                    break;
                case IndexTask.DEL_FILE:
                    processor.removeFile(task.stamp, task.path);
                    break;
                case IndexTask.SYNC_DIR:
                    processor.syncDirectory(task.stamp, task.path);
                    break;
                case IndexTask.SYNC_FILE:
                    processor.syncFile(task.stamp, task.path);
                    break;
                case IndexTask.ADD_WORD:
                    processor.addWord(task.stamp, task.path, task.word);
                    break;
                case IndexTask.REMOVE_WORDS:
                    processor.removeWords(task.stamp, task.path);
                    break;
                case IndexTask.FILE_FINISHED_OK:
                case IndexTask.FILE_FINISHED_FAIL:
                    processor.fileFinished(task.stamp, task.path, true);
                    break;
            }
        }
        filesProcessor.shutdown();
    }

    private final IndexProcessor processor;
    private final BlockingQueue<IndexTask> tasks;
    private final ITokenizerFactory tokenizerFactory;
    private final FilesProcessor filesProcessor;
}
