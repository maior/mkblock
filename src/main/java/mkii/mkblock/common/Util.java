package mkii.mkblock.common;

import java.sql.Timestamp;

import static mkii.mkblock.common.Constants.ANSI_RED;
import static mkii.mkblock.common.Constants.ANSI_RESET;

public class Util {

    public static boolean isAllZeroes(String data){
        for (char c: data.toCharArray()) {
            if (c != '0'){
                return false;
            }
        }
        return true;
    }

    public static boolean isInteger(String s){
        try{
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public static void OUTPRT(String s) {
        System.out.println(new Timestamp(System.currentTimeMillis()) + " - " + s);
    }

    public static void DOUTPRT(String s) {
        System.out.println(new Timestamp(System.currentTimeMillis()) + " - [" + ANSI_RED + "DEBUG" + ANSI_RESET + "] " + s);
    }
}
