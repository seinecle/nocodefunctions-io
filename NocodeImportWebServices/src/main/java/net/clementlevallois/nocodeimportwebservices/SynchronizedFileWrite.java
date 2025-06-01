/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.nocodeimportwebservices;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author LEVALLOIS
 */
public class SynchronizedFileWrite {

    public static synchronized void concurrentWriting(Path path, String string) {
        File file = path.toFile();
        if (!Files.exists(path) || string == null){
            System.out.println("file doesn't exist or string is empty in concurrentWriting method of IO");
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw"); FileChannel fileChannel = raf.getChannel()) {
            try (FileLock lock = fileChannel.lock()) {
                byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                raf.seek(raf.length());
                raf.write(bytes);
                // The lock is released when try-with-resources block exits
            }
        } catch (IOException e) {
            System.out.println("error in the concurrent write to file in one import API endpoint");
        }

    }

}
