package fr.uga.miashs.dciss.chatservice.common;

import java.io.*;

public class FileTransfer {
    private String fileName;
    private long fileSize;
    private byte[] fileData;
    
    public FileTransfer(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
        this.fileSize = fileData.length;
    }
    
    // Getters
    public String getFileName() {
        return fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    // Convertit l'objet en bytes pour l'envoi
    public static byte[] toBytes(FileTransfer ft) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        
        dos.writeByte(5); // Type 5 = transfert de fichier
        dos.writeUTF(ft.fileName);
        dos.writeLong(ft.fileSize);
        dos.write(ft.fileData);
        
        return bos.toByteArray();
    }
    
    // Crée un FileTransfer depuis des bytes reçus
    public static FileTransfer fromBytes(byte[] data) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bis);
        
        dis.skipBytes(1); // Skip type byte
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        
        byte[] fileContent = new byte[(int)fileSize];
        dis.readFully(fileContent);
        
        return new FileTransfer(fileName, fileContent);
    }
}