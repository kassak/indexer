package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 12.02.2015.
 */
public interface IIndexProcessor {
    void syncFile(long stamp, @NotNull Path file);

    void syncDirectory(long stamp, @NotNull Path file);

    void removeFile(long stamp, @NotNull Path file);

    void removeDirectory(long stamp, @NotNull Path file);

    void fileFinished(long stamp, @NotNull Path file, boolean b);

    void removeWords(long stamp, @NotNull Path file);

    void addWord(long stamp, @NotNull Path file, String word);

    @NotNull
    List<Map.Entry<String, Integer>> getFiles();

    @NotNull
    Collection<FileEntry> search(String word);
}
