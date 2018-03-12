package mkii.mkblock.process;
import mkii.mkblock.address.AddressManager;
import mkii.mkblock.block.DatabaseMaster;
import mkii.mkblock.common.Constants;
import mkii.mkblock.common.PendingTransactionContainer;
import mkii.mkblock.network.PeerNetwork;
import mkii.mkblock.network.RPC;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Util.OUTPRT;

public class Initialize {

    public static void mkblockInit() throws Exception {

        jrecheck();

        try{
            Thread.sleep(1000);
        } catch (Exception e){}

        databaseMaster = new DatabaseMaster("mkii-db");
        pendingTransactionContainer = new PendingTransactionContainer(databaseMaster);

        OUTPRT("Initiating peer network...   ");
        peerNetwork = new PeerNetwork();
        peerNetwork.start();
        OUTPRT("[ " + ANSI_GREEN + "OK" + ANSI_RESET + " ]");

        OUTPRT("Starting RPC daemon...   ");
        rpcAgent = new RPC();
        rpcAgent.start();
        OUTPRT("[ " + ANSI_GREEN + "OK" + ANSI_RESET + " ]");

        peerFile = new File("peers.list");
        peers = new ArrayList<>();

        addressManager = new AddressManager();
        if(!peerFile.exists()) {
            try {
                PrintWriter out = new PrintWriter(peerFile);
                for(int i = 0; i< Constants.FIXED_PEERS.length; i++) {
                    out.println(Constants.FIXED_PEERS[i]);
                }
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Scanner sc = new Scanner(peerFile);
            while(sc.hasNextLine()) {
                String combo = sc.nextLine();
                peers.add(combo);
                String host = combo.substring(0, combo.indexOf(":"));
                int port = Integer.parseInt(combo.substring(combo.indexOf(":") + 1));
                peerNetwork.connectToPeer(host, port);
            }
            sc.close();;
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jrecheck() throws IOException {
        OUTPRT("=========== MKII BLOCK CHAIN ===========");
        OUTPRT("Setting up a few things...");
        OUTPRT("Checking for a valid JRE Version...");
        int minor = Integer.parseInt(String.valueOf(System.getProperty("java.version").charAt(2)));

        if ((minor < 8) && (Integer.parseInt(String.valueOf(System.getProperty("java.version").charAt(0))) != 9)){
            OUTPRT("[  " + ANSI_RED + "FAIL" + ANSI_WHITE + "  ]");
            OUTPRT("mkblock needs a JRE version of 1.8 or greater, yours is " + ANSI_RED +
                    System.getProperty("java.version") + ANSI_WHITE);
            System.exit(-1);
        } else {
            OUTPRT("[  " + ANSI_GREEN + "OK" + ANSI_RESET + "  ]");
        }
    }
}
