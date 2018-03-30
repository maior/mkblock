package mkii.mkblock.block;

import mkii.mkblock.common.Certificate;
import mkii.mkblock.common.TransactionUtility;

import java.util.ArrayList;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Constants.addressManager;
import static mkii.mkblock.common.Constants.rpcAgent;
import static mkii.mkblock.common.Util.OUTPRT;

public class SubmitCert {
    public static void getSumbitCert(int i, String[] parts) {
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
                String ledgerHash = databaseMaster.chain.ledgerManager.getLedgerHash();
                OUTPRT("ledgerHash : " + ledgerHash);

                String fullBlock = BlockGenerator.compileBlock(System.currentTimeMillis(), databaseMaster.getBlockchainLength(), databaseMaster.getLatestBlock().blockHash,
                        150000, bestNonce, ledgerHash,
                        finalTransactionList, certificate, certificate.redeemAddress, addressManager.getDefaultPrivateKey(),
                        databaseMaster.getAddressSignatureIndex(certificate.redeemAddress), "");
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
    }
}
