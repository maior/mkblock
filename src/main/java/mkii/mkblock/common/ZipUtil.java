package mkii.mkblock.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Compress & Uncompress Util
 */
public class ZipUtil {

    /**
     * Compress
     * @param str
     * @return
     */
    public static byte[] compress(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
            //return out.toString("ISO-8859-1");
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Decompress
     * @param str
     * @return
     */
    public static String decompress(byte[] str) {
        if (str == null || str.length == 0) {
            return null;
        }
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(str);
            GZIPInputStream gis = new GZIPInputStream(in);
            BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
            String outStr = "";
            String line;
            while ((line=bf.readLine())!=null) {
                outStr += line;
            }
            //System.out.println("Output String lenght : " + outStr.length());
            return outStr;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
