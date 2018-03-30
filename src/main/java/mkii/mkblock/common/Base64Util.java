package mkii.mkblock.common;

import org.apache.commons.codec.binary.Base64;

/**
 * convert Stirng to base64
 */
public class Base64Util {

    /**
     * encode String
     * @param str
     * @return
     */
    public static String getEncodeString(String str) {
        byte[] bytesEncoded = Base64.encodeBase64(str.getBytes());
        return new String(bytesEncoded);
    }

    /**
     * decode String
     * @param str
     * @return
     */
    public static String getDecodeString(byte[] str) {
        byte[] valueDecoded = Base64.decodeBase64(str);
        return new String(valueDecoded);
    }
}
