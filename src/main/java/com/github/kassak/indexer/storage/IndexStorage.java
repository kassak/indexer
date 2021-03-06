package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concurrent SWMR bidirectional mapping between files & words
 */
class IndexStorage {
    static class IndexedWordWrapper {
        public IndexedWordWrapper(@NotNull IndexedWord wrapped) {
            this.wrapped = wrapped;
            files = Collections.newSetFromMap(new ConcurrentHashMap<IndexedFileWrapper, Boolean>(1));
        }

        public final IndexedWord wrapped;
        public final Set<IndexedFileWrapper> files;
    }
    static class IndexedFileWrapper {
        public IndexedFileWrapper(@NotNull IndexedFile wrapped) {
            this.wrapped = wrapped;
            words = Collections.newSetFromMap(new ConcurrentHashMap<IndexedWordWrapper, Boolean>(1));
        }

        public final IndexedFile wrapped;
        public final Set<IndexedWordWrapper> words;
    }
    public void addWord(@NotNull Path file, @NotNull String word) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null) {
            if(log.isLoggable(Level.FINER))
                log.finer("Ignoring attempt to add word to removed file " + file);
            return;
        }
        IndexedWordWrapper iword = words.get(word);
        if(iword == null) {
            iword = new IndexedWordWrapper(new IndexedWord(word));
            words.put(iword.wrapped.word, iword);
        }
        iword.files.add(ifile);
        ifile.words.add(iword);
    }

    @NotNull
    public Collection<FileEntry> search(@NotNull String word) {
        IndexedWordWrapper iword = words.get(word);
        if(iword == null)
            return Collections.emptyList();
        List<FileEntry> res = new ArrayList<>();
        for(IndexedFileWrapper ifile : iword.files)
            res.add(new FileEntry(ifile.wrapped.path, ifile.wrapped.state == States.VALID));
        return res;
    }

    @Nullable
    public IndexedFile getFile(@NotNull Path file) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null)
            return null;
        return ifile.wrapped;
    }

    public IndexedFile getOrAddFile(@NotNull Path file, long stamp) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null) {
            ifile = new IndexedFileWrapper(new IndexedFile(file.toString(), stamp));
            files.put(ifile.wrapped.path, ifile);
        }
        return ifile.wrapped;
    }

    @NotNull
    public List<FileStatistics> getFileNames() {
        List<FileStatistics> res = new ArrayList<>(files.size());
        for(Map.Entry<String, IndexedFileWrapper> f : files.entrySet())
            res.add(new FileStatistics(f.getKey(), f.getValue().wrapped.state, f.getValue().words.size()));
        return res;
    }

    @NotNull
    public IndexStatistics getStats() {
        IndexStatistics res = new IndexStatistics();
        res.numFiles = 0;
        res.numValidFiles = 0;
        for(IndexedFileWrapper f : files.values()) {
            ++res.numFiles;
            if(f.wrapped.state == States.VALID)
                ++res.numValidFiles;
        }
        res.numWords = words.size();
        return res;
    }

    @NotNull
    public List<String> getWords() {
        return new ArrayList<>(words.keySet());
    }

    public void removeFile(@NotNull Path file) {
        if(log.isLoggable(Level.FINER))
            log.finer("Removing file " + file);
        removeWords(file);
        files.remove(file.toString());
    }

    public void removeDirectory(@NotNull Path file) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINER))
            log.finer("Removing directory " + sfile);
        removeFile(file);
        sfile += FileSystems.getDefault().getSeparator();
        for(String f : files.subMap(sfile, sfile + Character.MAX_VALUE).keySet()) {
            removeFile(FileSystems.getDefault().getPath(f));
        }
    }

    public void removeNonexistent(@NotNull Path file) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINER))
            log.finer("Removing nonexistent " + sfile);
        removeFile(file);
        sfile += FileSystems.getDefault().getSeparator();
        for(String f : files.subMap(sfile, sfile + Character.MAX_VALUE).keySet()) {
            Path p = FileSystems.getDefault().getPath(f);
            if(!Files.exists(p))
                removeFile(p);
        }
    }

    public void removeWords(@NotNull Path file) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null) {
            if(log.isLoggable(Level.FINER))
                log.finer("Ignoring attempt to remove word from removed file " + file);
            return;
        }
        Iterator<IndexedWordWrapper> it = ifile.words.iterator();
        while(it.hasNext()) {
            IndexedWordWrapper iword = it.next();
            it.remove();
            iword.files.remove(ifile);
            if(iword.files.isEmpty())
            words.remove(iword.wrapped.word);
        }
    }

    private final SortedMap<String, IndexedFileWrapper> files = new ConcurrentSkipListMap<>();
    private final Map<String, IndexedWordWrapper> words = new ConcurrentHashMap<>();
    private static final Logger log = Logger.getLogger(IndexStorage.class.getName());
}
