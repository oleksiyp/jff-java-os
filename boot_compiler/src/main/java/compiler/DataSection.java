package compiler;

import static java.util.Arrays.copyOf;

public class DataSection {
    byte []data;
    int len;

    public void append(byte ...bytes) {
        while (len + bytes.length > data.length) {
            data = copyOf(data, data.length * 2);
        }

        System.arraycopy(bytes, 0, data, len, bytes.length);
        len += bytes.length;
    }
}
