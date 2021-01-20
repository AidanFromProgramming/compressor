package com.company;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;


public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        compressor compress = new compressor();
        deCompressor decompress = new deCompressor();


        byte[] fileContent = Files.readAllBytes(Paths.get("C:\\Users\\aidan\\IdeaProjects\\Compression_program\\Text.txt"));

        //byte[] fileContent = Files.readAllBytes(Paths.get("A:\\dawnloeds\\DiscordSetup.exe"));

        compress.Compress(fileContent);

        TimeUnit.SECONDS.sleep(1);

        decompress.deCompress("C:\\Users\\aidan\\IdeaProjects\\Compression_program\\CompressedStream.crunch",
                "C:\\Users\\aidan\\IdeaProjects\\Compression_program\\file.manifest");
    }
}
