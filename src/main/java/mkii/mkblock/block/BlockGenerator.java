package mkii.mkblock.block;

import mkii.mkblock.common.Certificate;
import mkii.mkblock.address.MerkleAddressUtility;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.ArrayList;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Util.OUTPRT;

public class BlockGenerator {
    public static String compileBlock(long timestamp, int blockIndex, String prevBlockHash, long difficulty, int winningNonce, String ledgerHash, ArrayList<String> txs, Certificate cert, String signingAddress, String privateKey, long minerSigIndex){
        OUTPRT(" [DAEMON] - " + ANSI_BLUE + "Creating block...");
        OUTPRT("\tTimestamp: " + timestamp);
        OUTPRT("\tBlock index: " + blockIndex);
        OUTPRT("\tPrevious block hash: " + prevBlockHash);
        OUTPRT("\tDifficulty: " + difficulty);
        OUTPRT("\tWinning nonce: " + winningNonce);
        OUTPRT("\tLedger hash: " + ledgerHash);
        OUTPRT("\tSigning address: " + signingAddress);
        OUTPRT("\tCertificate: " + cert);

        // Safety check
        if(minerSigIndex < 0){
            minerSigIndex = 0;
        }
        OUTPRT("\tMiner signature index: " + minerSigIndex);
        OUTPRT("\tPrivate key: " + privateKey);
        System.out.print(ANSI_RESET);

        OUTPRT("New block created and compiled");

        String block = "{" + timestamp + ":" + blockIndex + ":" + prevBlockHash + ":" + difficulty + ":" + winningNonce + "},{" + ledgerHash + "},{";
        String txString = "";
        for (int i = 0; i < txs.size(); i++) {
            if(txs.get(i).length() > 10){
                txString += txs.get(i) + "*";
            }
        }

        if (txString.length() > 1){
            txString = txString.substring(0, txString.length() - 1);
        }

        block += txString + "}," + cert.getFullCertificate();

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String blockHash = DatatypeConverter.printHexBinary(digest.digest(block.getBytes("UTF-8")));
            block += ",{" + blockHash + "}";
            OUTPRT(" [DAEMON] Pre-block: " + block);
            String sig = new MerkleAddressUtility().getMerkleSignature(block, privateKey, minerSigIndex, signingAddress);
            OUTPRT(" [DAEMON] - Signature: " + sig);
            block += ",{" + sig + "},{" + minerSigIndex + "}";
            return block;
        } catch (Exception e){
            OUTPRT("***Unable to sign a block***");
            OUTPRT(" [DAEMON] - " + ANSI_RED + "Unable to sign a block!" + ANSI_RESET);
            e.printStackTrace();
            return null;
        }

    }
}
