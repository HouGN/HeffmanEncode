package eassen;

import java.io.IOException;

/**
 * @Author eassen
 * @Create 2021/1/19 10:01 下午
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String zipFile = "/Users/eassen/Desktop/1.txt";
        String zipFile2 = "/Users/eassen/Desktop/2.txt";
        String zipFile3 = "/Users/eassen/Desktop/3.txt";
        HuffmanEncode.zipFile(zipFile, zipFile2);
        HuffmanEncode.unzipFile(zipFile2, zipFile3);
        System.out.println("ok");
    }
}
