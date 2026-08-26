package random.nist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Immutable binary sequence consumed by NIST statistical tests. */
public final class BitSequence {
    private final byte[] bits;

    private BitSequence(byte[] bits) {
        this.bits = bits;
    }

    public static BitSequence fromBytes(byte[] bytes) {
        byte[] bits = new byte[Math.multiplyExact(bytes.length, Byte.SIZE)];
        for (int byteIndex = 0; byteIndex < bytes.length; byteIndex++) {
            int value = bytes[byteIndex] & 0xFF;
            for (int bit = 0; bit < Byte.SIZE; bit++) {
                bits[byteIndex * Byte.SIZE + bit] = (byte) ((value >>> (7 - bit)) & 1);
            }
        }
        return new BitSequence(bits);
    }

    public static BitSequence fromAsciiFile(Path path) throws IOException {
        String text = Files.readString(path);
        byte[] bits = new byte[text.length()];
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '0' || character == '1') {
                bits[count++] = (byte) (character - '0');
            } else if (!Character.isWhitespace(character)) {
                throw new IllegalArgumentException("Unexpected character in bit sequence at offset " + index + ".");
            }
        }
        return new BitSequence(Arrays.copyOf(bits, count));
    }

    public int length() {
        return bits.length;
    }

    public int bitAt(int index) {
        return bits[index];
    }

    public BitSequence slice(int fromIndex, int toIndex) {
        return new BitSequence(Arrays.copyOfRange(bits, fromIndex, toIndex));
    }
}
