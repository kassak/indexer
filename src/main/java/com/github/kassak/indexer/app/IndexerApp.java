package com.github.kassak.indexer.app;

import com.github.kassak.indexer.Indexer;
import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.factories.AlphanumTokenizerFactory;
import com.github.kassak.indexer.tokenizing.factories.ITokenizerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexerApp {
    private static void help() {
        System.out.println("?\t--\thelp");
        System.out.println("a\t--\tadd file");
        System.out.println("r\t--\tremove file");
        System.out.println("s\t--\tsearch");
        System.out.println("f\t--\tfiles");
        System.out.println("l\t--\ttoggle console logging");
        System.out.println("q\t--\tquit");
    }

    private static void append(Scanner ins, Indexer indexer) {
        System.out.print("path > ");
        System.out.flush();
        try {
            indexer.add(ins.nextLine().trim());
            System.out.println("added");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void remove(Scanner ins, Indexer indexer) {
        System.out.print("path > ");
        System.out.flush();
        try {
            indexer.remove(ins.nextLine().trim());
            System.out.println("removed");
        } catch (Exception e) {
            e.printStackTrace();
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
            handler.setLevel(Level.ALL);
        } else if(handler.getLevel() == Level.SEVERE) {
            handler.setLevel(Level.WARNING);
        } else {
            handler.setLevel(Level.SEVERE);
        }
        System.out.println("Level: " + handler.getLevel());
    }

    private static void listFiles(Indexer indexer) {
        List<Map.Entry<String, Integer>> res = indexer.getFiles();
        Collections.sort(res, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(@NotNull Map.Entry<String, Integer> o1, @NotNull Map.Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        System.out.println("--------begin---------");
        long cnt = 0;
        for(Map.Entry<String, Integer> s : res) {
            System.out.println(s.getKey() + " : " + s.getValue());
            if(s.getValue() != 0)
                ++cnt;
        }
        System.out.println("---------end----------");
        System.out.println("Files: " + res.size() + ", non empty: " + cnt);
    }

    public static void main(String[] argv) {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.OFF);
        {
            Logger topLogger = Logger.getLogger("");
            topLogger.setLevel(Level.FINEST);
            topLogger.addHandler(handler);
        }


        ITokenizerFactory tf = new AlphanumTokenizerFactory();
        Indexer indexer = new Indexer(tf, 10, 100, 10, 100);
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
                } else if(cmd.equals("q")) {
                    break;
                } else if(cmd.equals("f")) {
                    listFiles(indexer);
                } else if(cmd.equals("l")) {
                    toggleLog(handler);
                } else {
                    System.out.println("Unknown command. Try `?`");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                indexer.stopService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}