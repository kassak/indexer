package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
    File system watcher interface
*/
public interface IFSWatcherService extends IService {
    /**
        Register file or folder and all subfolders

        @param path path to be registered
        @throws IOException in case of problems
    */
    public void registerRoot(@NotNull Path path) throws IOException;
    /**
        Unregister previously registered path

        @param path path to be unregistered
        @throws IOException in case of problems
    */
    public void unregisterRoot(@NotNull Path path) throws IOException;
}
