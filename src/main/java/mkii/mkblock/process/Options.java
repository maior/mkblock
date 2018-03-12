package mkii.mkblock.process;

import mkii.mkblock.address.MerkleTreeGenLimitless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static mkii.mkblock.common.Constants.*;

public class Options extends Thread {

    public void run() {

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.print(">> ");
                String s = br.readLine();

                if(s.equals("quit") || s.equals("exit")) {
                    break;
                }

                if(s.equals("getinfo")) {
                    // TODO have this give more info
                    String res = "Blocks: " + databaseMaster.getBlockchainLength();
                    res += "\nLast block hash: " + databaseMaster.getBlock(databaseMaster.getBlockchainLength() - 1).blockHash;
                    res += "\nDifficulty: " + databaseMaster.getDifficulty();
                    res += "\nMain address (default): " + addressManager.getDefaultAddress();
                    res += "\nMain address balance: " + databaseMaster.getAddressBalance(addressManager.getDefaultAddress());
                    res += "\nLatest transaction: " + databaseMaster.getLatestBlock().txs.get(databaseMaster.getLatestBlock().txs.size() - 1);

                    System.out.println(res);
                } else if(s.equals("getnewaccount")) {
                    MerkleTreeGenLimitless treeGen = new MerkleTreeGenLimitless();
                    String newKey = addressManager.getPrivateKey();
                    System.out.println("Created PrivateKey : " + ANSI_GREEN + newKey + ANSI_RESET);
                    String address = treeGen.generateMerkleTree(newKey, 14, 16, 128);
                    System.out.println("Created Address : " + ANSI_GREEN + address + ANSI_RESET);

                    //String fullTx = addressManager.getSignedTransaction(destAddr, amount, databaseMaster.getAddressSignatureIndex(addr) + addressManager.getDefaultOffset());
                }

            }
            br.close();
        } catch(IOException ioe){

        }
    }
}
