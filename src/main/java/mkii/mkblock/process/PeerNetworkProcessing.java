package mkii.mkblock.process;

import mkii.mkblock.block.Block;
import mkii.mkblock.common.TransactionUtility;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import static mkii.mkblock.common.Constants.*;
import static mkii.mkblock.common.Constants.peerNetwork;
import static mkii.mkblock.common.Util.OUTPRT;

public class PeerNetworkProcessing {
    private static ArrayList<String> allBroadcastTransactions = new ArrayList<>();
    private static ArrayList<String> allBroadcastBlocks = new ArrayList<>();
    private static int topBlock = 0;
    private static boolean catchupMode = true;

    public static void PNProcessing() {

        if(peerNetwork.newPeers.size() > 0) {
            for(int i=0; i<peerNetwork.newPeers.size(); i++) {
                if(peers.indexOf(peerNetwork.newPeers.get(i)) < 0) {
                    peers.add(peerNetwork.newPeers.get(i));
                }
            }

            peerNetwork.newPeers = new ArrayList<>();
            try {
                PrintWriter writerPeerFile = new PrintWriter(new File("peers.list"));
                for(int i=0; i<peers.size(); i++) {
                    writerPeerFile.println(peers.get(i));
                }
                writerPeerFile.close();
            } catch (Exception e) {
                OUTPRT("Error : Unable to write to peer file.");
                e.printStackTrace();
            }
        }

        for(int i=0; i<peerNetwork.peerThreads.size(); i++) {
            ArrayList<String> input = peerNetwork.peerThreads.get(i).inputThread.readData();
            if(input == null) {
                OUTPRT("Null ret retry.");
                System.exit(-5);
                break;
            }

            for(int j=0; j<input.size(); j++) {
                String data = input.get(j);
                if(data.length() > 60) {
                    OUTPRT("Got data : " + data.substring(0, 30) + "..." + data.substring(data.length() - 30, data.length()));
                } else {
                    OUTPRT("Got data : " + data);
                }
                String[] parts = data.split(" ");
                if(parts.length > 0) {
                    if(parts[0].equalsIgnoreCase("NETWORK_STATE")) {
                        topBlock = Integer.parseInt(parts[1]);
                    } else if(parts[0].equalsIgnoreCase("REQUEST_NET_STATE")) {
                        if(databaseMaster.getBlockchainLength() != 0)
                            peerNetwork.peerThreads.get(i).outputThread.write("NETWORK_STATE " + databaseMaster.getBlockchainLength() + " " + databaseMaster.getLatestBlock().blockHash);
                        else
                            peerNetwork.peerThreads.get(i).outputThread.write("NETWORK_STATE " + databaseMaster.getBlockchainLength());
                        for (int k = 0; k < pendingTransactionContainer.pending.size(); k++) {
                            peerNetwork.peerThreads.get(i).outputThread.write("TRANSACTION " + pendingTransactionContainer.pending.get(k));
                        }
                    } else if(parts[0].equalsIgnoreCase("BLOCK")) {
                        OUTPRT("Attempting to add block...");
                        boolean hasSeenBlockBefore = false;
                        for (int k = 0; k < allBroadcastBlocks.size(); k++) {
                            if(parts[1].equals(allBroadcastBlocks.get(k))){
                                hasSeenBlockBefore = true;
                            }
                        }

                        if(!hasSeenBlockBefore){
                            OUTPRT("Adding new block from network...");
                            OUTPRT("Block: ");
                            OUTPRT(parts[1].substring(0, 30) + "...");
                            allBroadcastBlocks.add(parts[1]);
                            Block blockToAdd = new Block(parts[1]);
                            if(databaseMaster.addBlock(blockToAdd) && !catchupMode){
                                OUTPRT("Added block " + blockToAdd.blockIndex + " with hash: [" + blockToAdd.blockHash.substring(0, 30) + "..." + blockToAdd.blockHash.substring(blockToAdd.blockHash.length() - 30, blockToAdd.blockHash.length() - 1) + "]");
                                peerNetwork.broadcast("BLOCK " + parts[1]);
                            }
                            pendingTransactionContainer.removeTransactionsInBlock(parts[1]);
                        }
                    } else if(parts[0].equalsIgnoreCase("TRANSACTION")) {
                        boolean alreadyExisted = false;
                        for (int k = 0; k < allBroadcastBlocks.size(); k++) {
                            if(parts[1].equalsIgnoreCase(allBroadcastTransactions.get(k))){
                                alreadyExisted = true;
                            }
                        }
                        if(!alreadyExisted){
                            allBroadcastTransactions.add(parts[1]);
                            pendingTransactionContainer.addTransaction(parts[1]);
                            if(TransactionUtility.isTransactionValid(parts[1])){
                                OUTPRT("New tx on network: ");
                                String[] txParts = parts[1].split("::");
                                for (int k = 2; k < txParts.length - 2; k+=2) {
                                    OUTPRT("     " + txParts[k + 1] + " mkii(s) from " + txParts[0] + " to " + txParts[k]);
                                }
                                OUTPRT("Total mkii sent: "+ txParts[1]);
                                peerNetwork.broadcast("TRANSACTION " + parts[1]);
                            } else {
                                OUTPRT("Invalid transaction: " + parts[1]);
                            }
                        }
                    } else if (parts[0].equalsIgnoreCase("PEER")){
                        boolean exists = false;
                        for (int k = 0; k < peers.size(); k++) {
                            if (peers.get(k).equals(parts[1] + ":" + parts[2])){
                                exists = true;
                            }
                        }

                        if (!exists){
                            try{
                                String peerAddr = parts[1].substring(0, parts[1].indexOf(":"));
                                int peerPort = Integer.parseInt(parts[1].substring(parts[1].indexOf(":") + 1));
                                peerNetwork.connectToPeer(peerAddr, peerPort);
                                peers.add(parts[1]);
                                PrintWriter out = new PrintWriter(peerFile);
                                for (int k = 0; k < peers.size(); k++) {
                                    out.println(peers.get(k));
                                }
                                out.close();
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    } else if (parts[0].equalsIgnoreCase("GET_PEER")){
                        Random random = new Random();
                        peerNetwork.peerThreads.get(i).outputThread.write("PEER " + peers.get(random.nextInt(peers.size())));
                    } else if (parts[0].equalsIgnoreCase("GET_BLOCK")){
                        try{
                            Block block = databaseMaster.getBlock(Integer.parseInt(parts[1]));
                            if (block != null){
                                OUTPRT("Sending block " + parts[1] + " to peer");
                                peerNetwork.peerThreads.get(i).outputThread.write("BLOCK " + block.getRawBlock());
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        int currentChainHeight = databaseMaster.getBlockchainLength();

        if(topBlock > currentChainHeight){
            catchupMode = true;
            OUTPRT("Current chain height: " + currentChainHeight);
            OUTPRT("Top block: " + topBlock);
            try{
                Thread.sleep(300);
            } catch (InterruptedException e){
                OUTPRT("Main thread sleep interrupted.");
                e.printStackTrace();
            }
            for (int i = currentChainHeight; i < topBlock; i++) {
                OUTPRT("Requesting block " + i + "...");
                peerNetwork.broadcast("GET_BLOCK " + i);
            }
        } else {
            if (catchupMode){
                OUTPRT("Caught up with network.");
            }
            catchupMode = false;
        }
    }
}
