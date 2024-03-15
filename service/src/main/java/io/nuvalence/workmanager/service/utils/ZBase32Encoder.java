package io.nuvalence.workmanager.service.utils;

/**
 * Simple class that provides utilities to work with zbase32 encoding.
 */
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public final class ZBase32Encoder {

    private static final char[] ENCODE_CIPHER = "ybndrfg8ejkmcpqxot1uwisza345h769".toCharArray();
    private static final int PART_BIT_MASK = 0x0000001f;
    private static final int PART_BIT_WIDTH = 5;

    /**
     * Private constructor to prevent instantiation.
     */
    private ZBase32Encoder() {}

    /**
     * Given a long number, it`s zbase32 encoded.
     *
     * @param input long to be base32 encoded
     * @return encoded zBase64 String
     */
    public static String encode(final long input) {
        // always return a string of at least 1 character
        if (input == 0) {
            return String.valueOf(ENCODE_CIPHER[0]);
        }

        final StringBuilder builder = new StringBuilder();

        long bits = input;
        int bitsShifted = 0;
        while (bitsShifted < Long.SIZE) {
            final int part = extractEncodablePart(bits);

            // ignore leading y characters (zeros)
            if (part > 0 || builder.length() > 0) {
                builder.append(ENCODE_CIPHER[part]);
            }

            bits = bits << PART_BIT_WIDTH;
            bitsShifted += PART_BIT_WIDTH;
        }

        return builder.toString();
    }

    private static int extractEncodablePart(long bits) {
        return (int) (bits >>> (Long.SIZE - PART_BIT_WIDTH)) & PART_BIT_MASK;
    }
}
