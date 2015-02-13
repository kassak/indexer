package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
    Interface for index processing.
    Needed for debug purposes.
*/
public interface IIndexProcessor {
    /**
        Update file index

        @param stamp event time
        @param file file to update
    */
    void syncFile(long stamp, @NotNull Path file);

    /**
        Update directory index

        @param stamp event time
        @param file directory to update
    */
    void syncDirectory(long stamp, @NotNull Path file);

    /**
        Remove file from index

        @param file file to remove
    */
    void removeFile(@NotNull Path file);

    /**
        Remove directory from index

        @param file directory to remove
    */
    void removeDirectory(@NotNull Path file);

    /**
        Process file finished event

        @param stamp time when file started processing
        @param file file that was processed
        @param b true if processing was ok
    */
    void fileFinished(long stamp, @NotNull Path file, boolean b);

    /**
        Remove all words, associated with file

        @param file file
    */
    void removeWords(@NotNull Path file);

    /**
        Add word associated to file

        @param file file
        @param word word
    */
    void addWord(@NotNull Path file, @NotNull String word);

    /**
        Get all files from index

        @return file states
    */
    @NotNull
    List<FileStatistics> getFiles();

    /**
        Search files with word

        @param word word
        @return list of files ant their validness
    */
    @NotNull
    Collection<FileEntry> search(@NotNull String word);
}
