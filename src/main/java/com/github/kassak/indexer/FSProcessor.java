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
    public void processFile(Path file) {
        indexManager.processFile(file.toAbsolutePath().toString());
    }

    private IIndexManager indexManager;
    private static final Logger log = Logger.getLogger(FSProcessor.class.getName());
}
