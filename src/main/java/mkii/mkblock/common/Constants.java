package mkii.mkblock.common;

import mkii.mkblock.address.AddressManager;
import mkii.mkblock.block.DatabaseMaster;
import mkii.mkblock.network.PeerNetwork;
import mkii.mkblock.network.RPC;

import java.io.File;
import java.util.ArrayList;

public class Constants {
    public static final String[] FIXED_PEERS= {"192.168.0.4:8787"};

    public static final String VERSION = "0.1.0";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String INFO_FILENAME = "info.dat";

    public static DatabaseMaster databaseMaster;
    public static PendingTransactionContainer pendingTransactionContainer;
    public static PeerNetwork peerNetwork;
    public static RPC rpcAgent;
    public static File peerFile;
    public static ArrayList<String> peers;
    public static AddressManager addressManager;
}
