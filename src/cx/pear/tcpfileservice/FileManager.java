package cx.pear.tcpfileservice;

import java.util.ArrayList;
import java.util.List;

public class FileManager {
    public List<String> getFileNames(byte[] bytes) {
        List<String> fileNames = new ArrayList<>();

        int i = 0;
        while (i < bytes.length) {
            int fileNameLength = bytes[i];
            StringBuilder fileNameBuilder = new StringBuilder();

            for (int j = 0; j < fileNameLength; j++) {
                if (++i >= bytes.length)
                    return fileNames;

                fileNameBuilder.append((char) bytes[i]);
            }

            fileNames.add(fileNameBuilder.toString());
            i++;
        }

        return fileNames;
    }
}
