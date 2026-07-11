package com.github.alantr7.torus.utils;

public class BitUtils {

    public static int read(int n, int rangeStart, int rangeEnd) {
        int length = rangeEnd - rangeStart;
        int rightBits = 32 - rangeEnd;

        int shifted = n >>> rightBits;
        int mask = (int) ((1L << length) - 1);

        return shifted & mask;
    }

}
