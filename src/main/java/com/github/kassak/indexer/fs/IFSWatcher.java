package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.IService;

import java.io.IOException;
import java.nio.file.Path;

public interface IFSWatcher extends IService {
    public void registerRoot(Path path) throws IOException;
    public void unregisterRoot(Path path) throws IOException;
}
