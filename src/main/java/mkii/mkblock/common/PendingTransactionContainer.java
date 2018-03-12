package mkii.mkblock.common;

import mkii.mkblock.block.Block;
import mkii.mkblock.block.DatabaseMaster;

import java.util.ArrayList;

import static mkii.mkblock.common.Constants.ANSI_YELLOW;
import static mkii.mkblock.common.Util.OUTPRT;

public class PendingTransactionContainer {
    public ArrayList<String> pending;
    private DatabaseMaster dbMaster;

    public ArrayList<StringDoublePair> accountBalanceDeltaTables; // pair addrs with their pending tx amounts

    public PendingTransactionContainer(DatabaseMaster dbMaster){
        this.dbMaster = dbMaster;
        this.pending = new ArrayList<>();
        this.accountBalanceDeltaTables = new ArrayList<>();
    }

    /**
     * Add Transaction
     * @param tx
     * @return
     */
    public boolean addTransaction(String tx){
        try{
            for (int i = 0; i < pending.size(); i++) {
                if(pending.get(i).equals(tx)){
                    return false;
                }
            }
            if(!TransactionUtility.isTransactionValid(tx)){
                OUTPRT("Ignoring invalid transaction: " + tx);
                return false;
            }
            String[] txParts = tx.split(":");
            String inAddr = txParts[0];
            double inAmount = Double.parseDouble(txParts[1]);
            double outstandingOutgoingAmount = 0D;
            int indexOfDelta = -1;
            for (int i = 0; i < accountBalanceDeltaTables.size(); i++) {
                if(accountBalanceDeltaTables.get(i).aString.equals(inAddr)){
                    outstandingOutgoingAmount = accountBalanceDeltaTables.get(i).aDouble;
                    indexOfDelta = i;
                    break;
                }
            }
            double prevBalance = dbMaster.getAddressBalance(inAddr);
            if (prevBalance < inAmount + outstandingOutgoingAmount){
                OUTPRT(inAddr + " tried to spend " + inAmount + " but only had " + (prevBalance - outstandingOutgoingAmount) + " coins.");
                return false;
            }
            if (indexOfDelta >= 0){
                accountBalanceDeltaTables.get(indexOfDelta).aDouble += inAmount;
            } else {
                accountBalanceDeltaTables.add(new StringDoublePair(inAddr, inAmount));
            }
            pending.add(tx); // Only here if tx is valid
            OUTPRT("Added transaction " + tx.substring(0, 20) + "..." + tx.substring(tx.length() - 20));
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Resets the pending transaction pool
     * @see ArrayList#clear()
     */
    public void reset(){
        pending.clear();
        accountBalanceDeltaTables.clear();
    }

    public boolean removeTransaction(String tx){
        for (int i = 0; i < pending.size(); i++) {
            if (pending.get(i).equals(tx)){
                pending.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove Transaction
     * @param raw
     * @return
     */
    public boolean removeTransactionsInBlock(String raw){
        try{
            Block temp = new Block(raw);
            // Tx format: inputAddr::inputAmt::outputAddr::etc.
            if(!temp.validateBlock(dbMaster.chain)){
                return false; // No txs to remove
            }
            ArrayList<String> txs = temp.txs;
            boolean allGood = true;
            for (int i = 0; i < txs.size(); i++) {
                if(!removeTransaction(txs.get(i))){
                    allGood = false;
                }
            }
            return allGood;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * get Pending balance
     * @param addr
     * @return
     */
    public double getPendingBalance(String addr){
        double delta = 0D;
        for (int i = 0; i < pending.size(); i++) {
            String tx = pending.get(i);
            try{
                if (tx.contains(addr)){
                    String[] txParts = tx.split("::");
                    String sender = txParts[0];
                    if(sender.equals(addr)){
                        delta += Double.parseDouble(txParts[1]);
                    }
                    for (int j = 2; j < txParts.length; j+=2) {
                        if(txParts[j].equals(addr)){
                            delta += Double.parseDouble(txParts[j+1]);
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                OUTPRT(ANSI_YELLOW + "Transaction in pending pool is incorrectly formatted. (Tx: " + tx + ")");
            }
        }
        return delta;
    }
}
