package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.IndexStatistics;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.List;

/**
    Interface for index manager
*/
public interface IIndexManagerService extends IService {
    @NotNull
    public Collection<FileEntry> search(@NotNull String word);
    @NotNull
    public List<FileStatistics> getFiles() ;
    @TestOnly
    public boolean isIdle();
    @NotNull
    public IndexStatistics getStats();
    @NotNull
    public List<String> getWords();
}
