package com.github.kassak.indexer.app;

import com.github.kassak.indexer.Indexer;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.storage.FileStatistics;
import com.github.kassak.indexer.storage.IndexStatistics;
import com.github.kassak.indexer.storage.States;
import com.github.kassak.indexer.tokenizing.factories.AlphanumTokenizerFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.logging.*;
import java.util.*;
import java.lang.reflect.Constructor;

public class IndexerApp {
    private static class Config {
        public Config(String file) throws IOException {
            Properties prop = new Properties();
            InputStream input = new FileInputStream(file);
 
            prop.load(input);
            tokenizerFactoryClass = prop.getProperty("tokenizerFactoryClass", AlphanumTokenizerFactory.class.getName());
            parserThreadsNum = Integer.parseInt(prop.getProperty("parserThreadsNum", "3"));
            parserQueueSize = Integer.parseInt(prop.getProperty("parserQueueSize", "10"));
            registrationQueueSize = Integer.parseInt(prop.getProperty("registrationQueueSize", "10"));
            internalQueueSize = Integer.parseInt(prop.getProperty("internalQueueSize", "100"));
            logFile = prop.getProperty("logFile", null);
        }

        public final String logFile;
        public final String tokenizerFactoryClass;
        public final int parserThreadsNum;
        public final int parserQueueSize;
        public final int registrationQueueSize;
        public final int internalQueueSize;
    }

    private static Future<Void> lastOp;

    private static void help() {
        System.out.println("?\t--\thelp");
        System.out.println("a\t--\tadd file");
        System.out.println("r\t--\tremove file");
        System.out.println("s\t--\tsearch");
        System.out.println("f\t--\tfiles");
        System.out.println("l\t--\ttoggle console logging");
        System.out.println("c\t--\tcancel last {,un}registration if it running");
        System.out.println("i\t--\tprint index stats");
        System.out.println("w\t--\twords");
        System.out.println("q\t--\tquit");
    }

    private static void append(Scanner ins, Indexer indexer) throws InterruptedException {
        System.out.print("path > ");
        System.out.flush();
        lastOp = indexer.add(ins.nextLine().trim());
        System.out.println("adding..");
    }

    private static void remove(Scanner ins, Indexer indexer) throws InterruptedException {
        System.out.print("path > ");
        System.out.flush();
        lastOp = indexer.remove(ins.nextLine().trim());
        System.out.println("removing..");
    }

    private static void cancel() {
        if(lastOp == null)
            System.out.println("no last op");
        else {
            lastOp.cancel(true);
            System.out.println("last op cancelling");
        }
    }

