package com.github.kassak.indexer;

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

    List<FileEntry> filesByWord(String word);

    void processFile(String file);
    void removeFileFromIndex(String file);
    void addWordToIndex(String file, String word);

}
