package cx.pear.tcpfileservice;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 2) {
            System.out.println("Syntax: Client <ServerIP> <ServerPort>");
            return;
        }

        InetAddress serverIP = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);
        InetSocketAddress serverAddress = new InetSocketAddress(serverIP, serverPort);

        if (serverPort <= 0) {
            System.out.println("ServerPort must be a positive integer!");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        System.out.println("Welcome to the File Service Client!");

        String command;

        do {
            printHelp();

            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toLowerCase();

            switch (command) {
                case "upload":
                    uploadFile(serverAddress, executor);
                    continue;
                case "download":
                    downloadFile(serverAddress, executor);
                    continue;
                case "rename":
                    renameFile(serverAddress);
                    continue;
                case "delete":
                    deleteFile(serverAddress);
                    continue;
                case "list":
                    listFiles(serverAddress);
                    continue;
                case "quit":
                    continue;
            }

            System.out.println("Invalid Command!");
        } while (!command.equals("quit"));

        System.out.println("Program exiting");
    }

    private static void printHelp() {
        System.out.println("Upload");
        System.out.println("Download");
        System.out.println("Rename");
        System.out.println("Delete");
        System.out.println("List");
        System.out.println("Quit");
    }

    private static void uploadFile(InetSocketAddress address, ExecutorService executor) {
        System.out.println("Enter name of file to upload: ");
        Scanner keyboard = new Scanner(System.in);
        String fileName = keyboard.nextLine().toLowerCase();

        executor.submit(() -> {
            try {
                FileInputStream fileStream = new FileInputStream(Paths.get("client_files", fileName).toString());
                FileChannel fileChannel = fileStream.getChannel();

                ByteBuffer request = ByteBuffer.allocate(1024);
                request.put(("C" + (char) fileName.length() + fileName).getBytes());
                request.flip();

                SocketChannel channel = sendRequest(address, request);

                ByteBuffer content = ByteBuffer.allocate(1024);
                while (fileChannel.read(content) >= 0) {
                    content.flip();
                    channel.write(content);
                    content.clear();
                }

                fileStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        });
    }

    private static void downloadFile(InetSocketAddress address, ExecutorService executor) {
        System.out.print("Enter name of file to download: ");

        Scanner keyboard = new Scanner(System.in);
        String fileName = keyboard.nextLine().toLowerCase();

        executor.submit(() -> {
            try {
                SocketChannel channel = sendRequest(address, ByteBuffer.wrap(("R" + fileName).getBytes()));
                FileOutputStream fileStream = new FileOutputStream(Paths.get("client_files", fileName).toString(), true);

                FileChannel fileChannel = fileStream.getChannel();

                ByteBuffer content = ByteBuffer.allocate(1024);
                while (channel.read(content) >= 0) {
                    content.flip();

                    fileChannel.write(content);
                    content.clear();
                }

                fileStream.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        });
    }

    private static void renameFile(InetSocketAddress address) {
        Scanner keyboard = new Scanner(System.in);

        System.out.print("Enter name of file to rename: ");
        String fileName = keyboard.nextLine().toLowerCase();
        int fileNameLength = fileName.length();

        System.out.print("Enter new name for the file: ");
        String newFileName = keyboard.nextLine().toLowerCase();

        String requestMessage = "U" + (char) fileNameLength + fileName + newFileName;
        ByteBuffer request = ByteBuffer.wrap(requestMessage.getBytes());

        SocketChannel channel = sendRequest(address, request);
        byte[] bytes = readResponse(channel);

        char response = (char) bytes[0];

        switch (response) {
            case 'S':
                System.out.println("File successfully renamed!");
                break;
            case 'F':
                System.out.println("File not renamed...");
                break;
            default:
                System.out.println("Server error...");
                break;
        }
    }

    private static void deleteFile(InetSocketAddress address) {
        System.out.print("Enter name of file to delete: ");

        Scanner keyboard = new Scanner(System.in);
        String fileName = keyboard.nextLine().toLowerCase();

        SocketChannel channel = sendRequest(address, ByteBuffer.wrap(("D" + fileName).getBytes()));
        byte[] bytes = readResponse(channel);

        char response = (char) bytes[0];

        switch (response) {
            case 'S':
                System.out.println("File successfully deleted!");
                break;
            case 'F':
                System.out.println("File not deleted...");
                break;
            default:
                System.out.println("Server error...");
                break;
        }
    }

    private static void listFiles(InetSocketAddress address) {
        SocketChannel channel = sendRequest(address, ByteBuffer.wrap("L".getBytes()));
        byte[] bytes = readResponse(channel);
        FileManager fileManager = new FileManager();

        List<String> fileNames = fileManager.getFileNames(bytes);

        System.out.println(fileNames);
    }

    private static SocketChannel sendRequest(InetSocketAddress address, ByteBuffer message) {
        try {
            SocketChannel channel = SocketChannel.open();
            channel.connect(address);
            channel.write(message);

            return channel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readResponse(SocketChannel channel) {
        try {
            ByteBuffer replyBuffer = ByteBuffer.allocate(4096);
            int bytesRead = channel.read(replyBuffer);

            return getBytesFromBuffer(replyBuffer, bytesRead);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getBytesFromBuffer(ByteBuffer buffer, int length) {
        buffer.flip();

        byte[] replyArray = new byte[length];
        buffer.get(replyArray);

        return replyArray;
    }
}
