package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IIndexProcessor {
    void syncFile(long stamp, @NotNull Path file);
    void syncDirectory(long stamp, @NotNull Path file);
    void removeFile(@NotNull Path file);
    void removeDirectory(@NotNull Path file);

    void fileFinished(long stamp, @NotNull Path file, boolean b);
    void removeWords(@NotNull Path file);
    void addWord(@NotNull Path file, String word);

    @NotNull
    List<FileStatistics> getFiles();
    @NotNull
    Collection<FileEntry> search(String word);
}
