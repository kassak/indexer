package com.github.kassak.indexer.tests;

import com.github.kassak.indexer.fs.FSEventsService;
import com.github.kassak.indexer.fs.IFSEventsProcessor;
import com.github.kassak.indexer.tests.util.IndexerTesting;
import com.github.kassak.indexer.utils.IService;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

class Collector implements IFSEventsProcessor {

    Collector() {
        files = new ConcurrentSkipListSet<>();
        dirs = new ConcurrentSkipListSet<>();
    }

    @Override
    public void onFileRemoved(@NotNull Path file) {
        files.remove(file.toString());
    }

    @Override
    public void onFileChanged(@NotNull Path file) {
        files.add(file.toString());
    }

    @Override
    public void onDirectoryRemoved(@NotNull Path file) {
        dirs.remove(file.toString());
        Iterator<String> it = files.iterator();
        while(it.hasNext()) {
            String s = it.next();
            if (s.startsWith(file.toString()))
                it.remove();
        }
    }

    @Override
    public void onDirectoryChanged(@NotNull Path file) {
        dirs.add(file.toString());
        Assert.assertTrue(false);
    }

    public final Set<String> files;
    public final Set<String> dirs;
}

public class FSWatcherTest {
    static {
        final Logger topLogger = Logger.getLogger("");
        Handler h = new ConsoleHandler();
        h.setLevel(Level.FINEST);
        topLogger.setLevel(Level.FINEST);
        topLogger.addHandler(h);
    }

    private void appendToFile(Path file, String s) throws IOException {
        try (Writer w = new FileWriter(file.toString(), true)) {
            w.write(s);
        }
    }

    private static Path newFile(Path p) throws IOException {
        Files.createFile(p);
        p.toFile().deleteOnExit();
        return p;
    }

    private static Path newDir(Path p) throws IOException {
        Files.createDirectory(p);
        p.toFile().deleteOnExit();
        return p;
    }

    private static Path addFile(Path dir, String name) throws IOException {
        Path res = dir.resolve(name);
        return newFile(res);
    }

    private static Path addDir(Path dir, String name) throws IOException {
        Path res = dir.resolve(name);
        return newDir(res);
    }

    private static Path tempDir() throws IOException {
        Path res = Files.createTempDirectory("test-");
        res.toFile().deleteOnExit();
        return res;
    }

    private Collector c;
    private FSEventsService watcher;

    @Before
    public void init() throws IService.FailureException {
        c = new Collector();
        watcher = new FSEventsService(c);
        watcher.startService();
    }

    @After
    public void deinit() throws InterruptedException {
        try {
            watcher.stopService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        watcher.waitFinished(10, TimeUnit.SECONDS);
    }

    @Test
    public void fileRegistering() throws IOException, InterruptedException {
        Path root = tempDir();

        Path temp = addFile(root, "blah0.txt");
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.registerRoot(temp);

        IndexerTesting.waitIdle(watcher);
        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();

        appendToFile(temp, "blah");

        IndexerTesting.waitIdle(watcher);
        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();
    }

    @Test
    public void fileSiblingFile() throws IOException, InterruptedException {
        Path root = tempDir();
        Path temp = addFile(root, "blah0.txt");

        watcher.registerRoot(temp);
        Path file2 = addFile(root, "blah1.txt");

        IndexerTesting.waitIdle(watcher);
        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());
    }

    @Test
    public void fileSiblingDir() throws IOException, InterruptedException {
        Path root = tempDir();
        Path temp = addFile(root, "blah0.txt");

        watcher.registerRoot(temp);
        Path tmp_dir = addDir(root, "test_dir");
        Path tmp_file = addFile(tmp_dir, "blah.txt");

        IndexerTesting.waitIdle(watcher);
        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(temp.toString()));
        Assert.assertTrue(c.dirs.isEmpty());
    }

    @Test
    public void fileUnregisration() throws IOException, InterruptedException {
        Path root = tempDir();
        Path temp = addFile(root, "blah0.txt");

        watcher.registerRoot(temp);
        IndexerTesting.waitIdle(watcher);
        watcher.unregisterRoot(temp);

        IndexerTesting.waitIdle(watcher);
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        appendToFile(temp, "blah");

        IndexerTesting.waitIdle(watcher);
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());
    }

    @Test
    public void fileDeletion() throws IOException, InterruptedException {
        Path root = tempDir();
        Path temp = addFile(root, "blah0.txt");

        watcher.registerRoot(temp);
        IndexerTesting.waitIdle(watcher);
        Files.delete(temp);

        IndexerTesting.waitIdle(watcher);
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        newFile(temp);

        IndexerTesting.waitIdle(watcher);
        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());
    }

    @Test
    public void directoryRegistering() throws IOException, InterruptedException {
        Path base1 = tempDir();
        Path base2 = tempDir();

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

        newDir(subdir2);
        IndexerTesting.waitIdle(watcher);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        newFile(file2);
        IndexerTesting.waitIdle(watcher);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        c.files.clear();
        appendToFile(file2, "blah");
        IndexerTesting.waitIdle(watcher);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        Files.delete(file2);
        IndexerTesting.waitIdle(watcher);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.createFile(file2);
        IndexerTesting.waitIdle(watcher);

        Assert.assertEquals(c.files.size(), 1);
        Assert.assertTrue(c.files.contains(file2.toString()));
        Assert.assertTrue(c.dirs.isEmpty());

        watcher.unregisterRoot(base2);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        appendToFile(file2, "blah");
        IndexerTesting.waitIdle(watcher);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());

        Files.delete(file2);
        Files.delete(subdir2);
        IndexerTesting.waitIdle(watcher);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());
    }

    @Test
    public void errors() throws IOException, InterruptedException {
        Path unexisting = tempDir();
        Files.delete(unexisting);
        watcher.registerRoot(unexisting);

        Assert.assertTrue(c.files.isEmpty());
        Assert.assertTrue(c.dirs.isEmpty());
    }
}
