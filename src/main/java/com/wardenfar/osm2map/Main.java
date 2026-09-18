package com.wardenfar.osm2map;

import com.google.gson.GsonBuilder;
import com.wardenfar.osm2map.config.Worlds;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Creates configuration without requiring a server or loading Bukkit classes. */
public class Main {
    public static void main(String[] args) throws Exception {
        Path path = Path.of(args.length == 0 ? "plugins/osm2map/config/config.json" : args[0]);
        if (Files.exists(path)) {
            System.out.println("Configuration already exists: " + path);
            return;
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (var writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(new Worlds(), writer);
        }
        System.out.println("Created configuration: " + path);
    }
}
