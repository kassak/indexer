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
    public void onFileRemoved(Path file) {

    }

    @Override
    public void onFileChanged(Path file) {
        indexManager.processFile(file.toAbsolutePath().toString());
    }

    @Override
    public void onDirectoryRemoved(Path file) {

    }

    @Override
    public void onDirectoryChanged(Path file) {

    }

    private IIndexManager indexManager;
    private static final Logger log = Logger.getLogger(FSProcessor.class.getName());
}
