package com.github.kassak.indexer.fs;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;

class WatcherRegistrationTask {
    public WatcherRegistrationTask(@NotNull Path path, boolean register) {
        this.path = path;
        this.register = register;
    }

    public final Path path;
    public final boolean register;
}
