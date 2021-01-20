package com.company;

import java.io.*;
import java.sql.Array;
import java.util.*;

class orderList implements Comparator<tableNode> {
    @Override
    public int compare(tableNode o1, tableNode o2) {
        return o2.frequency - o1.frequency;
    }
}

class fileEncoderThread extends Thread {

    private static boolean running = true;
    private long start;
    private long total;
    private tableNode finalNode;
    private byte[] file;

    public int ID;
    private BitStream compressionStream;


    public fileEncoderThread(long startByte, long endByte, int ID, byte[] fileSet, tableNode finalNode) throws FileNotFoundException {
        this.start = startByte;
        this.total = endByte;
        this.ID = ID;
        this.file = fileSet;
        this.finalNode = finalNode;
        OutputStream stream = new FileOutputStream("shard" + this.ID + ".crunch");
        compressionStream = new BitStream(stream);
    }

    public static boolean running() {
        return running;
    }

    public BitStream finalStream(){
        return compressionStream;
    }

    @Override
    public void run(){
        for(long n = start; n < total;n++){
            byte currentByte = file[(int) n];
            List<Boolean> streamArray = finalNode.mapValue(currentByte);
            for (Boolean direction: streamArray){
                if(!direction){
                    try {
                        compressionStream.writeBit(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        compressionStream.writeBit(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            compressionStream.flush();
            compressionStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }

}

class tableNode implements Serializable{

    boolean endNode = false;
    int frequency;
    tableNode leftNode;
    tableNode rightNode;
    byte value;

    public tableNode(){

    }

    public tableNode(tableNode left, tableNode right){
        leftNode = left;
        rightNode = right;
        frequency = left.frequency + right.frequency;
    }


    public tableNode(byte byteValue, int byteFrequency) {
        value = byteValue;
        frequency = byteFrequency;
        endNode = true;
    }

    public String toString(){
        if (endNode){
            return "[" + value + ":" + frequency + "]";
        }else {
            return frequency + "["  + leftNode.toString() + ":" + rightNode.toString() + "]";
        }
    }

    public List<Boolean> mapValue(byte targetValue){
        if (endNode && value == targetValue){
            return null;
        }else{
            List<Boolean> leftMap = new ArrayList<Boolean>();
            leftMap.add(false);
            List<Boolean> rightMap = new ArrayList<Boolean>();
            leftMap.add(true);

            List<Boolean> returnMapL = leftNode.mapValue(targetValue, leftMap);
            List<Boolean> returnMapR = rightNode.mapValue(targetValue, rightMap);

            if (returnMapL != null){
                return returnMapL;
            }else if(returnMapR != null){
                return returnMapR;
            }else{
                //System.out.println("Error: Unable to find value[" + targetValue + "] in tree");
                return null;
            }

        }
    }

    public List<Boolean> mapValue(byte targetValue, List<Boolean> currentMap){
        if (endNode && value == targetValue) {
            return currentMap;
        }else if(endNode){
            return null;
        }else{
            List<Boolean> leftMap = new ArrayList<Boolean>();
            List<Boolean> rightMap = new ArrayList<Boolean>();
            leftMap = currentMap;
            rightMap = currentMap;



            leftMap.add(false);
            rightMap.add(true);
            List<Boolean> returnMapL = leftNode.mapValue(targetValue, leftMap);
            List<Boolean> returnMapR = rightNode.mapValue(targetValue, rightMap);

            if (returnMapL != null){
                return returnMapL;
            }
            return returnMapR;
        }
    }
}

class metaData implements Serializable{

    tableNode huffmanTree;
    long totalBits;
    int totalBytes;
    int bufferSize;
    String fileName;

    public metaData(){

    }

    public metaData(String fileName,tableNode huffmanTree, long totalBits, int totalBytes, int bufferSize){
        this.fileName = fileName;
        this.huffmanTree = huffmanTree;
        this.totalBits = totalBits;
        this.totalBytes = totalBytes;
        this.bufferSize = bufferSize;
    }

}

public class compressor {

    int totalBitCounter;

    int currentByteNumber;

    public compressor(){
    }

    private int[] analyzeFile(byte[] unCompressedArray){
        Byte[] compressedArray;
        int[] valueTable = new int [256];
        for (Byte currentByte : unCompressedArray) {
            valueTable[currentByte.intValue()+128]++;
        }
        return valueTable;
    }

    private List<tableNode> createHuffmanList(int[] valueTable){
        System.out.println("Analysis complete... Creating Basic Huffman List");
        List<tableNode> huffmanTree = new ArrayList<tableNode>();
        for (int i = 0; i < 256; i++){
            if(valueTable[i] != 0){
                tableNode table = new tableNode((byte) (i - 128), valueTable[i]);
                huffmanTree.add(table);
            }
        }

        return huffmanTree;
    }

    private tableNode createHuffmanTree(byte[] unCompressedArray){

        System.out.println("Huffman List Competed... Converting To Huffman Tree");
        int[] valueTable = analyzeFile(unCompressedArray);
        List<tableNode> huffmanTree = createHuffmanList(valueTable);
        while (huffmanTree.size() != 1) {
            huffmanTree.sort(new orderList());

            tableNode valueL = huffmanTree.remove(huffmanTree.size() - 2);
            tableNode valueR = huffmanTree.remove(huffmanTree.size() - 1);
            System.out.println("Merging[" + huffmanTree.size() + "]: " + valueL + " | " + valueR);
            tableNode newNode = new tableNode(valueL, valueR);

            huffmanTree.add(newNode);
        }

        return huffmanTree.get(0);

    }

    private List<Boolean>[] createEncodingTable(tableNode huffmanTree){

        List<Boolean>[] encodingMap = new ArrayList[256];
        for (int i = 0; i < 256; i++){
            encodingMap[i] = new ArrayList<Boolean>();
            List<Boolean> streamArray;
            if(i > 32){
                streamArray = huffmanTree.mapValue((byte) (i - 128));
            }else {
                streamArray = huffmanTree.mapValue((byte) (i - 128));
            }
            if(streamArray == null) {
                encodingMap[i].add(null);
            }else {
                for (Boolean direction : streamArray) {
                    if (!direction) {
                        encodingMap[i].add(false);
                    } else {
                        encodingMap[i].add(true);
                    }
                }
            }
        }
        return encodingMap;
    }

    private BitStream encodeFile(List<Boolean>[] encodingTable, byte[] unCompressedArray) throws IOException {

        OutputStream stream = new FileOutputStream("CompressedStream.crunch");
        BitStream compressionStream = new BitStream(stream);
        List<Boolean> tempTest = new ArrayList<Boolean>();

        for (Byte currentByte : unCompressedArray) {
            List<Boolean> streamArray = encodingTable[currentByte + 128];
            String temp = String.valueOf(streamArray);
            for (Boolean direction: streamArray){
                if(!direction){
                    compressionStream.writeBit(0);
                }else{
                    compressionStream.writeBit(1);
                }
                totalBitCounter++;
            }
            //System.out.println(currentByte + " " + bitCounter);
            currentByteNumber++;
            if (currentByteNumber % 50000 == 0){
                System.out.println("Current Progress: " + currentByteNumber + "/" + unCompressedArray.length +
                        " " + (int)(((double) currentByteNumber/unCompressedArray.length) * 100) + "%\r");
            }
        }

        return compressionStream;

    }

    public void Compress(byte[] unCompressedArray) throws IOException {


        OutputStream stream = new FileOutputStream("CompressedStream.crunch");
        BitStream compressionStream = new BitStream(stream);

        System.out.println("File Loaded... Analyzing File");

        tableNode huffmanTree = createHuffmanTree(unCompressedArray);

        System.out.println("Huffman Tree Complete... Encoding File");

        List<Boolean>[] encodingTable = createEncodingTable(huffmanTree);

        System.out.println(Arrays.toString(encodingTable));

        BitStream compressedStream = encodeFile(encodingTable, unCompressedArray);

        compressedStream.flush();
        int buffer = compressedStream.getFlushPadding();

        metaData dataWrapper = new metaData("TestFile", huffmanTree, totalBitCounter, currentByteNumber, buffer);

        FileOutputStream fileOutputStream
                = new FileOutputStream("file.manifest");
        ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(dataWrapper);

//        List<fileEncoderThread> threads = new ArrayList<fileEncoderThread>();
//        long total = unCompressedArray.length;
//        long threadCount = 1;
//        long perThread = total/threadCount;
//        long lastEnd = 0;
//        int end = 0;
//        boolean allFinished = false;
//        for(int thread=1; thread < threadCount + 1; thread++) {
//            end += perThread;
//            fileEncoderThread fileThread = new fileEncoderThread(lastEnd, end, thread, unCompressedArray, finalNode);
//            threads.add(fileThread);
//            lastEnd = end;
//        }
//        for (fileEncoderThread value : threads) {
//            value.start();
//        }
//
//        while (!allFinished) {
//            allFinished = true;
//            for (fileEncoderThread threadn : threads) {
//                if (fileEncoderThread.running()) {
//                    allFinished = false;
//                }
//            }
//        }





        //System.out.println(finalNode);
        System.out.println(compressionStream.bitbuffer);
        compressionStream.close();
        System.out.println(Arrays.toString(unCompressedArray));
        File file = new File("unModified.txt");
        OutputStream os = new FileOutputStream(file);
        os.write(unCompressedArray);
        os.close();

        //System.out.println(valueTable);
        //compressedArray = (Byte[]) compressedList.toArray();
        //return compressedArray;
    }
}
