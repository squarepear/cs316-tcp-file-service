package cx.pear.tcpfileservice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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

        String command = "";

        while (!command.equals("quit")) {
            printHelp();

            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toLowerCase();

            switch (command) {
                case "upload":
                    uploadFile(serverAddress);
                    continue;
                case "download":
                    downloadFile(serverAddress);
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
        }

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

    private static void uploadFile(InetSocketAddress address) {
        byte[] bytes = sendRequest(address, ByteBuffer.wrap("C".getBytes()));

        System.out.println(Arrays.toString(bytes));
    }

    private static void downloadFile(InetSocketAddress address) {
        byte[] bytes = sendRequest(address, ByteBuffer.wrap("R".getBytes()));

        System.out.println(Arrays.toString(bytes));
    }

    private static void renameFile(InetSocketAddress address) {
        byte[] bytes = sendRequest(address, ByteBuffer.wrap("U".getBytes()));

        System.out.println(Arrays.toString(bytes));
    }

    private static void deleteFile(InetSocketAddress address) {
        System.out.print("Enter name of file to delete: ");

        Scanner keyboard = new Scanner(System.in);
        String fileName = keyboard.nextLine().toLowerCase();


        byte[] bytes = sendRequest(address, ByteBuffer.wrap(("D" + fileName).getBytes()));

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
        byte[] bytes = sendRequest(address, ByteBuffer.wrap("L".getBytes()));
        FileManager fileManager = new FileManager();

        List<String> fileNames = fileManager.getFileNames(bytes);

        System.out.println(fileNames);
    }

    private static byte[] sendRequest(InetSocketAddress address, ByteBuffer message) {
        try (SocketChannel channel = SocketChannel.open()) {
            channel.connect(address);
            channel.write(message);

            ByteBuffer replyBuffer = ByteBuffer.allocate(4096);
            int bytesRead = channel.read(replyBuffer);

            replyBuffer.flip();

            byte[] replyArray = new byte[bytesRead];
            replyBuffer.get(replyArray);

            return replyArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
