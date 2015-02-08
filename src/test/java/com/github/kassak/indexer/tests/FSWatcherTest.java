package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.fs.FSWatcher;
import com.github.kassak.indexer.fs.IFSProcessor;
import com.github.kassak.indexer.fs.IFSWatcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

class Collector implements IFSProcessor {

    Collector() {
        files = new ConcurrentSkipListSet<>();
        dirs = new ConcurrentSkipListSet<>();
    }

    @Override
    public void onFileRemoved(Path file) {
        files.remove(file.toString());
    }

    @Override
    public void onFileChanged(Path file) {
        files.add(file.toString());
    }

    @Override
    public void onDirectoryRemoved(Path file) {
        dirs.remove(file.toString());
        Iterator<String> it = files.iterator();
        while(it.hasNext()) {
            String s = it.next();
            if (s.startsWith(file.toString()))
                it.remove();
        }
    }

    @Override
    public void onDirectoryChanged(Path file) {
        dirs.add(file.toString());
        Assert.assertTrue(false);
    }

    public Set<String> files;
    public Set<String> dirs;
}

public class FSWatcherTest {
    static {
        final Logger topLogger = Logger.getLogger("");
        Handler h = new ConsoleHandler();
        h.setLevel(Level.FINEST);
        topLogger.setLevel(Level.FINEST);
        topLogger.addHandler(h);
    }

    void appendToFile(Path file, String s) throws IOException {
        try (Writer w = new FileWriter(file.toString(), true)) {
            w.write(s);
        }
    }

    @Test
    public void fileRegistering() throws IOException, InterruptedException {
        Collector c = new Collector();
        IFSWatcher watcher = new FSWatcher(c);
        watcher.startService();

        Path temp = Files.createTempFile("test-", ".txt").toAbsolutePath();
        temp.toFile().deleteOnExit();
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.registerRoot(temp);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.unregisterRoot(temp);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        appendToFile(temp, "blah");
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.registerRoot(temp);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        Files.delete(temp);
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.stopService();
        watcher.waitFinished(10, TimeUnit.SECONDS);
    }

    @Test
    public void directoryRegistering() throws IOException, InterruptedException {
        Collector c = new Collector();
        IFSWatcher watcher = new FSWatcher(c);
        watcher.startService();

        Path base1 = Files.createTempDirectory("test-");
        base1.toFile().deleteOnExit();

        Path base2 = Files.createTempDirectory("test-");
        base2.toFile().deleteOnExit();

        Path subdir11 = base1.resolve("sub1");
        Path subdir12 = base1.resolve("sub2");

        Path subdir2 = base2.resolve("sub");

        Path file1 = base1.resolve("file1.txt");
        Path file12 = subdir11.resolve("file2.txt");

        Path file2 = subdir2.resolve("file.txt");

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.registerRoot(base2);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.createDirectories(subdir2);
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.createFile(file2);
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();
        appendToFile(file2, "blah");
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        Files.delete(file2);
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.createFile(file2);
        Thread.sleep(1000);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.unregisterRoot(base2);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        appendToFile(file2, "blah");
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.delete(file2);
        Files.delete(subdir2);
        Thread.sleep(1000);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.stopService();
        watcher.waitFinished(10, TimeUnit.SECONDS);
    }

    @Test
    public void errors() throws IOException, InterruptedException {
        Collector c = new Collector();
        IFSWatcher watcher = new FSWatcher(c);
        watcher.startService();

        Path unexisting = Files.createTempDirectory("test-");
        Files.delete(unexisting);
        watcher.registerRoot(unexisting);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.stopService();
        watcher.waitFinished(10, TimeUnit.SECONDS);
    }
}
