package com.wardenfar.osm2map.terrainGeneration;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import java.io.FileInputStream;
import java.io.IOException;
import com.wardenfar.osm2map.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SchemList {

    List<SuperSchematic> schems = new ArrayList<>();
    private int maxSize;

    public SchemList() {

    }

    public void addAllFiles(File folder, boolean rotated) {
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                System.out.println("Load Tree Schematic : " + file.getPath());

                try {
                    SuperSchematic schem = new SuperSchematic(loadClipboard(file));
                    addSchem(schem);
                    if (rotated) {
                        for (int i = 1; i <= 3; i++) {
                            addSchem(schem.rotate(i * 90));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error when loading schem : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void addAllFilesRotateTag(File folder) {
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                System.out.println("Load Car Schematic : " + file.getPath());
                try {
                    SuperSchematic schem = new SuperSchematic(loadClipboard(file));
                    String[] filename = Util.getFileNameWithoutExtension(file).split("-r=");
                    if (filename.length == 2) {
                        int rotation = Integer.parseInt(filename[1]);
                        if (rotation % 90 == 0) {
                            System.out.println("Rotation : " + rotation);
                            schem = schem.rotate(rotation);
                        }
                    }
                    addSchem(schem);
                } catch (Exception e) {
                    System.err.println("Error when loading schem : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static Clipboard loadClipboard(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IOException("Unsupported schematic format: " + file);
        try (FileInputStream input = new FileInputStream(file);
             ClipboardReader reader = format.getReader(input)) {
            return reader.read();
        }
    }

    public void addSchem(SuperSchematic schem) {
        this.schems.add(schem);
        maxSize = Math.max(maxSize, Math.max(schem.getWidth(), schem.getLength()));
    }

    public int getBuffer() {
        return maxSize / 2 + 1;
    }
}
