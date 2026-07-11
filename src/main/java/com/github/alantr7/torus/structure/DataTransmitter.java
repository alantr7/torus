package com.github.alantr7.torus.structure;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.security.SecureRandom;

public interface DataTransmitter {

    String getMAC();

    static String generateMAC() {
        return NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 8);
    }

    int onDataRequest(DataTransmitter requester, int input);

}
