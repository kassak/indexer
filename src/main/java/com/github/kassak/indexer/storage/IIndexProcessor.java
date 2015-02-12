package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IIndexProcessor {
    void syncFile(long stamp, @NotNull Path file);
    void syncDirectory(long stamp, @NotNull Path file);
    void removeFile(long stamp, @NotNull Path file);
    void removeDirectory(long stamp, @NotNull Path file);

    void fileFinished(long stamp, @NotNull Path file, boolean b);
    void removeWords(@NotNull Path file);
    void addWord(@NotNull Path file, String word);

    @NotNull
    List<Map.Entry<String, Integer>> getFiles();
    @NotNull
    Collection<FileEntry> search(String word);
}
