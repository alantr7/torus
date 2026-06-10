package com.github.alantr7.torus.structure.registry;

import java.util.Map;

public class RegisteredStructure {

    public final String namespacedId;

    public final int numericId;

    public int version;

    public final Map<Integer, byte[]> collisionsVersions;

    public RegisteredStructure(String namespacedId, int numericId, int version, Map<Integer, byte[]> collisionsVersions) {
        this.namespacedId = namespacedId;
        this.numericId = numericId;
        this.version = version;
        this.collisionsVersions = collisionsVersions;
    }
}
