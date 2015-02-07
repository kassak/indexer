package com.github.kassak.indexer;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface IIndexManager extends Runnable {
    static public class FileEntry {
        public FileEntry(String path, boolean valid) {
            this.path = path;
            this.valid = valid;
        }

        public String getPath() {
            return path;
        }

        public boolean isValid() {
            return valid;
        }

        private final String path;
        private final boolean valid;
    }

    public List<FileEntry> filesByWord(String word);

    public void addWordToIndex(Path file, String word);
    public void removeFromIndex(Path file);
    public void submitFinishedProcessing(Path file, long stamp, boolean valid);

    public ITokenizer newTokenizer(Path file) throws FileNotFoundException;

    public void syncFile(Path file);
    public void syncDirectory(Path file);
    public void removeFile(Path file);
    public void removeDirectory(Path file);

    public Collection<String> getFiles() ;
}
