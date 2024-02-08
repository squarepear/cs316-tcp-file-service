package cx.pear.tcpfileservice;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    public static void main(String[] args) {
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

                String command = "C";
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

    private static void listFiles(SocketChannel serveChannel, ByteBuffer request) {
        System.out.println("List");
    }
}
