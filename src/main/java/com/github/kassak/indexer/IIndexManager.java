package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.utils.IService;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IIndexManager extends IService {

    public Collection<FileEntry> search(String word);

    public void addWordToIndex(Path file, String word) throws InterruptedException;
    public void removeFromIndex(Path file) throws InterruptedException;
    public void submitFinishedProcessing(Path file, long stamp, boolean valid) throws InterruptedException;

    public ITokenizer newTokenizer(Path file) throws FileNotFoundException;

    public void syncFile(Path file) throws InterruptedException;
    public void syncDirectory(Path file) throws InterruptedException;
    public void removeFile(Path file) throws InterruptedException;
    public void removeDirectory(Path file) throws InterruptedException;

    public List<Map.Entry<String, Integer>> getFiles() ;
    public void processFile(Path file);
}
