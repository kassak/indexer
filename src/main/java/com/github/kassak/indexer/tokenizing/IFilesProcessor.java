package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.utils.IService;

import java.nio.file.Path;

public interface IFilesProcessor extends IService {
    public void processFile(Path f) throws InterruptedException;
}
