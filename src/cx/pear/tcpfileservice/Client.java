package cx.pear.tcpfileservice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
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
        System.out.println(Arrays.toString(sendRequest(address, ByteBuffer.wrap("C".getBytes()))));
    }

    private static void downloadFile(InetSocketAddress address) {
        System.out.println(Arrays.toString(sendRequest(address, ByteBuffer.wrap("R".getBytes()))));
    }

    private static void renameFile(InetSocketAddress address) {
        System.out.println(Arrays.toString(sendRequest(address, ByteBuffer.wrap("U".getBytes()))));
    }

    private static void deleteFile(InetSocketAddress address) {
        System.out.println(Arrays.toString(sendRequest(address, ByteBuffer.wrap("D".getBytes()))));
    }

    private static void listFiles(InetSocketAddress address) {
        System.out.println(Arrays.toString(sendRequest(address, ByteBuffer.wrap("L".getBytes()))));
    }

    private static byte[] sendRequest(InetSocketAddress address, ByteBuffer message) {
        try (SocketChannel channel = SocketChannel.open()) {
            System.out.println("AAA");

            channel.connect(address);
            channel.write(message);

            System.out.println("BBB");

            ByteBuffer replyBuffer = ByteBuffer.allocate(1024);
            int bytesRead = channel.read(replyBuffer);

            System.out.println("CCC");

            replyBuffer.flip();

            byte[] replyArray = new byte[bytesRead];
            replyBuffer.get(replyArray);

            System.out.println("DDD");

            return replyArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
