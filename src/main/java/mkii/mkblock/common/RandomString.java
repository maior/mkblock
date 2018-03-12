package mkii.mkblock.common;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class RandomString {
    private final char[] buf;
    private final char[] symbols;
    private final Random random;

    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String lower = upper.toLowerCase(Locale.ROOT);
    public static final String digits = "0123456789";
    public static final String alphanum = upper + lower + digits;

    public RandomString(int length, Random random, String symbols) {
        if(length < 1) throw new IllegalArgumentException();
        if(symbols.length() < 2) throw new IllegalArgumentException();
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }

    public RandomString(int length, Random random) {
        this(length, random, alphanum);
    }

    public RandomString(int length) {
        this(length, new SecureRandom());
    }

    public RandomString() {
        this(21);
    }

    public String nextString() {
        for(int idx=0; idx<buf.length; idx++) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }
}
