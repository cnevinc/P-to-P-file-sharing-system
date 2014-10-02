
//package peerprocess;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.lang.Math.*;
import java.net.*;
import java.lang.Thread;
import java.util.Random;
// import java.util.concurrent;
/**
 * @author Lijun Ji, Ping Ying
 */
public class peerProcess {
    // The following are the common configuration fields.
    public static int NumberOfPreferredNeighbors;
    public static int UnchokingInterval;
    public static int OptimisticUnchokingInterval;
    public static String FileName;
    public static int FileSize;
    public static int PieceSize;
    public static int Pieces;
    
    // The following are the PeerInfo configuration fields.
    public static Vector<peer> Peers = new Vector<>();
    public static int SelfID;
    public static int Prefix;
    public static byte[] data;
    public static boolean[] choked;
    public static FileWriter logfile = null;
    public static Vector<worker> Workers = new Vector<>();
    public static Vector<Thread> WorkTrd = new Vector<>();
    // From here, 2013.3.5
    public static int[] Speed;
    public static int[] PreferredNeighbour;
    public static int OptimisticNeighbour;
    
    public static void main(String[] args) {
        //Initialization.
        if (args.length == 0) {
            System.out.println("Please specify an ID for this peer.");
            return;
        }//if
        SelfID = Integer.parseInt(args[0]);
        Prefix = SelfID - SelfID%1000;
        try {
            System.out.print("Peer "+SelfID+ " is initializing...");
            if (init() == false) {
                System.out.println("Initialization failed, please check configure files.");
                return;
            }// if
        }catch(Exception e){
        }
        System.out.print("Done.\n");

        // Establish TCP connections here.
        // First, establish a server socket to listen if it is not the last one 
        // in the peer list.
        ServerSocket listenSocket = null;
        if (SelfID != Peers.lastElement().id) {
            try{
                System.out.print("Peer " + SelfID+ " is openning listening socket...");
                listenSocket = new ServerSocket(Peers.elementAt(SelfID%1000-1).port);
            }catch(Exception e){
                // if establish 
                System.out.println("Peer " + SelfID+ " can't open listening socket.");
                return;
            }
            System.out.println("Done.\n");
        }// if
        
        // Second, establish connection to all previous peers.
        Socket toPrevious;
        Date cur = null;
        for (int i = 0; i<SelfID%1000-1; i++){
            try{
                logfile.write(logs.logmsg(0, SelfID, Peers.elementAt(i).id));
                System.out.print("Peer " + SelfID+ " is making TCP connection to Peer " + Peers.elementAt(i).id + "...");
                toPrevious = new Socket(Peers.elementAt(i).host, Peers.elementAt(i).port);
                Workers.add(new worker(toPrevious, Peers.elementAt(i).id));
                Thread trd = new Thread(Workers.lastElement());
                trd.setPriority(10);
                WorkTrd.add(trd);
//                trd.start();
            }catch(Exception e){
                System.out.print("Peer " + SelfID+ " can't make TCP connection to Peer " + Peers.elementAt(i).id + "...");
                for (int j = 0; j<Workers.size(); j++) {
                    try{
                        Workers.elementAt(j).workerSocket.close();
                    }catch(Exception e1){}
                }
                return;
            }
            System.out.print("Done.\n");
        }// for
        
        //Third, accept connections from later peers.
        if (listenSocket != null){
            Socket toLater;
            System.out.println("Peer " + SelfID+ " is listenning for later peers...");
            int laterPeers = Peers.lastElement().id - SelfID;
    
            long prev = System.currentTimeMillis();
            long curr = prev;
            try{
//                listenSocket.setSoTimeout(10000);
            }catch(Exception e){}
            while (laterPeers > 0) {
                prev = curr;
                curr = System.currentTimeMillis();
                try{
                    toLater = listenSocket.accept();
                    Workers.add(new worker(toLater));
                    Thread trd = new Thread(Workers.lastElement());
                    trd.setPriority(10);
                    WorkTrd.add(trd);
//                    trd.start();
                }catch(Exception e){
                    System.out.print("Peer " + SelfID+ " can't receive TCP requests from later peers.\n");
                    return;
                }
                laterPeers--;
            }// while
            if (laterPeers > 0){
                //Failed to establish TCP connection with later peers.
                for (int j = 0; j<Workers.size(); j++) {
                    try{
                        Workers.elementAt(j).workerSocket.close();
                    }catch(Exception e1){}
                }
                return;
            }
            System.out.println("Peer " + SelfID+ " has established TCP connections with all peers.");
            try{
                listenSocket.close();
            }catch(Exception e){
                return;
            }//try catch
        }//if

        // established all TCP connections, start threads now.
        for (int i = 0; i<WorkTrd.size(); i++) {
            WorkTrd.elementAt(i).start();
        }
        long prevPref = System.currentTimeMillis();
        long prevOpti = prevPref;
        long curtime;
        // Enter while loop.
        while (allShared() == false) {
            // Get system time.
            curtime = System.currentTimeMillis();
            if (curtime-prevPref > UnchokingInterval * 1000) {
                // Time to reselect prefferred neighbours.
                PreferredNeighbour = selectPreferred();
                prevPref = curtime;
            }
            if (curtime-prevOpti > peerProcess.OptimisticUnchokingInterval * 1000) {
                // Time to reselect optimistic neighbour.
                OptimisticNeighbour = selectOptimistic();
                prevOpti = curtime;
            }
            try{
                Thread.sleep(100);
            }catch(Exception e){
                
            }
        }//while
        
        // all peers have all the pieces.
        // store the file in disk, then exit.
        ////////////////////////////////////////////////////////////////////////
        //// Important, store file in the disk here.
        ///////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        if (SelfID != Peers.elementAt(0).id) {
            StringBuilder filenm = new StringBuilder();
            filenm.append("./peer_");
            filenm.append(SelfID);
            filenm.append("/");
            filenm.append(FileName);
            try {
                FileOutputStream out = new FileOutputStream(filenm.toString());
                out.write(data);
                out.close();
            }catch(Exception e){
            }
        }//if
        
