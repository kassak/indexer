package com.github.kassak.indexer;

import java.nio.file.Path;

public interface IFSProcessor {
    void onFileRemoved(Path file);
    void onFileChanged(Path file);
    void onDirectoryRemoved(Path file);
    void onDirectoryChanged(Path file);
}
