package com.github.kassak.indexer;

import java.util.concurrent.atomic.AtomicInteger;

public class FileData {
    static final int VALID = 0;
    static final int INVALID = 0;

    volatile int state = INVALID;
}