        // Let the worker threads exist.
        for (int i = 0; i<WorkTrd.size(); i++){
            try{
                WorkTrd.elementAt(i).join();
            }catch(Exception e){}
        }//for
        
        // all worker threads are terminated. Terminate self.
        try{
            logfile.close();
        }catch(Exception e){
            
        }
        
        for (int j = 0; j<Workers.size(); j++) {
            try{
                Workers.elementAt(j).workerSocket.close();
            }catch(Exception e1){}
        }
        
        System.out.println("Peer " + SelfID +" is exiting now.");
    }// main function
    
    public static boolean init() throws Exception {
        BufferedReader comcfg = null;
        BufferedReader percfg = null;
        String ln = null;
        String[] spstr = null;
        boolean success = true;
        try {
            // First, read the common config file.
            comcfg = new BufferedReader(new FileReader(new File("Common.cfg")));
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            NumberOfPreferredNeighbors = Integer.parseInt(spstr[1]);
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            UnchokingInterval = Integer.parseInt(spstr[1]);
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            OptimisticUnchokingInterval = Integer.parseInt(spstr[1]);
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            FileName = spstr[1];
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            FileSize = Integer.parseInt(spstr[1]);
            ln = comcfg.readLine();
            spstr = ln.split(" ");
            PieceSize = Integer.parseInt(spstr[1]);
            Pieces = (int)(Math.ceil((double)FileSize/PieceSize));
            comcfg.close();

            // Second, read the peer info config file. And generate peers.
            percfg = new BufferedReader(new FileReader(new File("PeerInfo.cfg")));
            int id;
            String host;
            int port;
            int have;
            while ((ln = percfg.readLine())!=null) {
                spstr = ln.split(" ");
                if (spstr[0].charAt(0) != '%') {
                    id = Integer.parseInt(spstr[0]);
                    host = spstr[1];
                    port = Integer.parseInt(spstr[2]);
                    have = Integer.parseInt(spstr[3]);
                    Peers.add(new peer(id, host, port, have));
                    if (have == 0){
                        String dirName = "peer_"+id;
                        File dirFile = new File(dirName);
                        if (dirFile.exists() == false){
                            if (dirFile.mkdir() == false){
                                System.out.println("Making directory failed for peer "+id);
                                return false;
                            }
                        }
                    }// have == 0, create directory.
                }
            }//while
            percfg.close();
            
            // Third, get a block of memory for the data.
            // if the peer has the data, read it from the file.
            choked = new boolean[Peers.size()];
            for (int i = 0; i<Peers.size(); i++) {
                choked[i] = true;
            }//for
            data = new byte[FileSize];
            StringBuilder filenm = new StringBuilder();
            int ind = SelfID%1000-1;
            if (Peers.elementAt(ind).haveall == 0) {
                for (int i = 0; i<FileSize; i++) {
                    data[i] = 0;
                }//for
            }else{
                filenm.append("./peer_");
                filenm.append(SelfID);
                filenm.append("/");
                filenm.append(FileName);
                InputStream in = null;
                int byteread;
                try {
                    in = new FileInputStream(filenm.toString());
                    if ((byteread = in.read(data, 0, FileSize)) != FileSize) {
                        System.out.println("Something is wrong with the data file.");
                        throw(new Exception("File size is wrong."));
                    }
                }catch(Exception ine){
                    data = null;
                }finally{
                    if (in != null) {
                        in.close();
                    }
                }// try catch finally.
            }// if else
            
            //Fourth, open the log file.
            filenm.delete(0, filenm.length());
            filenm.append("log_peer_");
            filenm.append(SelfID);
            filenm.append(".log");
            logfile = new FileWriter(filenm.toString(), false);
            
            //Fifth, initialize neighbour list.
            Speed = new int[Peers.size()];
            OptimisticNeighbour = -1;
            PreferredNeighbour = selectPreferred();
            OptimisticNeighbour = selectOptimistic();
        }catch(Exception e) {
            success = false;
            if (logfile != null){
                try{
                    logfile.close();
                }catch(Exception e1){
                }
            }// if, close percfg.
        }finally{
            if (comcfg != null){
                try{
                    comcfg.close();
                }catch(Exception e){
                }
            }// if, close comcfg.
            if (percfg != null){
                try{
                    percfg.close();
                }catch(Exception e){
                }
            }// if, close percfg.
        }// try catch finally.
        return success;
    }// init
    
    private static boolean allShared(){
        for (int i = 0; i<Peers.size(); i++){
            if (Peers.elementAt(i).gotAll() == false){
                return false;
            }
        }//for
        return true;
    }// all shared
    
    public static int[] selectPreferred(){
        Vector<Integer> candidates = new Vector();
        for (int i = 0; i<Peers.size(); i++){
            candidates.add(i);
        }// every peer is a candidate.
        candidates.remove(SelfID%1000-1);   // self can't be neighbour.
        if (OptimisticNeighbour > 0) {
            for (int i=0; i<candidates.size(); i++){
                if (OptimisticNeighbour%1000-1 == candidates.elementAt(i)) {
                    candidates.remove(i);
                    break;
                }
            }
        }// if optimistic neighbour is initialized, it can't be preferred neighbour.
        int m = 0;
        while (m < candidates.size()) {
            if (Peers.elementAt(candidates.elementAt(m)).gotAll() == true) {
                candidates.remove(m);
            }else{
                m++;
            }
        }// if the other party already have all the pieces, we don't bother select them as preferred neighbour.
        
        int [] list;
        if (candidates.size() ==0){
            list = null;
        }else if (candidates.size() <= NumberOfPreferredNeighbors) {
            list = new int[candidates.size()];
            for (int i = 0; i<candidates.size(); i++){
                list[i] = candidates.elementAt(i)+1+Prefix;
            }
        } else {
            // there are more neighbours than we can serve.
            list = new int[NumberOfPreferredNeighbors];
            if (Peers.elementAt(SelfID%Prefix-1).gotAll() == true){
                // self has got all. select preferred neighbours randomly.
                Random rnd = new Random();
                rnd.setSeed(System.currentTimeMillis());
                for (int i = 0; i<NumberOfPreferredNeighbors; i++){
                    int ind = Math.abs(rnd.nextInt())%candidates.size();
                    list[i] = candidates.elementAt(ind)+1+Prefix;
                    candidates.remove(ind);
                }
            }else{
                // determine preferred neighbour according to speed.
                // first, get speed of candidates.
                int[] candSpeed = new int[candidates.size()];
                for (int i = 0; i<candidates.size(); i++){
                    candSpeed[i] = Speed[candidates.elementAt(i)];
                }
                // second, sort candidates according to their speed.
                int key, candkey;
                for (int i = 1; i<candidates.size(); i++){
                    int j = i-1;
                    key = candSpeed[i];
                    candkey = candidates.elementAt(i);
                    while (j >=0 && key > candSpeed[j]) {
                        candSpeed[j+1] = candSpeed[j];
                        candSpeed[j] = key;
                        candidates.set(j+1, candidates.elementAt(j));
                        candidates.set(j, candkey);
                        j--;
                    }
                }//sort the candidates according to their speed.
                
                // third, select top candidates as preferred neighbours.
                for (int i = 0; i<NumberOfPreferredNeighbors; i++){
                    list[i] = candidates.elementAt(i)+1+Prefix;
                }
            }
        }// do the selection.
        
        // After selection, set choked data structure.
        for (int i = 0; i<choked.length; i++) {
            choked[i] = true;
        }
        if (list != null){
            for (int i = 0; i<list.length; i++) {
                choked[list[i]%Prefix-1] = false;
            }
        }
        if (OptimisticNeighbour != -1){
            choked[OptimisticNeighbour%Prefix-1] = false;
        }
        // reset speed to 0.
        for (int i = 0; i<Speed.length; i++){
            Speed[i] = 0;
        }
        
        // update log file.
        try{
            logfile.write(logs.logmsg(1, SelfID, list));
        }catch(Exception e){
        }
        return list;
    }// select preferred neighbours.
    
    public static int selectOptimistic(){
        Vector<Integer> candidates = new Vector();
        for (int i = 0; i<Peers.size(); i++){
            candidates.add(i);
        }// every peer is a candidate.
        candidates.remove(SelfID%1000-1);   // self can't be neighbour.
        if (PreferredNeighbour != null) {
            for (int i = 0; i<PreferredNeighbour.length; i++) {
                for (int j = 0; j<candidates.size(); j++) {
                    if (PreferredNeighbour[i]%1000-1 == candidates.elementAt(j)) {
                        candidates.remove(j);
                        break;
                    }
                }
            }
        }// Preferred neighbour can't be optimistic neighbour.
        int m = 0;
        while (m < candidates.size()) {
            if (Peers.elementAt(candidates.elementAt(m)).gotAll() == true) {
                candidates.remove(m);
            }else{
                m++;
            }
        }// if the other party already have all the pieces, we don't bother select them as preferred neighbour.
        
        int list;
        if (candidates.size() ==0){
            list = -1;
        }else {
            Random rnd = new Random();
            rnd.setSeed(System.currentTimeMillis());
            int ind = Math.abs(rnd.nextInt())%candidates.size();
            list = candidates.elementAt(ind)+1+Prefix;
        }// do the selection.
        
        // After selection, set choked data structure.
        for (int i = 0; i<choked.length; i++) {
            choked[i] = true;
        }
        if (PreferredNeighbour != null){
            for (int i = 0; i<PreferredNeighbour.length; i++) {
                choked[PreferredNeighbour[i]%Prefix-1] = false;
            }
        }
        if (list != -1){
            choked[list%Prefix-1] = false;
        }
        
        // update log file.
        try{
            logfile.write(logs.logmsg(2, SelfID, list));
        }catch(Exception e){
            
        }
        return list;
    }// select optimistic neighbour.
    
    public static byte[] getPiece(int index){
        int size;
        if (index != Pieces-1){
            size = PieceSize;
        }else{
            size = FileSize - (Pieces-1)*PieceSize;
        }
        int start = index * PieceSize;
        byte [] origdata = new byte[size];
        System.arraycopy(data, start, origdata, 0, size);
        return origdata;
    }// get piece
    
    synchronized public static void writePiece(int index, byte[] origdata){
        // set bitfield
        Peers.elementAt(SelfID%Prefix-1).set(index);
        
        // data download completed, update log.
        if (Peers.elementAt(SelfID%Prefix-1).gotAll() == true) {
            try{
                logfile.write(logs.logmsg(9, SelfID));
            }catch(Exception e){
            }
        }
        
        // update data
        int size = origdata.length;
        int start = index * PieceSize;
        System.arraycopy(origdata, 0, data, start, size);
    }// write piece
}// peerProcess class
