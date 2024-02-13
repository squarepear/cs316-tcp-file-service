package cx.pear.tcpfileservice;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws Exception{
        if (args.length != 1) {
            System.out.println("WHAT DO YOU THINK YOU ARE DOING?");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try {
            ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
            welcomeChannel.socket().bind(new InetSocketAddress(port));

            while (welcomeChannel.isOpen()) {
                SocketChannel serveChannel = welcomeChannel.accept();

                ByteBuffer request = ByteBuffer.allocate(1024);
                int bytesRead = serveChannel.read(request);

                request.flip();

                byte [] clientQueryArray = new byte[bytesRead];
                request.get(clientQueryArray);

                String clientQuery = new String(clientQueryArray);
                System.out.println(clientQuery);

                //receiveMessage(port);
                String command = clientQuery;
                switch (command) {
                    case "C": // Create
                        uploadFile(serveChannel, request);
                        break;
                    case "R": // Read
                        downloadFile(serveChannel, request);
                        break;
                    case "U": // Update
                        renameFile(serveChannel, request);
                        break;
                    case "D": // Delete
                        deleteFile(serveChannel, request);
                        break;
                    case "L": // List
                        listFiles(serveChannel, request);
                        break;
                    default:
                        System.out.println("AAAAAAHHHHHH I DONT KNOW WHAT THAT MEANS!");
                }

                serveChannel.close();
            }
        } catch (Exception ignored) {}
    }

    private static void uploadFile(SocketChannel serveChannel, ByteBuffer request) {
        System.out.println("Upload");
    }

    private static void downloadFile(SocketChannel serveChannel, ByteBuffer request) {
        System.out.println("Download");
    }

    private static void renameFile(SocketChannel serveChannel, ByteBuffer request) {
        System.out.println("Rename");
    }

    private static void deleteFile(SocketChannel serveChannel, ByteBuffer request) {
        System.out.println("Delete");
    }

    private static void listFiles(SocketChannel serveChannel, ByteBuffer request) throws Exception {
        Set<String> files =listFilesUsingDirectoryStream("files");
        StringBuilder fileList = new StringBuilder();
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

//    private static void receiveMessage(int port) throws Exception {
//        System.out.println("At least starting");
//        DatagramSocket socket = new DatagramSocket(port);
//        DatagramPacket clientRequest = new DatagramPacket(new byte[1024], 1024);
//        socket.receive(clientRequest);
//        System.out.println("Received");
//        byte[] content = Arrays.copyOf(clientRequest.getData(), clientRequest.getLength());
//        String clientMessage = new String(content);
//        System.out.println(clientMessage);
//    }
}
