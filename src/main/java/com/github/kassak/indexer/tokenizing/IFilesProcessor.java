package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface IFilesProcessor extends IService {
    public boolean processFile(@NotNull Path f);
}
