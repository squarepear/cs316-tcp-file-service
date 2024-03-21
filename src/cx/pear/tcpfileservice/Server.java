package cx.pear.tcpfileservice;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) throws Exception{
        if (args.length != 1) {
            System.out.println("WHAT DO YOU THINK YOU ARE DOING?");
            return;
        }

        int port = Integer.parseInt(args[0]);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
            welcomeChannel.socket().bind(new InetSocketAddress(port));

            for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
                executor.submit(() -> {
                    while (welcomeChannel.isOpen()) {
                        try {
                            SocketChannel serveChannel = welcomeChannel.accept();

                            ByteBuffer request = ByteBuffer.allocate(1024);
                            int bytesRead = serveChannel.read(request);

                            request.flip();

                            byte[] clientQueryArray = new byte[bytesRead];
                            request.get(clientQueryArray);

                            String clientQuery = new String(clientQueryArray);
                            //TODO//
                            System.out.println(clientQuery);

                            //receiveMessage(port);
                            char command = clientQuery.charAt(0);
                            switch (command) {
                                case 'C': // Create
                                    uploadFile(serveChannel, request);
                                    break;
                                case 'R': // Read
                                    downloadFile(serveChannel, request);
                                    break;
                                case 'U': // Update
                                    renameFile(serveChannel, request);
                                    break;
                                case 'D': // Delete
                                    deleteFile(serveChannel, request);
                                    break;
                                case 'L': // List
                                    listFiles(serveChannel, request);
                                    break;
                                default:
                                    System.out.println("AAAAAAHHHHHH I DONT KNOW WHAT THAT MEANS!");
                            }

                            serveChannel.close();
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        } catch (Exception ignored) {}

        String command;

        do {
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toLowerCase();
        } while (!command.equals("quit"));

        executor.shutdownNow();
    }

    private static void uploadFile(SocketChannel serveChannel, ByteBuffer request) {
        String fileString = new String(request.array());
        StringBuilder mixedFileName = new StringBuilder();
        StringBuilder fileName = new StringBuilder();
        for (char letter : fileString.toCharArray()){
            if(letter != 0){
                mixedFileName.append(letter);
            }
        }

        mixedFileName.deleteCharAt(0);
        int fileNameLength = mixedFileName.charAt(0);
        mixedFileName.deleteCharAt(0);

        for(int i =0; i<fileNameLength; i++){
            fileName.append(mixedFileName.charAt(0));
            mixedFileName.deleteCharAt(0);
        }

        try {
            FileOutputStream fileStream = new FileOutputStream("server_files/" + fileName);
            FileChannel fileChannel = fileStream.getChannel();

            request.flip();

            request.position(fileNameLength + 2);

            fileChannel.write(request);

            ByteBuffer content = ByteBuffer.allocate(1024);
            while (serveChannel.read(content) >= 0) {
                content.flip();

                fileChannel.write(content);
                content.clear();
            }

            fileStream.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void downloadFile(SocketChannel serveChannel, ByteBuffer request) {
        // Request format: "R<filename>"
        // Response format: "<file content>"
        String fileString = new String(request.array());
        StringBuilder fileName = new StringBuilder();
        for (char letter : fileString.toCharArray()){
            if (letter == 0)
                break;

            fileName.append(letter);
        }
        fileName.deleteCharAt(0);

        try {
            FileInputStream fileStream = new FileInputStream("server_files/" + fileName);
            FileChannel fileChannel = fileStream.getChannel();
            ByteBuffer content = ByteBuffer.allocate(1024);

            while (fileChannel.read(content) >= 0) {
                content.flip();
                serveChannel.write(content);
                content.clear();
            }

            fileStream.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private static void renameFile(SocketChannel serveChannel, ByteBuffer request) throws Exception{
        String fileString = new String(request.array());
        StringBuilder mixedFileName = new StringBuilder();
        StringBuilder fileName = new StringBuilder();
        for (char letter : fileString.toCharArray()){
            if(letter != 0){
                mixedFileName.append(letter);
            }
        }
        mixedFileName.deleteCharAt(0);
        int fileNameLength = mixedFileName.charAt(0);
        mixedFileName.deleteCharAt(0);
        for(int i =0; i<fileNameLength; i++){
            fileName.append(mixedFileName.charAt(0));
            mixedFileName.deleteCharAt(0);
        }
        Path newFilePath = Paths.get("server_files/" + mixedFileName);

        String out = "F";

        Path filePath = Paths.get("server_files/" + fileName);

        if(Files.exists(filePath)){
            File file = new File(String.valueOf(filePath));
            File newFileName = new File(newFilePath.toString());

            if(file.renameTo(newFileName))
                out = "S";
        }

        ByteBuffer replyBuffer = ByteBuffer.wrap(out.getBytes());
        serveChannel.write(replyBuffer);

        serveChannel.close();
    }

    private static void deleteFile(SocketChannel serveChannel, ByteBuffer request) throws Exception {
        String fileString = new String(request.array());
        StringBuilder fileName = new StringBuilder();
        for (char letter : fileString.toCharArray()){
            if(letter != 0){
                fileName.append(letter);
            }
        }
        fileName.deleteCharAt(0);
        byte[] out = null;
        if(Files.deleteIfExists(Paths.get("server_files/"+ fileName))){
            out = "S".getBytes();
        }
        else{
            out = "F".getBytes();
        }
        ByteBuffer replyBuffer = ByteBuffer.wrap(out);
        serveChannel.write(replyBuffer);

        serveChannel.close();

    }

    private static void listFiles(SocketChannel serveChannel, ByteBuffer request) throws Exception {
        Set<String> files =listFilesUsingDirectoryStream("server_files/");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (String file : files) {

            output.write(file.length());
            output.write(file.getBytes());
        }

        byte[] out = output.toByteArray();
        ByteBuffer replyBuffer = ByteBuffer.wrap(out);
        serveChannel.write(replyBuffer);

        serveChannel.close();
    }



    public static Set<String> listFilesUsingDirectoryStream(String dir) throws Exception {
        Set<String> fileSet = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileSet.add(path.getFileName()
                            .toString());
                }
            }
        }
        return fileSet;
    }
}
