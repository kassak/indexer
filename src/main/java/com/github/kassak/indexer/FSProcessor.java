package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FSProcessor implements IFSProcessor {
    public FSProcessor(IIndexManager im) {
        this.indexManager = im;
    }

    @Override
    public void onFileRemoved(Path file) throws InterruptedException {
        indexManager.removeFile(file);
    }

    @Override
    public void onFileChanged(Path file) throws InterruptedException {
        indexManager.syncFile(file);
    }

    @Override
    public void onDirectoryRemoved(Path file) throws InterruptedException {
        indexManager.removeDirectory(file);
    }

    @Override
    public void onDirectoryChanged(Path file) throws InterruptedException {
        indexManager.syncDirectory(file);
    }

    private IIndexManager indexManager;
    private static final Logger log = Logger.getLogger(FSProcessor.class.getName());
}
