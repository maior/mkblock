package mkii.mkblock.address;

import mkii.mkblock.common.Constants;
import mkii.mkblock.common.RandomString;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import static mkii.mkblock.common.Util.OUTPRT;

public class AddressManager {
    private ArrayList<String> addrs;
    private ArrayList<String> privateKeys;
    private MerkleTreeGenLimitless treeGen;

    private int defaultOffset = 1;

    /**
     * Address Manager
     */
    public AddressManager() {
        this.privateKeys = new ArrayList<>();
        this.addrs = new ArrayList<>();
        treeGen = new MerkleTreeGenLimitless();
        try {
            File wallet = new File("wallet-keys.dat");
            if(!wallet.exists()) {
                OUTPRT("Generating new address...   ");
                String key = getPrivateKey();
                String address = treeGen.generateMerkleTree(key, 14, 16, 128);
                OUTPRT("New address : " + Constants.ANSI_GREEN + address + Constants.ANSI_RESET);

                PrintWriter out = new PrintWriter(wallet);
                out.println(address + ":" + key);
                out.close();

                addrs.add(address);
                privateKeys.add(key);

            } else {
                Scanner sc = new Scanner(wallet);
                while(sc.hasNextLine()) {
                    String in = sc.nextLine();
                    String addr = in.substring(0, in.indexOf(":"));
                    String privateKey = in.substring(in.indexOf(":") + 1);
                    addrs.add(addr);
                    privateKeys.add(privateKey);
                }
            }
            File addressFolder = new File("addresses");
            if(addressFolder.exists()) {
                Scanner sc = new Scanner(wallet);
                while(sc.hasNextLine()){
                    String[] combo = sc.nextLine().split(":");
                    treeGen.generateMerkleTree(combo[1], 14, 16, 128);
                }
                sc.close();
            } else {
                OUTPRT("No need to regenerate address file");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getDefaultOffset() {
        return defaultOffset;
    }

    public void incrementDefaultOffset() {
        defaultOffset++;
    }

    public void resetDefaultOffset() {
        defaultOffset = 1;
    }

    public String getPrivateKey() {
        return new RandomString().nextString();
    }

    public String getDefaultPrivateKey() {
        return privateKeys.get(0);
    }

    /**
     *
     * @param dest
     * @param amount
     * @param index
     * @return
     */
    public String getSignedTransaction(String dest, double amount, int index) {
        String data = getDefaultAddress() + "::" + amount + "::" + dest + "::" + amount;
        String pKey = getDefaultPrivateKey();
        String dAddr = getDefaultAddress();
        OUTPRT("index : " + index);
        OUTPRT("data : " + data);
        OUTPRT("pKey : " + pKey + ", dAddr : " + dAddr);
        String sig = new MerkleAddressUtility().getMerkleSignature(data, pKey, index, dAddr);
        String fullTx = data + "::" + sig + "::" + index;
        OUTPRT("fullTx : " + fullTx);
        return fullTx;
    }

    public String getDefaultAddress() {
        return addrs.get(0);
    }

    /**
     * get New Address
     * @return
     */
    public String getNewAddress() {
        String privateKey = getPrivateKey();
        String address = treeGen.generateMerkleTree(privateKey, 14, 16, 128);
        addrs.add(address);
        privateKeys.add(privateKey);
        return address;
    }
}
