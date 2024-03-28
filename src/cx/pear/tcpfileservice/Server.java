package cx.pear.tcpfileservice;

import java.io.*;
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
import java.util.concurrent.Future;

public class Server {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("WHAT DO YOU THINK YOU ARE DOING?");
            return;
        }

        int port = Integer.parseInt(args[0]);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind(new InetSocketAddress(port));

        Future<?> serverListenTask = executor.submit(() -> {
            try {
                while (welcomeChannel.isOpen()) {
                    SocketChannel serveChannel = welcomeChannel.accept();

                    executor.submit(() -> {
                        try {
                            ByteBuffer request = ByteBuffer.allocate(1024);
                            int bytesRead = 0;
                            bytesRead = serveChannel.read(request);

                            request.flip();

                            byte[] clientQueryArray = new byte[bytesRead];
                            request.get(clientQueryArray);

                            String clientQuery = new String(clientQueryArray);
                            System.out.println(clientQuery);

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
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });

        String command;

        do {
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toLowerCase();
        } while (!command.equals("quit"));

        serverListenTask.cancel(true);
        executor.shutdown();
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
            FileOutputStream fileStream = new FileOutputStream(Paths.get("server_files", fileName.toString()).toString());
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
            FileInputStream fileStream = new FileInputStream(Paths.get("server_files", fileName.toString()).toString());
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
        Path newFilePath = Paths.get(Paths.get("server_files", mixedFileName.toString()).toString());

        String out = "F";

        Path filePath = Paths.get(Paths.get("server_files", fileName.toString()).toString());

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
        if(Files.deleteIfExists(Paths.get(Paths.get("server_files", fileName.toString()).toString()))){
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
        Set<String> files =listFilesUsingDirectoryStream("server_files");
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
