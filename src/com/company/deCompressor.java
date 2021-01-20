package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class deCompressor {

    public deCompressor(){

    }

    public void deCompress(String filePath, String treePath) throws IOException, ClassNotFoundException {

        File inputFile = new File(filePath);
        FileInputStream inputStream = new FileInputStream(inputFile);
        BitStream compressedStream = new BitStream(inputStream);

        File metaFile = new File(treePath);
        FileInputStream fis = new FileInputStream(metaFile);
        ObjectInputStream metaStream = new ObjectInputStream(fis);
        metaData manifest = (metaData) metaStream.readObject();

        tableNode tree = manifest.huffmanTree;

        int totalBytes = manifest.totalBytes;
        long totalBits = manifest.totalBits;
        int bufferSize = manifest.bufferSize;

        byte[] deCompressedArray = new byte[(int) totalBytes];
        boolean eof = false;
        boolean treeReturn = false;
        tableNode treeBranch = tree;
        int currentByte = 0;
        long currentBitNumber = 0;
        compressedStream.readBits(0);
        while(!eof){
            try {
                if(treeReturn){
                    treeBranch = tree;
                    treeReturn = false;
                    currentByte++;
                }

                boolean currentBit = compressedStream.readBit();
                currentBitNumber++;

                if(currentBitNumber > totalBits){
                    System.out.println("Final Bit: " + currentBitNumber + "/" + totalBits);
                    System.out.println("Total Byte: " + currentByte + "/" + totalBytes);
                    eof = true;
                }

                if(!currentBit){
                    treeBranch = treeBranch.leftNode;
                }else{
                    treeBranch = treeBranch.rightNode;
                }

                if(treeBranch.endNode){
                    try{
                        deCompressedArray[currentByte] = (treeBranch.value);
                        treeReturn = true;
                    }catch (ArrayIndexOutOfBoundsException e){
                        //System.out.println("Error Byte: " + currentByte + "/" + totalBytes);
                        treeReturn = true;
                    }
                }


            }catch (IOException e){
                eof = true;
            }
        }

        //System.out.println("Final Bit: " + currentBitNumber + "/" + totalBits + " | " + bufferSize);
        //System.out.println("Total Byte: " + currentByte + "/" + totalBytes);

        System.out.println(Arrays.toString(deCompressedArray));
        File file = new File("deCompressed.txt");
        OutputStream os = new FileOutputStream(file);
        os.write(deCompressedArray);
        os.close();

    }
}
