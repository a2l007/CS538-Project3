package edu.indiana.p538;

import java.io.IOException;
import java.nio.ByteBuffer;

import static edu.indiana.p538.Utils.hexArray;

/**
 * Created by atmohan on 28-11-2016.
 */
public class TestBuffer {
    public static void main(String args[]){
        try {
            int num = 45;
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(num);
            byte[] bytes = b.array();
            char[] hexChars = new char[bytes.length * 2];
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            System.out.println(new String(hexChars));
        }
        catch(Exception ioe){
            ioe.printStackTrace();
        }
    }
}
