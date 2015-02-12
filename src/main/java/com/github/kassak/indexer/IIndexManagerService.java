package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IIndexManagerService extends IService {
    @NotNull
    public Collection<FileEntry> search(@NotNull String word);
    @NotNull
    public List<Map.Entry<String, Integer>> getFiles() ;
}