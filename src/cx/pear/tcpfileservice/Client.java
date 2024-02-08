package cx.pear.tcpfileservice;

import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("WHAT DO YOU THINK YOU ARE DOING?");
            return;
        }

        int serverPort = Integer.parseInt(args[1]);

        String command = "";

        while (!command.equals("quit")) {
            printHelp();

            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toLowerCase();

            switch (command) {
                case "upload":
                    uploadFile();
                    continue;
                case "download":
                    downloadFile();
                    continue;
                case "rename":
                    renameFile();
                    continue;
                case "delete":
                    deleteFile();
                    continue;
                case "list":
                    listFiles();
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
        System.out.println("Quit");
        System.out.println("Download");
        System.out.println("Rename");
        System.out.println("Delete");
        System.out.println("List");
    }

    private static void uploadFile() {
        System.out.println("Upload");
    }

    private static void downloadFile() {
        System.out.println("Download");
    }

    private static void renameFile() {
        System.out.println("Rename");
    }

    private static void deleteFile() {
        System.out.println("Delete");
    }

    private static void listFiles() {
        System.out.println("List");
    }
}
