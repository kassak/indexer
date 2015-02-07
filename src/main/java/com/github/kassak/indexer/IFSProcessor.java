package com.github.kassak.indexer;

import java.nio.file.Path;

public interface IFSProcessor {
    void onFileRemoved(Path file) throws InterruptedException;
    void onFileChanged(Path file) throws InterruptedException;
    void onDirectoryRemoved(Path file) throws InterruptedException;
    void onDirectoryChanged(Path file) throws InterruptedException;
}
