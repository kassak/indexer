package com.github.kassak.indexer;

import com.github.kassak.indexer.storage.FileEntry;
import com.github.kassak.indexer.tokenizing.ITokenizerFactory;
import com.github.kassak.indexer.tokenizing.WhitespaceTokenizerFactory;

import java.util.Collection;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexerApp {
    public static void main(String[] argv) {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.OFF);
        {
            Logger topLogger = Logger.getLogger("");
            topLogger.setLevel(Level.FINEST);
            topLogger.addHandler(handler);
        }


        ITokenizerFactory tf = new WhitespaceTokenizerFactory();
        try(
            Indexer indexer = new Indexer(tf, 100, 10, 100);
            Scanner ins = new Scanner(System.in)
        ) {
            while(true) {
                System.out.print("`?` for help > ");
                System.out.flush();
                String cmd = ins.nextLine();
                if(cmd == null)
                    break;
                cmd = cmd.trim();
                if(cmd.equals("?")) {
                    System.out.println(
                        "?\t--\thelp\n" +
                        "a\t--\tadd file\n" +
                        "r\t--\tremove file\n" +
                        "s\t--\tsearch\n" +
                        "l\t--\ttoggle console logging\n" +
                        "q\t--\tquit\n"
                    );
                } else if(cmd.equals("a")) {
                    System.out.print("path > ");
                    System.out.flush();
                    try {
                        indexer.add(ins.nextLine().trim());
                        System.out.println("added");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(cmd.equals("r")) {
                    System.out.print("path > ");
                    System.out.flush();
                    try {
                        indexer.remove(ins.nextLine().trim());
                        System.out.println("removed");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(cmd.equals("s")) {
                    System.out.print("word > ");
                    System.out.flush();
                    try {
                        Collection<FileEntry> res = indexer.search(ins.nextLine().trim());
                        System.out.println("--------begin---------");
                        for(FileEntry fe : res) {
                            System.out.print(fe.isValid() ? " " : "*");
                            System.out.println(fe.getPath());
                        }
                        System.out.println("---------end----------");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(cmd.equals("q")) {
                    break;
                } else if(cmd.equals("l")) {
                    handler.setLevel(handler.getLevel() == Level.ALL ? Level.OFF : Level.ALL);
                }
                else {
                    System.out.println("Unknown command. Try `?`");
                }

            }
        } catch (Indexer.IndexerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}