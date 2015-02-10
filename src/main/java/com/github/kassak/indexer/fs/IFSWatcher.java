package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public interface IFSWatcher extends IService {
    public void registerRoot(@NotNull Path path) throws IOException;
    public void unregisterRoot(@NotNull Path path) throws IOException;
}
