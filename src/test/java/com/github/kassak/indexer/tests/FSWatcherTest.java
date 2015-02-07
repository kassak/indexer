package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.FSWatcher;
import com.github.kassak.indexer.IFSProcessor;
import com.github.kassak.indexer.IFSWatcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

class Collector implements IFSProcessor {

    Collector() {
        files = new ConcurrentSkipListSet<>();
    }

    @Override
    public void processFile(Path file) {
        files.add(file.toAbsolutePath().toString());
    }

    public Set<String> files;
}

public class FSWatcherTest {
    void appendToFile(Path file, String s) throws IOException {
        try (Writer w = new FileWriter(file.toString(), true)) {
            w.write(s);
        }
    }

    @Test
    public void fileRegistering() throws IOException, InterruptedException {
        Collector c = new Collector();
        IFSWatcher watcher = new FSWatcher(c);
        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        Path temp = Files.createTempFile("test-", ".txt");
        temp.toFile().deleteOnExit();
        Assert.assertTrue(c.files.isEmpty());

        watcher.registerRoot(temp);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertEquals(c.files.iterator().next(), temp.toAbsolutePath().toString());

        c.files.clear();

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertEquals(c.files.iterator().next(), temp.toAbsolutePath().toString());

        c.files.clear();

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertEquals(c.files.iterator().next(), temp.toAbsolutePath().toString());

        c.files.clear();

        watcher.unregisterRoot(temp);

        Assert.assertTrue(c.files.isEmpty());

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());

        watcher.registerRoot(temp);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertEquals(c.files.iterator().next(), temp.toAbsolutePath().toString());

        c.files.clear();

        Files.delete(temp);

        Assert.assertEquals(c.files.size(), 1);

        watcherThread.interrupt();
        watcherThread.join();
    }
}
