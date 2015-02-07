package com.github.kassak.indexer;

import java.util.Scanner;

public class IndexerApp {
    public static void main(String[] argv) {
        ITokenizerFactory tf = new WhitespaceTokenizerFactory();
        IVocabularyFactory vf = new CHMVocabularyFactory();
        try(
            Indexer indexer = new Indexer(tf, vf, 100, 10, 100);
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
                        "s\t--\tsearch"
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
                        indexer.search(ins.nextLine().trim());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (Indexer.IndexerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}