    private static void search(Scanner ins, Indexer indexer) {
        System.out.print("word > ");
        System.out.flush();
        try {
            List<FileEntry> res = new ArrayList<>(indexer.search(ins.nextLine().trim()));
            Collections.sort(res, new Comparator<FileEntry>() {
                @Override
                public int compare(@NotNull FileEntry o1, @NotNull FileEntry o2) {
                    return o1.getPath().compareTo(o2.getPath());
                }
            });
            System.out.println("--------begin---------");
            for(FileEntry fe : res) {
                System.out.print(fe.isValid() ? " " : "*");
                System.out.println(fe.getPath());
            }
            System.out.println("---------end----------");
            System.out.println("Files: " + res.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void toggleLog(Handler handler) {
        if(handler.getLevel() == Level.ALL) {
            handler.setLevel(Level.OFF);
        } else if(handler.getLevel() == Level.WARNING) {
            handler.setLevel(Level.FINE);
        } else if(handler.getLevel() == Level.FINE) {
            handler.setLevel(Level.FINER);
        } else if(handler.getLevel() == Level.FINER) {
            handler.setLevel(Level.ALL);
        } else if(handler.getLevel() == Level.SEVERE) {
            handler.setLevel(Level.WARNING);
        } else {
            handler.setLevel(Level.SEVERE);
        }
        System.out.println("Level: " + handler.getLevel());
    }

    private static void listFiles(Indexer indexer) {
        List<FileStatistics> res = indexer.getFiles();
        Collections.sort(res, new Comparator<FileStatistics>() {
            @Override
            public int compare(@NotNull FileStatistics o1, @NotNull FileStatistics o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        System.out.println("--------begin---------");
        long ne_cnt = 0;
        long inval_cnt = 0;
        long val_cnt = 0;
        long proc_cnt = 0;
        for(FileStatistics s : res) {
            String prefix = "";
            if(s.state == States.INVALID) {
                ++inval_cnt;
                prefix = " - ";
            } else if(s.state == States.PROCESSING) {
                ++proc_cnt;
                prefix = " * ";
            } else if(s.state == States.VALID) {
                ++val_cnt;
                prefix = " + ";
            }
            System.out.println(prefix + " " + s.name + " : " + s.wordsNum);
            if(s.wordsNum != 0)
                ++ne_cnt;
        }
        System.out.println("---------end----------");
        System.out.println("Files: " + res.size() + ", non empty: " + ne_cnt
                           + ", valid: " + val_cnt + ", processing: " + proc_cnt + ", invalid: " + inval_cnt);
    }

    private static void listWords(Indexer indexer) {
        List<String> res = indexer.getWords();
        Collections.sort(res);
        System.out.println("--------begin---------");
        for(String s : res) {
            System.out.println(s);
        }
        System.out.println("---------end----------");
        System.out.println("Words: " + res.size());
    }

    private static void listStats(Indexer indexer) {
        IndexStatistics s = indexer.getStats();
        System.out.println("Files: " + s.numFiles + ", Valid files: "
                + s.numValidFiles + ", Words: " + s.numWords);
    }

    public static void main(String[] argv) {
        if(argv.length != 1) {
            System.err.println("First argument should be config file path.");
            return;
        }
        final Config c;
        final ITokenizerFactory tf;
        try {
            c = new Config(argv[0]);
            Class<?> clazz = Class.forName(c.tokenizerFactoryClass);
            Constructor<?> ctor = clazz.getConstructor();
            tf = (ITokenizerFactory)ctor.newInstance();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Failed to configure.");
            return;
        }

        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.OFF);
        {
            Logger topLogger = Logger.getLogger("");
            topLogger.setLevel(Level.FINEST);
            topLogger.addHandler(handler);

            if(c.logFile != null) {
                try {
                    Handler fh = new FileHandler(c.logFile);
                    fh.setLevel(Level.FINER);
                    fh.setFormatter(new SimpleFormatter());
                    topLogger.addHandler(fh);
                } catch (IOException e) {
                    System.out.println("Log file creation failed:");
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Current configuration:");
        System.out.println("\tTokenizer factory: " + c.tokenizerFactoryClass);
        System.out.println("\tNumber of parser threads: " + c.parserThreadsNum);
        System.out.println("\tParser queue size: " + c.parserQueueSize);
        System.out.println("\tInternal queue size: " + c.internalQueueSize);
        System.out.println("\tRegistration queue size: " + c.registrationQueueSize);
        
        Indexer indexer = new Indexer(tf, c.registrationQueueSize, c.internalQueueSize, c.parserThreadsNum, c.parserQueueSize);
        try(Scanner ins = new Scanner(System.in)) {
            indexer.startService();
            while(true) {
                System.out.print("`?` for help > ");
                System.out.flush();
                String cmd = ins.nextLine();
                if(cmd == null)
                    break;
                cmd = cmd.trim();
                if(cmd.equals("?")) {
                    help();
                } else if(cmd.equals("a")) {
                    append(ins, indexer);
                } else if(cmd.equals("r")) {
                    remove(ins, indexer);
                } else if(cmd.equals("s")) {
                    search(ins, indexer);
                } else if(cmd.equals("f")) {
                    listFiles(indexer);
                } else if(cmd.equals("l")) {
                    toggleLog(handler);
                } else if(cmd.equals("c")) {
                    cancel();
                } else if(cmd.equals("i")) {
                    listStats(indexer);
                } else if(cmd.equals("w")) {
                    listWords(indexer);
                } else if(cmd.equals("q")) {
                    break;
                } else {
                    System.out.println("Unknown command. Try `?`");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            indexer.stopService();
        }

    }
}
