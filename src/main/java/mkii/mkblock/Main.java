package mkii.mkblock;


import mkii.mkblock.process.Options;

import static mkii.mkblock.process.Initialize.mkblockInit;
import static mkii.mkblock.process.precessing.blockProcessing;

public class Main {

    /**
     * Main
     * @param args
     */
    public static void main(String[] args) {

        // option
        if(args.length > 0) {
            if (args[0].equals("console")) {
                Options options = new Options();
                options.start();
            }
        }

        try {
            mkblockInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // processing mkblock
        blockProcessing();


    }



}
