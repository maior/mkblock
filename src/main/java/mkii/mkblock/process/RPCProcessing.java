package mkii.mkblock.process;

import mkii.mkblock.address.MerkleTreeGenLimitless;
import mkii.mkblock.block.Block;
import mkii.mkblock.block.BlockGenerator;
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
                    try{
                        double amount = Double.parseDouble(parts[1]);
                        String destAddr = parts[2];
                        String addr = addressManager.getDefaultAddress();
                        String fullTx = addressManager.getSignedTransaction(destAddr, amount, databaseMaster.getAddressSignatureIndex(addr) + addressManager.getDefaultOffset());
                        addressManager.incrementDefaultOffset();
                        OUTPRT("Trying to verify transaction... " + TransactionUtility.isTransactionValid(fullTx));
                        if (TransactionUtility.isTransactionValid(fullTx)){
                            pendingTransactionContainer.addTransaction(fullTx);
                            peerNetwork.broadcast("TRANSACTION " + fullTx);
                            OUTPRT("Sending " + amount + " to " + destAddr + " from " + addr);
                            rpcAgent.rpcThreads.get(i).res = "Sent " + amount + " from " + addr + " to " + destAddr;
                        } else {
                            rpcAgent.rpcThreads.get(i).res = "Unable to send: invalid transaction :(";
                        }
                    } catch (Exception e){
                        rpcAgent.rpcThreads.get(i).res = "Syntax (no '<' or '>'): send <amount> <dest>";
                    }
                } else if (parts[0].equals("submit_tx")){
                    if(TransactionUtility.isTransactionValid(parts[1])){
                        pendingTransactionContainer.addTransaction(parts[0]);
                        peerNetwork.broadcast("TRANSACTION " + parts[1]);
                        rpcAgent.rpcThreads.get(i).res = "Sent raw tx.";
                    } else {
                        rpcAgent.rpcThreads.get(i).res = "Invalid transaction";
                    }
                } else if (parts[0].equals("trypos")){
                    rpcAgent.rpcThreads.get(i).req = null;
                    // Address can not have mined a PoS block or sent a transaction in the last 50 blocks

                    String PoSAddress = addressManager.getDefaultAddress();

                    boolean conditionsMet = true;

                    for (int j = databaseMaster.getBlockchainLength() - 1; j > databaseMaster.getBlockchainLength() - 50; j--) {

                        if(j<0) break;

                        if (!databaseMaster.getBlock(j).isPoWBlock()) {
                            // Then PoS block
                            if (databaseMaster.getBlock(j).getMiner().equals(PoSAddress)) {
                                // Address has mined PoS block too recently!
                                rpcAgent.rpcThreads.get(i).res = "A PoS block was mined too recently: " + j;
                                conditionsMet = false;
                            }
                        }

                        ArrayList<String> transactions = databaseMaster.getBlock(i).getTransactionsInvolvingAddress(PoSAddress);
                        for (String transaction : transactions) {
                            if (transaction.split(":")[0].equals(PoSAddress)) {
                                // Address has sent coins too recently!
                                rpcAgent.rpcThreads.get(i).res = "A PoS block was mined too recently: " + j;
                                conditionsMet = false;
                            }
                        }
                    }

                    if (conditionsMet) {
                        OUTPRT("Last block: " + databaseMaster.getBlockchainLength());
                        OUTPRT("That block's hash: " + databaseMaster.getBlock(databaseMaster.getBlockchainLength() - 1).blockHash);
                        String previousBlockHash = databaseMaster.getBlock(databaseMaster.getBlockchainLength() - 1).blockHash;
                        double currentBalance = databaseMaster.getAddressBalance(PoSAddress);

                        int max = 9999999;
                        int min = 1111111;

                        long maxNonce = (int)currentBalance != 0 ? (int)currentBalance*100 : (new Random()).nextInt(max - min + 1) + min;
                        Certificate certificate = new Certificate(PoSAddress, "0", maxNonce, "0", databaseMaster.getBlockchainLength() + 1, previousBlockHash, 0, "0,0");

                        String[] scoreAndNonce = certificate.getMinCertificateScoreWithNonce().split(":");
                        int bestNonce = Integer.parseInt(scoreAndNonce[0]);
                        long lowestScore = Long.parseLong(scoreAndNonce[1]);
                        //long target = Long.MAX_VALUE/(100000/2); // Hard-coded PoS difficulty for this test
                        long target = Long.MAX_VALUE; // Hard-coded PoS difficulty for this test
                        if (lowestScore <= target)
                        {
                            try //Some stuff here may throw exceptions
                            {
                                //Great, certificate is a winning certificate!
                                //Gather all of the transactions from pendingTransactionContainer, check them.
                                ArrayList<String> allPendingTransactions = pendingTransactionContainer.pending;
                                OUTPRT("Initial pending pool size: " + allPendingTransactions.size());
                                allPendingTransactions = TransactionUtility.sortTransactionsBySignatureIndex(allPendingTransactions);
                                OUTPRT("Pending pool size after sorting: " + allPendingTransactions.size());
                                //All transactions have been ordered, and tested for validity. Now, we need to check account balances to make sure transactions are valid.
                                //As all transactions are grouped by address, we'll check totals address-by-address
                                ArrayList<String> finalTransactionList = new ArrayList<String>();
                                for (int j = 0; j < allPendingTransactions.size(); j++)
                                {
                                    String transaction = allPendingTransactions.get(j);
                                    String address = transaction.split("::")[0];
                                    //Begin at 0D, and add all outputs to exitBalance
                                    double exitBalance = 0D;
                                    double originalBalance = databaseMaster.getAddressBalance(address);
                                    //Used to keep track of the offset from j while still working on the same address, therefore not going through the entire for-loop again
                                    int counter = 0;
                                    //Previous signature count for an address--in order to ensure transactions use the correct indices
                                    long previousSignatureCount = databaseMaster.getAddressSignatureIndex(address);
                                    boolean foundNewAddress = false;
                                    while (!foundNewAddress && j + counter < allPendingTransactions.size()) {
                                        transaction = allPendingTransactions.get(j + counter);
                                        if (!address.equals(transaction.split("::")[0])) {
                                            foundNewAddress = true;
                                            address = transaction.split("::")[0];
                                            j = j + counter;
                                        } else {
                                            exitBalance += Long.parseLong(transaction.split("::")[1]); //Element at index 1 (2nd element) is the full output amount!
                                            if (exitBalance <= originalBalance && previousSignatureCount + 1 == Long.parseLong(transaction.split(";")[transaction.split(";").length - 1])) {
                                                //Transaction looks good!
                                                //Add seemingly-good transaction to the list, and increment previousSignatureCount for signature order assurance.
                                                finalTransactionList.add(transaction);
                                                OUTPRT("While making block, added transaction " + transaction);
                                                previousSignatureCount++;
                                            } else {
                                                OUTPRT("Transaction failed final validation...");
                                                OUTPRT("exitBalance: " + exitBalance);
                                                OUTPRT("originalBalance: " + originalBalance);
                                                OUTPRT("previousSignatureCount: " + previousSignatureCount);
                                                OUTPRT("signature count of new tx: " + Long.parseLong(transaction.split("::")[transaction.split("::").length - 1]));
                                            }
                                            //Counter keeps track of the sub-2nd-layer-for-loop incrementation along the ArrayList. It's kinda 3D.
                                            counter++;
                                        }
                                    }
                                }
                                //We have the transaction list; now we need to assemble the block.
                                //databaseMaster.getBlockchainLength() doesn't have one added to it to account for starting from 0!
                                String fullBlock = BlockGenerator.compileBlock(System.currentTimeMillis(), databaseMaster.getBlockchainLength(),
                                        databaseMaster.getLatestBlock().blockHash, 100000 /*fixed testnet PoS difficulty for now...*/,
                                        bestNonce, "0000000000000000000000000000000000000000000000000000000000000000",
                                        finalTransactionList, certificate, certificate.redeemAddress,
                                        addressManager.getDefaultPrivateKey(), databaseMaster.getAddressSignatureIndex(certificate.redeemAddress));

                                OUTPRT("Compiled PoS block: " + fullBlock);

                                //We finally have the full block. Now to submit it to ourselves...
                                Block toAdd = new Block(fullBlock);
                                boolean success = databaseMaster.addBlock(toAdd);

                                OUTPRT("Block add success: " + success);

                                if (success) {
                                    //The block appears legitimate to ourselves! Send it to others!
                                    peerNetwork.broadcast("BLOCK " + fullBlock);
                                    OUTPRT("PoS Block added to network successfully!");
                                    pendingTransactionContainer.reset(); //Any transactions left in pendingTransactionContainer that didn't get submitted into the block should be cleared anyway--they probably aren't valid for some reason, likely balance issues.
                                    addressManager.resetDefaultOffset();
                                } else {
                                    OUTPRT("Block was not added successfully! :(");
                                }
                                rpcAgent.rpcThreads.get(i).res = "Successfully submitted block! \nCertificate earned score " + lowestScore + "\nWhich is below target " + target + " so earned PoS!";
                            } catch (Exception e) {
                                rpcAgent.rpcThreads.get(i).res = "Failure to construct certificate!";
                                OUTPRT("Constructing certificate failed!");
                                e.printStackTrace();
                            }
                        } else {
                            rpcAgent.rpcThreads.get(i).res = "Pos mining failed with target score " + lowestScore + "\nWhich is above target " + target;
                        }
                    }
                } else if (parts[0].equals("submit_cert")){
                    rpcAgent.rpcThreads.get(i).req = null;
                    /*
                     * We have seven things to do:
                     * 1.) Check certificate for all nonces
                     * If 1. shows a difficulty above the network difficulty (below the target), proceed with creating a block:
                     * 2.) Gather all transactions from the pending transaction pool. Test all for validity. Test all under a max balance test.
                     * 3.) Put correct transactions in any arbitrary order, except for multiple transactions from the same address, which are ordered by signature index.
                     * 4.) Input the ledger hash (In 2.0.0a5, this is 0000000000000000000000000000000000000000000000000000000000000000, as ledger hashing isn't fully implemented)
                     * 5.) Hash the block
                     * 6.) Sign the block
                     * 7.) Return full block
                     * Steps 5, 6, and 7 are handled outside of MainClass, by a static method inside BlockGenerator.
                     */
                    //First, we'll check for the max difficulty.
                    Certificate certificate = new Certificate(parts[1]);
                    String[] scoreAndNonce = certificate.getMinCertificateScoreWithNonce().split(":");
                    int bestNonce = Integer.parseInt(scoreAndNonce[0]);
                    long lowestScore = Long.parseLong(scoreAndNonce[1]);
                    long target = Long.MAX_VALUE/(databaseMaster.getDifficulty()/2); //Difficulty and target have an inverse relationship.
                    if (lowestScore < target) {
                        try {
                            //Some stuff here may throw exceptions
                            //Great, certificate is a winning certificate!
                            //Gather all of the transactions from pendingTransactionContainer, check them.
                            ArrayList<String> allPendingTransactions = pendingTransactionContainer.pending;
                            OUTPRT("Initial pending pool size: " + allPendingTransactions.size());
                            allPendingTransactions = TransactionUtility.sortTransactionsBySignatureIndex(allPendingTransactions);
                            OUTPRT("Pending pool size after sorting: " + allPendingTransactions.size());
                            //All transactions have been ordered, and tested for validity. Now, we need to check account balances to make sure transactions are valid.
                            //As all transactions are grouped by address, we'll check totals address-by-address
                            ArrayList<String> finalTransactionList = new ArrayList<String>();
                            for (int j = 0; j < allPendingTransactions.size(); j++) {
                                String transaction = allPendingTransactions.get(j);
                                String address = transaction.split("::")[0];
                                //Begin at 0D, and add all outputs to exitBalance
                                double exitBalance = 0L;
                                double originalBalance = databaseMaster.getAddressBalance(address);
                                //Used to keep track of the offset from j while still working on the same address, therefore not going through the entire for-loop again
                                int counter = 0;
                                //Previous signature count for an address--in order to ensure transactions use the correct indices
                                long previousSignatureCount = databaseMaster.getAddressSignatureIndex(address);
                                boolean foundNewAddress = false;
                                while (!foundNewAddress && j + counter < allPendingTransactions.size()) {
                                    transaction = allPendingTransactions.get(j + counter);
                                    if (!address.equals(transaction.split("::")[0])) {
                                        foundNewAddress = true;
                                        address = transaction.split("::")[0];
                                        j = j + counter;
                                    } else {
                                        exitBalance += Long.parseLong(transaction.split("::")[1]); //Element at index 1 (2nd element) is the full output amount!
                                        if (exitBalance <= originalBalance && previousSignatureCount + 1 == Long.parseLong(transaction.split("::")[transaction.split("::").length - 1])) {
                                            //Transaction looks good!
                                            //Add seemingly-good transaction to the list, and increment previousSignatureCount for signature order assurance.
                                            finalTransactionList.add(transaction);
                                            OUTPRT("While making block, added transaction " + transaction);
                                            previousSignatureCount++;
                                        } else {
                                            OUTPRT("Transaction failed final validation...");
                                            OUTPRT("exitBalance: " + exitBalance);
                                            OUTPRT("originalBalance: " + originalBalance);
                                            OUTPRT("previousSignatureCount: " + previousSignatureCount);
                                            OUTPRT("signature count of new tx: " + Long.parseLong(transaction.split("::")[transaction.split("::").length - 1]));
                                        }
                                        //Counter keeps track of the sub-2nd-layer-for-loop incrementation along the ArrayList. It's kinda 3D.
                                        counter++;
                                    }
                                }
                            }

                            String fullBlock = BlockGenerator.compileBlock(System.currentTimeMillis(), databaseMaster.getBlockchainLength(), databaseMaster.getLatestBlock().blockHash,
                                    150000, bestNonce, "0000000000000000000000000000000000000000000000000000000000000000",
                                    finalTransactionList, certificate, certificate.redeemAddress, addressManager.getDefaultPrivateKey(),
                                    databaseMaster.getAddressSignatureIndex(certificate.redeemAddress));
                            //We finally have the full block. Now to submit it to ourselves...
                            Block toAdd = new Block(fullBlock);
                            boolean success = databaseMaster.addBlock(toAdd);
                            if (success) {
                                //The block appears legitimate to ourselves! Send it to others!
                                OUTPRT("Block added to network successfully!");
                                peerNetwork.broadcast("BLOCK " + fullBlock);
                                pendingTransactionContainer.reset(); //Any transactions left in pendingTransactionContainer that didn't get submitted into the block should be cleared anyway--they probably aren't valid for some reason, likely balance issues.
                                addressManager.resetDefaultOffset();
                            } else {
                                OUTPRT("Block was not added successfully! :(");
                            }
                            rpcAgent.rpcThreads.get(i).res = "Successfully submitted block! \nCertificate earned target score " + lowestScore + "\nWhich is below target " + target;
                        } catch (Exception e) {
                            rpcAgent.rpcThreads.get(i).res = "Failure to construct certificate!";
                            OUTPRT("Constructing certificate failed!");
                            e.printStackTrace();
                        }
                    } else {
                        rpcAgent.rpcThreads.get(i).res = "Certificate failed with target score " + lowestScore + "\nWhich is above target " + target;
                    }
                } else if (parts[0].equals("get_history")){
                    if (parts.length > 1){
                        ArrayList<String> allTransactions = databaseMaster.getAllTransactionsInvolvingAddress(parts[1]);
                        String allTransactionsFlat = "";
                        for (int j = 0; j < allTransactions.size(); j++) {
                            allTransactionsFlat += allTransactions.get(j) + "\n";
                        }
                        rpcAgent.rpcThreads.get(i).res = allTransactionsFlat;
                    } else {
                        rpcAgent.rpcThreads.get(i).res = "Syntax: get_history <address>";
                    }
                } else if (parts[0].equals("get_pending")){
                    if(parts.length > 1){
                        rpcAgent.rpcThreads.get(i).res = "" + pendingTransactionContainer.getPendingBalance(parts[1]);
                    } else rpcAgent.rpcThreads.get(i).res = "get_pending <address>";
                } else if( parts[0].equals("getnewaccount")) {
                    String newKey = addressManager.getPrivateKey();
                    System.out.println("Created PrivateKey : " + ANSI_GREEN + newKey + ANSI_RESET);
                    String address = treeGen.generateMerkleTree(newKey, 14, 16, 128);
                    System.out.println("Created Address : " + ANSI_GREEN + address + ANSI_RESET);

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
