package com.github.kassak.indexer.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface IFSWatcher extends Runnable {
    public void registerRoot(Path path) throws IOException;
    public void unregisterRoot(Path path) throws IOException;
}
