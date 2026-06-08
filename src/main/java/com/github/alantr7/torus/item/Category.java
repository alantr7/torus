package com.github.alantr7.torus.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class Category {

    public final String namespacedId;

    public final String id;

    public final String name;

    public ItemStack display;

    public final List<TorusItem> items = new LinkedList<>();

    public static final Category GENERAL = new Category("torus:general", "general", "General", new ItemStack(Material.COMPASS));

    public static final Category RESOURCES = new Category("torus:resources", "resources", "Resources", new ItemStack(Material.STICK));

    public static final Category GENERATORS = new Category("torus:generators", "generators", "Generators", new ItemStack(Material.PAPER));

    public static final Category MACHINES = new Category("torus:machines", "machines", "Machines", new ItemStack(Material.PAPER));

    public static final Category NETWORK = new Category("torus:network", "network", "Network", new ItemStack(Material.REDSTONE));

    public static final Category COMPONENTS = new Category("torus:components", "components", "Components", new ItemStack(Material.STICK));

    public static final Category STORAGE = new Category("torus:storage", "storage", "Storage", new ItemStack(Material.CHEST));

    public static final Category TOOLS = new Category("torus:tools", "tools", "Tools", new ItemStack(Material.STICK));

    public Category(String namespacedId, String id, String name, ItemStack display) {
        this.namespacedId = namespacedId;
        this.id = id;
        this.name = name;
        this.display = display;
    }

}
