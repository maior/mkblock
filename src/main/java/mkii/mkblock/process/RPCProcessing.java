package mkii.mkblock.process;

import mkii.mkblock.address.MerkleTreeGenLimitless;
import mkii.mkblock.block.*;
import mkii.mkblock.common.Certificate;
import mkii.mkblock.common.TransactionUtility;

import java.util.ArrayList;
import java.util.Random;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Util.OUTPRT;

public class RPCProcessing {

    public static void ResponseProcess() {

        MerkleTreeGenLimitless treeGen = new MerkleTreeGenLimitless();
        /*
            Loop through RPC threads checking for new input
            MARKER: RPC parsing
             */
        for (int i = 0; i < rpcAgent.rpcThreads.size(); i++) {
            String request = rpcAgent.rpcThreads.get(i).req;
            if(request != null){
                OUTPRT("request : " + request);
                String[] parts = request.split(" ");
                parts[0] = parts[0].toLowerCase();
                if (parts[0].equals("getbalance")){
                    if (parts.length > 1){
                        rpcAgent.rpcThreads.get(i).res = databaseMaster.getAddressBalance(parts[1]) + "";
                    } else {
                        rpcAgent.rpcThreads.get(i).res = databaseMaster.getAddressBalance(addressManager.getDefaultAddress()) + "";
                    }
                } else if(parts[0].equals("getinfo")){
                    // TODO have this give more info
                    String res = "Blocks: " + databaseMaster.getBlockchainLength();
                    res += "\nLast block hash: " + databaseMaster.getBlock(databaseMaster.getBlockchainLength() - 1).blockHash;
                    res += "\nDifficulty: " + databaseMaster.getDifficulty();
                    res += "\nMain address (default): " + addressManager.getDefaultAddress();
                    res += "\nMain address balance: " + databaseMaster.getAddressBalance(addressManager.getDefaultAddress());
                    res += "\nLatest transaction: " + databaseMaster.getLatestBlock().txs.get(databaseMaster.getLatestBlock().txs.size() - 1);
                    rpcAgent.rpcThreads.get(i).res = res;
                } else if (parts[0].equals("send")){
                    BlockSendCoin.sendCoin(i, parts);
                } else if (parts[0].equals("submittx")){
                    if(TransactionUtility.isTransactionValid(parts[1])){
                        pendingTransactionContainer.addTransaction(parts[0]);
                        peerNetwork.broadcast("TRANSACTION " + parts[1]);
                        rpcAgent.rpcThreads.get(i).res = "Sent raw tx.";
                    } else {
                        rpcAgent.rpcThreads.get(i).res = "Invalid transaction";
                    }
                } else if (parts[0].equals("registercontract")){
                    OUTPRT("trying registerContract..... ");
                    String[] data = parts[1].split(":");
//                    String destAddr = data[0];
//                    String taxi = data[1];
//                    String contractData = data[2];
//                    String contractSouce = data[3];

                    BlockRegisterData.addContentDataBlock(i, data);

//                    OUTPRT("destAddr : " + destAddr);
//                    OUTPRT("taxi : " + taxi);
//                    String addr = addressManager.getDefaultAddress();
//                    String fullTx = addressManager.getSignedDataTransaction(destAddr, Double.valueOf(taxi), data, databaseMaster.getAddressSignatureIndex(addr) + addressManager.getDefaultOffset());
//                    addressManager.incrementDefaultOffset();
//                    if(TransactionUtility.isTransactionValid(fullTx)) {
//                        OUTPRT("Trying to verify transaction... " + "[True]");
//                        if(pendingTransactionContainer.addTransaction(fullTx)) {
//                            peerNetwork.broadcast("TRANSACTION " + fullTx);
//                            OUTPRT("Sending " + taxi + " to " + destAddr + " from " + addr);
//                            rpcAgent.rpcThreads.get(i).res = "Sent " + taxi + " from " + addr + " to " + destAddr;
//                        } else {
//                            rpcAgent.rpcThreads.get(i).res = "Add Transction Error";
//                        }
//                    } else {
//                        OUTPRT("Trying to verify transaction... " + "[False]");
//                        rpcAgent.rpcThreads.get(i).res = "Transction Error";
//                    }

                } else if (parts[0].equals("trypos")){
                    BlockPoS.getTryPoS(i);
                } else if (parts[0].equals("submitcert")){
                    SubmitCert.getSumbitCert(i, parts);
                } else if (parts[0].equals("gethistory")){
                    BlockHistory.getBlockHistory(i, parts);
                } else if (parts[0].equals("getpending")){
                    if(parts.length > 1){
                        rpcAgent.rpcThreads.get(i).res = "" + pendingTransactionContainer.getPendingBalance(parts[1]);
                    } else rpcAgent.rpcThreads.get(i).res = "get_pending <address>";
                } else if( parts[0].equals("getnewaccount")) {
                    String newKey = addressManager.getPrivateKey();
                    OUTPRT("Created PrivateKey : " + ANSI_GREEN + newKey + ANSI_RESET);
                    String address = treeGen.generateMerkleTree(newKey, 14, 16, 128);
                    OUTPRT("Created Address : " + ANSI_GREEN + address + ANSI_RESET);

                    String res = "";
                    res += "New PrivateKey : " + newKey + "\n";
                    res += "New Address : " + address + "\n";
                    rpcAgent.rpcThreads.get(i).res = res;

                } else if( parts[0].equals("getnewblock")) {

                } else {
                    rpcAgent.rpcThreads.get(i).res = "Unknown command: \"" + parts[0] + "\"";
                }
            }
        }
    }
}
