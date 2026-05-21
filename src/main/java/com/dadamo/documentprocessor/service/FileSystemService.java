package com.dadamo.documentprocessor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileSystemService {

    private final Path outputDir;

    public FileSystemService(Path outputDir) {
        this.outputDir = outputDir;
    }

    public byte[] read(String storageRef) throws IOException {
        return Files.readAllBytes(Paths.get(storageRef));
    }

    public Path writeZip(String documentId, byte[] zipBytes) throws IOException {
        Files.createDirectories(outputDir);
        var target = outputDir.resolve(documentId + ".zip");
        Files.write(target, zipBytes);
        return target;
    }
}
