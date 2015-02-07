package com.github.kassak.indexer.storage;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexStorage {
    static class IndexedWordWrapper {
        public IndexedWordWrapper(IndexedWord wrapped) {
            this.wrapped = wrapped;
            files = Collections.newSetFromMap(new ConcurrentHashMap<IndexedFileWrapper, Boolean>());
        }

        public final IndexedWord wrapped;
        public final Set<IndexedFileWrapper> files;
    }
    static class IndexedFileWrapper {
        public IndexedFileWrapper(IndexedFile wrapped) {
            this.wrapped = wrapped;
            words = Collections.newSetFromMap(new ConcurrentHashMap<IndexedWordWrapper, Boolean>());
        }

        public final IndexedFile wrapped;
        public final Set<IndexedWordWrapper> words;
    }
    public void addWord(Path file, String word) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(file == null) {
            if(log.isLoggable(Level.FINE))
                log.fine("Ignoring attempt to add word to removed file " + file);
            return;
        }
        IndexedWordWrapper iword = words.get(word);
        if(iword == null) {
            iword = new IndexedWordWrapper(new IndexedWord(word));
            words.put(iword.wrapped.getWord(), iword);
        }
        iword.files.add(ifile);
        ifile.words.add(iword);
    }

    public Collection<FileEntry> search(String word) {
        IndexedWordWrapper iword = words.get(word);
        if(iword == null)
            return Collections.emptyList();
        List<FileEntry> res = new ArrayList<>();
        for(IndexedFileWrapper ifile : iword.files)
            res.add(new FileEntry(ifile.wrapped.getPath(), ifile.wrapped.getState() == IndexedFile.VALID));
        return res;
    }

    public IndexedFile getFile(Path file) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null)
            return null;
        return ifile.wrapped;
    }

    public IndexedFile getOrAddFile(Path file, long stamp) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null) {
            ifile = new IndexedFileWrapper(new IndexedFile(file.toString(), IndexedFile.INVALID, stamp));
            files.put(ifile.wrapped.getPath(), ifile);
        }
        return ifile.wrapped;
    }

    public Collection<String> getFileNames() {
        return files.keySet();
    }

    public void removeFile(Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("Removing file " + file);
        removeWords(file);
        files.remove(file.toString());
    }

    public void removeDirectory(Path file) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("Removing directory " + sfile);
        removeFile(file);
        sfile += FileSystems.getDefault().getSeparator();
        for(String f : files.subMap(sfile, sfile + Character.MAX_VALUE).keySet()) {
            removeFile(FileSystems.getDefault().getPath(f));
        }
    }

    public void removeNonexistent(Path file) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("Removing nonexistent " + sfile);
        removeFile(file);
        sfile += FileSystems.getDefault().getSeparator();
        for(String f : files.subMap(sfile, sfile + Character.MAX_VALUE).keySet()) {
            Path p = FileSystems.getDefault().getPath(f);
            if(!Files.exists(p))
                removeFile(p);
        }
    }

    public void removeWords(Path file) {
        IndexedFileWrapper ifile = files.get(file.toString());
        if(ifile == null) {
            if(log.isLoggable(Level.FINE))
                log.fine("Ignoring attempt to remove word from removed file " + file);
            return;
        }
        Iterator<IndexedWordWrapper> it = ifile.words.iterator();
        while(it.hasNext()) {
            IndexedWordWrapper iword = it.next();
            it.remove();
            iword.files.remove(ifile);
            if(iword.files.isEmpty())
            words.remove(iword.wrapped.getWord());
        }
    }

    private final SortedMap<String, IndexedFileWrapper> files = new ConcurrentSkipListMap<>();
    private final Map<String, IndexedWordWrapper> words = new ConcurrentHashMap<>();
    private static final Logger log = Logger.getLogger(IndexStorage.class.getName());
}
