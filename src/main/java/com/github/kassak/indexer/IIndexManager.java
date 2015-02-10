package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IIndexManager extends IService {

    @NotNull
    public Collection<FileEntry> search(@NotNull String word);

    public void addWordToIndex(@NotNull Path file, @NotNull String word) throws InterruptedException;
    public void removeFromIndex(@NotNull Path file) throws InterruptedException;
    public void submitFinishedProcessing(@NotNull Path file, long stamp, boolean valid) throws InterruptedException;

    public @NotNull ITokenizer newTokenizer(@NotNull Path file) throws FileNotFoundException;

    public void syncFile(@NotNull Path file) throws InterruptedException;
    public void syncDirectory(@NotNull Path file) throws InterruptedException;
    public void removeFile(@NotNull Path file) throws InterruptedException;
    public void removeDirectory(@NotNull Path file) throws InterruptedException;

    @NotNull
    public List<Map.Entry<String, Integer>> getFiles() ;
    public void processFile(@NotNull Path file);
}
