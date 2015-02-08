package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizer;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collection;

public interface IIndexManager extends Runnable {

    public Collection<FileEntry> search(String word);

    public void addWordToIndex(Path file, String word) throws InterruptedException;
    public void removeFromIndex(Path file) throws InterruptedException;
    public void submitFinishedProcessing(Path file, long stamp, boolean valid) throws InterruptedException;

    public ITokenizer newTokenizer(Path file) throws FileNotFoundException;

    public void syncFile(Path file) throws InterruptedException;
    public void syncDirectory(Path file) throws InterruptedException;
    public void removeFile(Path file) throws InterruptedException;
    public void removeDirectory(Path file) throws InterruptedException;

    public Collection<String> getFiles() ;
    public void processFile(Path file) throws InterruptedException;
}
