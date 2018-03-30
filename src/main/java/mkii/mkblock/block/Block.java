package mkii.mkblock.block;

import mkii.mkblock.common.Certificate;
import mkii.mkblock.address.MerkleAddressUtility;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.util.ArrayList;

import static mkii.mkblock.common.Util.OUTPRT;

public class Block {
    public long timestamp;
    public int blockIndex;
    public String prevBlockHash;
    public String blockHash;
    public Certificate cert;
    public long difficulty;
    public int winningNonce;
    public String ledgerHash;
    public ArrayList<String> txs;
    public String minerSig;
    public long minerSigIndex;
    public String contentData;

    public Block(long timestamp, int blockIndex, String prevBlockHash, Certificate cert, long difficulty, int winningNonce, String ledgerHash, ArrayList<String> txs, String minerSig, int minerSigIndex, String contentData) {
        this.timestamp = timestamp;
        this.blockIndex = blockIndex;
        this.prevBlockHash = prevBlockHash;
        this.cert = cert;
        this.difficulty = difficulty;
        this.winningNonce = winningNonce;
        this.txs = txs;
        this.ledgerHash = ledgerHash;
        this.minerSig = minerSig;
        this.minerSigIndex = minerSigIndex;
        this.contentData = contentData;

        try {
            String txString = "";

            for(int i=0; i<txs.size(); i++) {
                if(txs.get(i).length() > 10) {
                    txString += txs.get(i) + "*";
                }
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            txString = txString.substring(0, txString.length() - 1);
            String blockData = "{" + timestamp + ":" + blockIndex + ":" + prevBlockHash + ":" + difficulty + ":" + winningNonce + "},{" + txString + "},{" + txString + "},";
            blockData += cert.getFullCertificate();
            blockData += contentData;

            this.blockHash = DatatypeConverter.printHexBinary(md.digest(blockData.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * check POW block
     * @return
     */
    public boolean isPoWBlock() {
        return cert.isPoWCertificate();
    }

    /**
     * Block
     * @param rawBlock
     */
    public Block(String rawBlock) {
        String[] parts = new String[11];
        parts[0] = rawBlock.substring(0, rawBlock.indexOf("}") + 1);
        rawBlock = rawBlock.substring(rawBlock.indexOf("}") + 2); //Account for comma
        parts[1] = rawBlock.substring(0, rawBlock.indexOf("}") + 1);
        rawBlock = rawBlock.substring(rawBlock.indexOf("}") + 2); //Account for comma, again
        parts[2] = rawBlock.substring(0, rawBlock.indexOf("}") + 1);
        rawBlock = rawBlock.substring(rawBlock.indexOf("}") + 2); //Account for comma a third time
        String[] partsInitial = rawBlock.split(",");
        for (int i = 3; i < 11; i++) {
            parts[i] = partsInitial[i - 3];
        }
        OUTPRT("Block parts in DB : " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            String toPrint = parts[i];
            if (parts[i].length() > 40)
                toPrint = parts[i].substring(0, 20) + "..." + parts[i].substring(parts[i].length() - 20);
            OUTPRT("     " + i + ": " + toPrint);
        }

        // It's Frist '{}'in DB file.
        //
        String firstPart = parts[0].replace("{", "");
        firstPart = firstPart.replace("}", "");
        String[] firstPartParts = firstPart.split(":");
        try {
            this.timestamp = Long.parseLong(firstPartParts[0]);
            this.blockIndex = Integer.parseInt(firstPartParts[1]);
            this.prevBlockHash = firstPartParts[2];
            this.difficulty = Long.parseLong(firstPartParts[3]);
            this.winningNonce = Integer.parseInt(firstPartParts[4]);
            this.ledgerHash = parts[1].replace("{", "").replace("}", "");
            this.contentData = partsInitial[8].replace("{", "").replace("}", "");;
            String transactionsString = parts[2].replace("{", "").replace("}", "");
            this.txs = new ArrayList<String>();

            //Transactions are separated by an asterisk, as the colon, double-colon, and comma are all used in other places, and would be a pain to use here.
            String[] rawTransactions = transactionsString.split("\\*");
            for (int i = 0; i < rawTransactions.length; i++) {
                this.txs.add(rawTransactions[i]);
            }
            this.cert = new Certificate(parts[3] + "," + parts[4] + "," + parts[5] + "," + parts[6]);
            //parts[7] is a block hash
            this.minerSig = parts[8].replace("{", "") + "," + parts[9].replace("}", "");
            this.minerSigIndex = Integer.parseInt(parts[10].replace("{", "").replace("}", ""));
            /*
             * Ugly, will fix later.
             */
            try {
                transactionsString = "";
                //Transaction format: FromAddress;InputAmount;ToAddress1;Output1;ToAddress2;Output2... etc.
                for (int i = 0; i < txs.size(); i++) {
                    if (txs.get(i).length() > 10) {
                        //Arbitrary number, make sure a transaction has some size to it
                        transactionsString += txs.get(i) + "*";
                    }
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                if (transactionsString.length() > 2) {
                    //Protect against empty transaction sets tripping errors with negative substring indices
                    transactionsString = transactionsString.substring(0, transactionsString.length() - 1);
                }
                String blockData = "{" + timestamp + ":" + blockIndex + ":" + prevBlockHash + ":" + difficulty + ":" + winningNonce + "},{" + ledgerHash + "},{" + transactionsString + "},";
                        blockData += cert.getFullCertificate();
                        blockData += getContentData();

                OUTPRT("blockData : " + blockData);
                this.blockHash = DatatypeConverter.printHexBinary(md.digest(blockData.getBytes("UTF-8")));

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get Miner
     * @return
     */
    public String getMiner() {
        return cert.redeemAddress;
    }

    /**
     * get ContentData
     * @return
     */
    public String getContentData() {
        return ",{" + this.contentData + "}";
    }

    /**
     * setting content data
     * @param data
     */
    public void setContentData(String data) {
        this.contentData = data;
    }

    /**
     * Checking block validation
     * @param chain
     * @return
     */
    public boolean validateBlock(BlockChain chain) {
        OUTPRT("Validating block " + blockIndex + "...");
        OUTPRT("Difficulty:" + difficulty);

        if (difficulty == 100000){

            // No validation needed, cert is filled with zeros
            if (winningNonce > cert.maxNonce){
                return false;
            }
            // No PoS before block 500
//            if (blockIndex < 500){
//                return false;
//            }

            // Address can not have mined a PoS block or sent a transaction in the last 50 blocks
            for (int i = blockIndex - 1; i > blockIndex - 50; i--) {
                if( i < 0 ) break;
                if (!chain.getBlock(i).isPoWBlock()){
                    if(chain.getBlock(i).getMiner().equals(cert.redeemAddress)){
                        return false; // Addr has mined a PoS block too recently
                    }
                }

                ArrayList<String> tsx = chain.getBlock(i).getTransactionsInvolvingAddress(cert.redeemAddress);
                for (String tx: txs) {
                    if (tx.split(":")[0].equals(cert.redeemAddress)){
                        return false;
                    }
                }
            }

            try {
                /*
                 * Transaction format:
                 * InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex
                 */
                if(!checkTxFormat()) return false;

            } catch (Exception e) { }
            // PoS block appears to be formatted correctly
            return true;
        } else if (difficulty == 150000) {
            // PoW block
            try
            {
                if (!cert.validCertificate()) {
                    OUTPRT("Certificate validation error");
                    return false; //Certificate is not valid.
                }
                if (winningNonce > cert.maxNonce) {
                    OUTPRT("Winning nonce error");
                    return false; //winningNonce is outside of the nonce range!
                }
                if (blockIndex != cert.blockIndex) {
                    OUTPRT("Block height does not match certificate height!");
                    return false; //Certificate and block height are not equal
                }
                long certificateScore = cert.getScoreAtNonce(winningNonce); //Lower score is better
                long target = Long.MAX_VALUE/(difficulty/2);
                if (certificateScore < target) {
                    OUTPRT("Certificate score error");
                    return false; //Certificate doesn't fall below the target difficulty when mined.
                }

                /*
                 * Transaction format:
                 * InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex
                 */
                if(!checkTxFormat()) return false;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checking Transaction Format
     * @return
     */
    public boolean checkTxFormat() {
        try {
            String transactionsString = "";
            //Transaction format: FromAddress;InputAmount;ToAddress1;Output1;ToAddress2;Output2... etc.
            for (int i = 0; i < txs.size(); i++) {
                if (txs.get(i).length() > 10) {
                    //Arbitrary number, makes sure empty transaction sets still function
                    transactionsString += txs.get(i) + "*";
                }
            }
            //Recalculate block hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (transactionsString.length() > 2) {
                //Prevent empty transaction sets from tripping with a negative substring index
                transactionsString = transactionsString.substring(0, transactionsString.length() - 1);
            }
            String blockData = "{" + timestamp + ":" + blockIndex + ":" + prevBlockHash + ":" + difficulty + ":" + winningNonce + "},{" + ledgerHash + "},{" + transactionsString + "}," + cert.getFullCertificate();
            String blockHash = DatatypeConverter.printHexBinary(md.digest(blockData.getBytes("UTF-8")));
            String fullBlock = blockData + ",{" + blockHash + "}"; //This is the message signed by the block miner
            MerkleAddressUtility MerkleAddressUtility = new MerkleAddressUtility();
            if (!MerkleAddressUtility.verifyMerkleSignature(fullBlock, minerSig, cert.redeemAddress, minerSigIndex))
            {
                OUTPRT("Block didn't verify for " + cert.redeemAddress + " with index " + minerSigIndex);
                OUTPRT("Signature mismatch error");
                OUTPRT("fullBlock: " + fullBlock);
                OUTPRT("minerSignature: " + minerSig);
                return false; //Block mining signature is not valid
            }
            if (txs.size() == 1 && txs.get(0).equals("")) {
                //Block has no explicit transactions
                return true;
            } else if (txs.size() == 0) {
                //Block has no explicit transactions
                return true;
            }
            for (int i = 0; i < txs.size(); i++) {
                try {
                    String tempTransaction = txs.get(i);
                    String[] transactionParts = tempTransaction.split(";");
                    if (transactionParts.length % 2 != 0 || transactionParts.length < 6) {
                        OUTPRT("Error validating block: transactionParts.length = " + transactionParts.length);
                        for (int j = 0; j < transactionParts.length; j++) {
                            OUTPRT("     " + j + ": " + transactionParts[j]);
                        }
                        return false; //Each address should line up with an output, and no explicit transaction is possible with fewer than six parts (see above)
                    }
                    //Last two parts are signatureData and signatureIndex,respectively
                    for (int j = 0; j < transactionParts.length - 2; j += 2) {
                        if (!MerkleAddressUtility.isAddressFormattedCorrectly(transactionParts[j])) {
                            OUTPRT("Error validating block: address " + transactionParts[j] + " is invalid.");
                            return false; //Address in transaction is misformatted
                        }
                    }
                    long inputAmount = Long.parseLong(transactionParts[1]);
                    long outputAmount = 0L;

                    //Element 3 (4th element) and each subsequent odd-numbered index up to transactionParts should be an output amount.
                    for (int j = 3; j < transactionParts.length - 2; j += 2) {
                        outputAmount += Long.parseLong(transactionParts[j]);
                    }
                    if (inputAmount - outputAmount < 0) {
                        OUTPRT("Error validating block: more coins output than input!");
                        return false; //Coins can't be created out of thin air!
                    }
                    String transactionData = "";
                    for (int j = 0; j < transactionParts.length - 2; j++) {
                        transactionData += transactionParts[j] + ";";
                    }
                    transactionData = transactionData.substring(0, transactionData.length() - 1);
                    if (!MerkleAddressUtility.verifyMerkleSignature(transactionData, transactionParts[transactionParts.length - 2], transactionParts[0], Long.parseLong(transactionParts[transactionParts.length - 1]))) {
                        OUTPRT("Error validating block: signature does not match!");
                        return false; //Siganture doesn't match
                    }
                } catch (Exception e) {
                    //Likely an error parsing a Long or performing some String manipulation task. Maybe array bounds exceptions.
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * get Transaction involving address
     * @param addr
     * @return
     */
    public ArrayList<String> getTransactionsInvolvingAddress(String addr){
        ArrayList<String> relevantParts = new ArrayList<>();
        for (int i = 0; i < txs.size(); i++) {
            String temp = txs.get(i);
            String[] txParts = temp.split("::");
            String sender = txParts[0];

            if (addr.equals(cert.redeemAddress)){
                relevantParts.add("COINBASE:100:" + cert.redeemAddress);
            }
            if(sender.equalsIgnoreCase(addr)){
                for (int j = 2; j < txParts.length - 2; j+=2) {
                    relevantParts.add(sender + ":" + txParts[j+1]);
                }
            } else {
                for(int j = 2; j < txParts.length - 2; j+=2){
                    if(txParts[j].equalsIgnoreCase(addr)){
                        relevantParts.add(sender + ":" + txParts[j+1] + ":" + txParts[j]);
                    }
                }
            }
        }
        return relevantParts;
    }

    /**
     * Returns the raw (String) representation of the block and its data.
     * @return String The raw block
     */
    public String getRawBlock(){

        String raw = "{" + timestamp + ":" + blockIndex + ":" + prevBlockHash + ":" + difficulty + ":" + winningNonce + "},{" + ledgerHash + "},{";
        String txString = "";
        for (int i = 0; i < txs.size(); i++) {
            if(txs.get(i).length() > 10){
                txString += txs.get(i) + "*";
            }
        }
        if(txString.length() > 2) {  // Prevent empty tx strings from throwing an IndexOutOfBounds exception
            txString = txString.substring(0, txString.length() - 1);
        }
        raw += txString + "}," + cert.getFullCertificate() + ",{" + blockHash + "},{" + minerSig + "},{" + minerSigIndex + "}";
        raw += getContentData();
        return raw;
    }
}
