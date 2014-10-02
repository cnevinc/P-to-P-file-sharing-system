/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package peerprocess;
import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.lang.Exception;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author pingying
 */
public class worker extends Thread{
    public Socket workerSocket;
    public int OtherID;
    public boolean exited = false;
    //public String handshakeM;
    public worker(Socket temp){
        workerSocket = temp;
        OtherID = -1;
    }//constructor.
    public worker(Socket temp, int id){
        workerSocket = temp;
        OtherID = id;
    }//constructor.
    //  StringBuilder handshakeM = new StringBuilder();
    String handshakehead="CEN5501C2008SPRING";
    String handshakeM=handshakehead+"0000000000"+peerProcess.SelfID+"\n";
    DataOutputStream outToOther;
    BufferedReader inFromOther;
    InputStream pieceReader;
    String checkhsm;
    String checkhearder;
    String checkID;
    String unchokedM="1@pingying@lijunji@1@pingying@lijunji@-1\n";
    String chokedM="1@pingying@lijunji@0@pingying@lijunji@-1\n";
    String exitM="1@pingying@lijunji@8@pingying@lijunji@-1\n";
    String notexitM="1@pingying@lijunji@9@pingying@lijunji@-1\n";
    String chclearM = "1@pingying@lijunji@10@pingying@lijunji@-1\n";
    BufferedReader MinFromOther;
    String MsFromOther;
    String[] RevMessage;
    int msLen=0;
    int msType=1;
    String msPayload;
    int intrpid=-1;
    String haveM;
    String mybitfield;
    String piecemessage;
    
    @Override
    public void run(){
        try{
            inFromOther=new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));
            pieceReader = workerSocket.getInputStream();
            outToOther = new DataOutputStream(workerSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        /////////////TCP handshake start
        // send handshake message to other
        if(OtherID!=-1){
            try {
                outToOther.writeBytes(handshakeM);
            } catch (IOException ex) {
                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
            }
            try{  
                while(inFromOther.ready()==false){
                Thread.sleep(5);
                }
            }catch(Exception e){}
            try {
                checkhsm=inFromOther.readLine()+"";
            } catch (IOException ex) {
                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
            }
            checkhearder = checkhsm.substring(0,18);
            OtherID=Integer.parseInt(checkhsm.substring(28,32));
            //send ackhandshake message
        }else{
            try{  
                while(inFromOther.ready()==false){
                    Thread.sleep(5);
                }
            }catch(Exception e){}
        
            try {
                checkhsm=inFromOther.readLine()+"";
            } catch (IOException ex) {
                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
            }
            checkhearder = checkhsm.substring(0,18);
            OtherID=Integer.parseInt(checkhsm.substring(28,32));
            //send ackhandshake message
            if(checkhearder.equals(handshakehead)){//check if it is the right neighbour,
                try {
                    //if is unchoked,then send the unchock message
                    outToOther.writeBytes(handshakeM);
                } catch (IOException ex) {
                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                }                 
            }// if handshake
        }// if else on other id.
        
        /////////////TCP handshake ends

        // enters file transfer logic.
        try{
            while (exited == false && (peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).gotAll()==false || peerProcess.Peers.elementAt(OtherID%1000-1).gotAll()==false) ){
                // check for exit, not exit message.
                try{
                    outToOther.writeBytes(notexitM);
                    System.out.println("Peer " + peerProcess.SelfID + " sent not exit message to peer " +OtherID +".");
                }catch(Exception e){}
                try{
                    while(inFromOther.ready()==false){
//                        Thread.sleep(5);
                    }
                }catch(Exception e){}
                try {
                    MsFromOther=inFromOther.readLine()+"";
                } catch (IOException ex) {
                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                }
                RevMessage=MsFromOther.split("@pingying@lijunji@");
                msLen = Integer.parseInt(RevMessage[0]);
                msType = Integer.parseInt(RevMessage[1]);
                msPayload =RevMessage[2];
                if (msType == 8){
                    // received exit message.
                    System.out.println("Peer " + peerProcess.SelfID + " received exit message from peer " +OtherID +".");
                    exited = true;
                    break;
                }else if (msType != 9){
                    System.out.println("Expecting exit/not exit message, received message type " + msType+".");
                }else{
                    System.out.println("Peer " + peerProcess.SelfID + " received not exit message from peer " +OtherID +".");
                }
                
                // exchange bitField
                try{
                    mybitfield=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).getBitField();
                    haveM=mybitfield.length()+"@pingying@lijunji@"+4+"@pingying@lijunji@"+mybitfield+"\n";
                    outToOther.writeBytes(haveM);
                    System.out.println("Peer " + peerProcess.SelfID + " sent bitfield message to peer " +OtherID +".");
                }catch(Exception e){}
                try{  
                    while(inFromOther.ready()==false){
//                        Thread.sleep(5);
                    }
                }catch(Exception e){}
                try {
                    MsFromOther=inFromOther.readLine()+"";
                } catch (IOException ex) {
                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                }
                RevMessage=MsFromOther.split("@pingying@lijunji@");
                msLen = Integer.parseInt(RevMessage[0]);
                msType = Integer.parseInt(RevMessage[1]);
                msPayload =RevMessage[2];
                if (msType == 4){
                    // received bitfield from the other party.
                    System.out.println("Peer " + peerProcess.SelfID + " received bitField message from peer " +OtherID +".");
                    peerProcess.Peers.elementAt(OtherID%1000-1).setBitField(msPayload);
                }else if (msType == 8){
                    // received exit message.
                    System.out.println("Peer " + peerProcess.SelfID + " received exit message from peer " +OtherID +".");
                    exited = true;
                    break;
                }else{
                    System.out.println("Expecting bitField message, received message type " + msType+".");
                }

                //////////////////check choke unchoke
                if(peerProcess.choked[OtherID%1000-1]==false){
                    try {
                        //if is unchoked,then send the unchock message
                        outToOther.writeBytes(unchokedM);
                    } catch (IOException ex) {
                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                    }//try catch
                    // send bitfield to others if unchoked.
                    mybitfield=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).getBitField();
                    haveM=mybitfield.length()+"@pingying@lijunji@"+4+"@pingying@lijunji@"+mybitfield+"\n";
                    try {
                        //if is unchoked,then send the have message
                        outToOther.writeBytes(haveM);
                    } catch (IOException ex) {
                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    try{
                        while(inFromOther.ready()==false){
//                            Thread.sleep(5);
                        }
                    }catch(Exception e){}
                    try {
                        MsFromOther=inFromOther.readLine();
                    } catch (IOException ex) {
                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    RevMessage = MsFromOther.split("@pingying@lijunji@");
                    msLen = Integer.parseInt(RevMessage[0]);
                    msType = Integer.parseInt(RevMessage[1]);
                    msPayload =RevMessage[2];
                    if(msType==1){//if receive unchoke
                        // case 1: the other party is unchoked. And self is unchoked.
                        try{
                            peerProcess.logfile.write(logs.logmsg(3,peerProcess.SelfID,OtherID));
                        } catch (IOException ex) {
                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        try{  
                            while(inFromOther.ready()==false){
//                                Thread.sleep(5);
                            }
                        }catch(Exception e){} 
                        try {
                            MsFromOther=inFromOther.readLine();//read again
                        } catch (IOException ex) {
                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        RevMessage = MsFromOther.split("@pingying@lijunji@");
                        msLen = Integer.parseInt(RevMessage[0]);
                        msType = Integer.parseInt(RevMessage[1]);
                        msPayload =RevMessage[2];
                        if(msType==4){//receive have message
                            peerProcess.Peers.elementAt(OtherID%1000-1).setBitField(msPayload);
                            intrpid=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).interest(OtherID);
                            try {
                                peerProcess.logfile.write(logs.logmsg(5,peerProcess.SelfID,OtherID,intrpid));
                            } catch (IOException ex) {
                                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            if (intrpid==-1){// send no interested message
                                try {
                                    outToOther.writeBytes(1+"@pingying@lijunji@"+3+"@pingying@lijunji@"+"-1\n");
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                //judge receive is in or notin
                                try{  
                                    while(inFromOther.ready()==false){
//                                        Thread.sleep(5);
                                    }
                                }catch(Exception e){}
                                try {
                                    MsFromOther=inFromOther.readLine()+"";
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                RevMessage=MsFromOther.split("@pingying@lijunji@");
                                msLen = Integer.parseInt(RevMessage[0]);
                                msType = Integer.parseInt(RevMessage[1]);
                                msPayload =RevMessage[2];
                                if(msType==2){ //if receive interested\
                                    try {
                                        peerProcess.logfile.write(logs.logmsg(6,peerProcess.SelfID,OtherID));
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    byte[] piececontent = peerProcess.getPiece(Integer.parseInt(msPayload));
                                    piecemessage=msPayload+"@pingying@lijunji@"+7+"@pingying@lijunji@"+piececontent.length+"\n";
                                    try {// receive interest message ,sent piece message
                                        outToOther.writeBytes(piecemessage);
                                        // wait for channel clear signal.
                                        while(inFromOther.ready() == false){}
                                        // read channel clear signal.
                                        try{
                                            MsFromOther = inFromOther.readLine();
                                        }catch(Exception e){}
                                        RevMessage = MsFromOther.split("@pingying@lijunji@");
                                        msLen = Integer.parseInt(RevMessage[0]);
                                        msType = Integer.parseInt(RevMessage[1]);
                                        msPayload =RevMessage[2];
                                        if (msType == 10) {
                                            // received channel clear message. will send content message.
                                            outToOther.write(piececontent);
                                            // wait for channel clear message to enter next loop
                                            while (inFromOther.ready()==false){}
                                            // read channel clear message
                                            try{
                                                MsFromOther = inFromOther.readLine();
                                            }catch(Exception e){}
                                            RevMessage = MsFromOther.split("@pingying@lijunji@");
                                            msLen = Integer.parseInt(RevMessage[0]);
                                            msType = Integer.parseInt(RevMessage[1]);
                                            msPayload =RevMessage[2];
                                            if (msType != 10){
                                                System.out.println("Expecting channel clear message, received message type "+msType);
                                            }
                                            continue;
                                        }else{
                                            System.out.println("Expecting channel clear message, received message type "+msType);
                                        }
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    } 
                                }else if(msType==3){//if receive not interested
                                    try {
                                        //not interestes
                                        peerProcess.logfile.write(logs.logmsg(7,peerProcess.SelfID,OtherID));
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }// if else
                            }else{// send interested message
                                try {
                                    outToOther.writeBytes(1+"@pingying@lijunji@"+2+"@pingying@lijunji@"+intrpid+"\n");
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                //judge receive message is in or not in
                                try{  
                                    while(inFromOther.ready()==false){
//                                        Thread.sleep(5);
                                    }
                                }catch(Exception e){}
                                try {
                                    MsFromOther=inFromOther.readLine()+"";
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                RevMessage=MsFromOther.split("@pingying@lijunji@");
                                msLen = Integer.parseInt(RevMessage[0]);
                                msType = Integer.parseInt(RevMessage[1]);
                                msPayload =RevMessage[2];
                                if(msType==2){ //if receive interested\
                                    // prepare data to send.
                                    byte[] piececontent = peerProcess.getPiece(Integer.parseInt(msPayload));
                                    piecemessage=msPayload+"@pingying@lijunji@"+7+"@pingying@lijunji@"+piececontent.length+"\n";
                                    try {// receive interest message ,sent piece message
                                        try {
                                            peerProcess.logfile.write(logs.logmsg(6,peerProcess.SelfID,OtherID));
                                        } catch (IOException ex) {
                                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        outToOther.writeBytes(piecemessage);
                                        //////wait 7
                                        while(inFromOther.ready() == false){}
                                        try{
                                            MsFromOther = inFromOther.readLine();
                                        }catch(Exception e){}
                                        RevMessage = MsFromOther.split("@pingying@lijunji@");
                                        msLen = Integer.parseInt(RevMessage[0]);
                                        int indexFromOther = msLen;
                                        msType = Integer.parseInt(RevMessage[1]);
                                        msPayload =RevMessage[2];
                                        int loadsize = Integer.parseInt(msPayload);
                                        byte [] mspiececon = new byte[loadsize];
                                        if (msType == 7) {
                                            if (peerProcess.SelfID < OtherID ) {
                                                // seldid < other id, send channel clear message.
                                                outToOther.writeBytes(chclearM);
                                                // Then wait for data.
                                                while (pieceReader.available() <= 0){}
                                                // read piececontent.
                                                int totallen = 0;
                                                byte [] c = new byte[1];
                                                while (totallen < loadsize){
                                                    try{
                                                        pieceReader.read(c);
                                                    }catch(Exception e){}
                                                    mspiececon[totallen] = c[0];
                                                    totallen++;
                                                }
                                                outToOther.write(piececontent);
                                                try {
                                                    //piece
                                                    peerProcess.writePiece(indexFromOther, mspiececon);  
                                                    peerProcess.logfile.write(logs.logmsg(8,peerProcess.SelfID,OtherID,  intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).haveHowMany()));
                                                } catch (IOException ex) {
                                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                                }
                                                // wait for the other party to finish reading.
                                                while (inFromOther.ready()==false){}
                                                // read channel clear message
                                                try{
                                                    MsFromOther = inFromOther.readLine();
                                                }catch(Exception e){}
                                                RevMessage = MsFromOther.split("@pingying@lijunji@");
                                                msLen = Integer.parseInt(RevMessage[0]);
                                                msType = Integer.parseInt(RevMessage[1]);
                                                msPayload =RevMessage[2];
                                                if (msType != 10){
                                                    System.out.println("Expecting channel clear message, received message type "+msType);
                                                }
                                                continue;
                                            }else{
                                                // wait for channel clear signal from the other party.
                                                while(inFromOther.ready() == false){}
                                                // read channel clear signal.
                                                try{
                                                    MsFromOther = inFromOther.readLine();
                                                }catch(Exception e){}
                                                RevMessage = MsFromOther.split("@pingying@lijunji@");
                                                msLen = Integer.parseInt(RevMessage[0]);
                                                msType = Integer.parseInt(RevMessage[1]);
                                                msPayload =RevMessage[2];
                                                if (msType == 10) {
                                                    // got channel clear message.
                                                    outToOther.write(piececontent);
                                                    // wait for the other party finish reading, wait for piececontent from the other party.
                                                    while (pieceReader.available() <= 0){}
                                                    // read piececontent.
                                                    int totallen = 0;
                                                    byte [] c = new byte[1];
                                                    while (totallen < loadsize){
                                                        try{
                                                            pieceReader.read(c);
                                                        }catch(Exception e){}
                                                        mspiececon[totallen] = c[0];
                                                        totallen++;
                                                    }
                                                    outToOther.writeBytes(chclearM);
                                                    try {
                                                        //piece
                                                        peerProcess.writePiece(indexFromOther, mspiececon);  
                                                        peerProcess.logfile.write(logs.logmsg(8,peerProcess.SelfID,OtherID,  intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).haveHowMany()));
                                                    } catch (IOException ex) {
                                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                                    }
                                                    continue;
                                                }else{
                                                    System.out.println("Expecting channel clear message, received message type "+msType);
                                                }// message type 10
                                            }// self id < other id or not.
                                        }else{
                                            System.out.println("Expecting piecemessage, received message type "+msType);
                                        }// if message type == 7
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }else if(msType==3){//if receive not interested
                                    try {
                                        //not interestes
                                        peerProcess.logfile.write(logs.logmsg(7,peerProcess.SelfID,OtherID));
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    try {//wait for piece
                                        MsFromOther=inFromOther.readLine()+"";
                                    } catch (IOException ex) {
                                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    RevMessage=MsFromOther.split("@pingying@lijunji@");
                                    msLen = Integer.parseInt(RevMessage[0]);
                                    int indexFromOther = msLen;
                                    msType = Integer.parseInt(RevMessage[1]);
                                    msPayload =RevMessage[2];  
                                    int loadsize = Integer.parseInt(msPayload);
                                    byte[] mspiececon = new byte[loadsize];
                                    if (msType == 7){
                                        outToOther.writeBytes(chclearM);
                                        // wait for content data to come.
                                        while (pieceReader.available()<=0){}
                                        int totallen = 0;
                                        byte [] c = new byte[1];
                                        while (totallen < loadsize){
                                            try{
                                                pieceReader.read(c);
                                            }catch(Exception e){}
                                            mspiececon[totallen] = c[0];
                                            totallen++;
                                        }
                                        outToOther.writeBytes(chclearM);
                                        try {
                                            //piece
                                            peerProcess.writePiece(indexFromOther, mspiececon);  
                                            peerProcess.logfile.write(logs.logmsg(8,peerProcess.SelfID,OtherID,  intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).haveHowMany()));
                                        } catch (IOException ex) {
                                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                        continue;
                                    }else{
                                        System.out.println("Expecting piece data, received message type" + msType + ".");
                                    }// if else
                                }// if the other party is interested in me or not.

                            }// if I am interested in the other party or not.
                        }//if received have message.
                    }else if(msType==0){
                        //if receive choke 
                        try{
                            peerProcess.logfile.write(logs.logmsg(4,peerProcess.SelfID,OtherID));
                        } catch (IOException ex) {
                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        try{  
                            while(inFromOther.ready()==false){
//                                Thread.sleep(5);
                            }
                        }catch(Exception e){} 
                        try {
                            MsFromOther=inFromOther.readLine();//read again
                        } catch (IOException ex) {
                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        RevMessage = MsFromOther.split("@pingying@lijunji@");
                        msLen = Integer.parseInt(RevMessage[0]);
                        msType = Integer.parseInt(RevMessage[1]);
                        msPayload =RevMessage[2];
                        if(msType==2){ //if receive interested\
                            byte[] piececontent = peerProcess.getPiece(Integer.parseInt(msPayload));
                            piecemessage=msPayload+"@pingying@lijunji@"+7+"@pingying@lijunji@"+piececontent.length+"\n";
                            try {// receive interest message ,sent piece message
                                outToOther.writeBytes(piecemessage);
                                // wait for channel clear signal.
                                while(inFromOther.ready() == false){}
                                // read channel clear signal.
                                try{
                                    MsFromOther = inFromOther.readLine();
                                }catch(Exception e){}
                                RevMessage = MsFromOther.split("@pingying@lijunji@");
                                msLen = Integer.parseInt(RevMessage[0]);
                                msType = Integer.parseInt(RevMessage[1]);
                                msPayload =RevMessage[2];
                                if (msType == 10) {
                                    // received channel clear message. will send content message.
                                    outToOther.write(piececontent);
                                    // wait for channel clear message to enter next loop
                                    while (inFromOther.ready() == false){}
                                    // read channel clear message
                                    try{
                                        MsFromOther = inFromOther.readLine();
                                    }catch(Exception e){}
                                    RevMessage = MsFromOther.split("@pingying@lijunji@");
                                    msLen = Integer.parseInt(RevMessage[0]);
                                    msType = Integer.parseInt(RevMessage[1]);
                                    msPayload =RevMessage[2];
                                    if (msType != 10){
                                        System.out.println("Expecting channel clear message, received message type "+msType);
                                    }
                                    continue;
                                }else{
                                    System.out.println("Expecting channel clear message, received message type "+msType);
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            try {
                                peerProcess.logfile.write(logs.logmsg(6,peerProcess.SelfID,OtherID));
                            } catch (IOException ex) {
                                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }else if(msType==3){//if receive not interested
                            try {
                                //not interestes
                                peerProcess.logfile.write(logs.logmsg(7,peerProcess.SelfID,OtherID));
                            } catch (IOException ex) {
                                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }// if received interested or not.
                    }else {continue;}// if received choked/unchoked.
                }else{// send chock message
                    // the other party is choked,send chocked message
                    try {
                    outToOther.writeBytes(chokedM);
                    } catch (IOException ex) {
                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    // wait for choke/unchoke message from the other party.
                    try{  
                        while(inFromOther.ready()==false){
//                            Thread.sleep(5);
                        }
                    }catch(Exception e){}
                    try {
                        MsFromOther=inFromOther.readLine()+"";
                    } catch (IOException ex) {
                        Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    RevMessage=MsFromOther.split("@pingying@lijunji@");
                    msLen = Integer.parseInt(RevMessage[0]);
                    msType = Integer.parseInt(RevMessage[1]);
                    msPayload =RevMessage[2];
                    if (msType == 0){
                        // case 3:
                        // received choked message.
                        // Both parties are choked. Do nothing.
                        try{
                            peerProcess.logfile.write(logs.logmsg(4, peerProcess.SelfID, OtherID));
                        }catch(Exception e){}
//                        Thread.sleep(5);
                        continue;
                    }else if (msType == 1){
                        // case 4:
                        // received unchoked message.
                        // wait for have message here.
                        try{
                            peerProcess.logfile.write(logs.logmsg(3, peerProcess.SelfID, OtherID));
                        }catch(Exception e){}
                        try{  
                            while(inFromOther.ready()==false){
//                                Thread.sleep(5);
                            }
                        }catch(Exception e){}
                        try {
                            MsFromOther=inFromOther.readLine()+"";
                        } catch (IOException ex) {
                            Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        RevMessage=MsFromOther.split("@pingying@lijunji@");
                        msLen = Integer.parseInt(RevMessage[0]);
                        msType = Integer.parseInt(RevMessage[1]);
                        msPayload =RevMessage[2];
                        if (msType == 4){
                            // received have message
                            peerProcess.Peers.elementAt(OtherID%1000-1).setBitField(msPayload);
                            intrpid=peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).interest(OtherID);
                            try{
                                peerProcess.logfile.write(logs.logmsg(5, peerProcess.SelfID, OtherID, intrpid));
                            }catch(Exception e){}
                            if (intrpid==-1){// send not interested message
                                try {
                                    outToOther.writeBytes(1+"@pingying@lijunji@"+3+"@pingying@lijunji@"+"-1\n");
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                } 
                            }else{// send interested message
                                try {
                                    outToOther.writeBytes(1+"@pingying@lijunji@"+2+"@pingying@lijunji@"+intrpid+"\n"); 
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                } 
                                // sent interested message, wait for data here.
                            try{  
                                while(inFromOther.ready()==false){
//                                    Thread.sleep(5);
                                }
                            }catch(Exception e){}
                            try {
                                MsFromOther=inFromOther.readLine()+"";
                            } catch (IOException ex) {
                                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            RevMessage=MsFromOther.split("@pingying@lijunji@");
                            msLen = Integer.parseInt(RevMessage[0]);
                            int indexFromOther = msLen;
                            msType = Integer.parseInt(RevMessage[1]);
                            msPayload =RevMessage[2];
                            int loadsize = Integer.parseInt(msPayload);
                            byte[] mspiececon = new byte[loadsize];
                            if (msType == 7){// received piecemessage. Send clear message to the other party.
                                outToOther.writeBytes(chclearM);
                                // wait for content data to come.
                                while (pieceReader.available()<=0){}
                                int totallen = 0;
                                byte [] c = new byte[1];
                                while (totallen < loadsize){
                                    try{
                                        pieceReader.read(c);
//                                            inFromOther.r.read(c);
                                    }catch(Exception e){}
                                    mspiececon[totallen] = c[0];
                                    totallen++;
                                }
                                outToOther.writeBytes(chclearM);
                                try {
                                    //piece
                                    peerProcess.writePiece(indexFromOther, mspiececon);  
                                    peerProcess.logfile.write(logs.logmsg(8,peerProcess.SelfID,OtherID,  intrpid, peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).haveHowMany()));
                                } catch (IOException ex) {
                                    Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                continue;
                            }else{
                                System.out.println("Expecting piece data, received message type" + msType + ".");
                            }
                            }// if interest or not
                            
                        }else{
                            System.out.println("Expecting have message, received message type" + msType + ".");
                        }// if have message
                    }else{
                        System.out.println("Expecting choked/unchoked message, received message type" + msType + ".");
                    }//if choked/unchoked.
                }// end of if else
            }// endwhile
        } catch(Exception ite){}

        // send exit message to the other party.
        if (exited == false) {
            // exited = false, means this peer is the party who initiated the 
            // exit process.
            try {
                outToOther.writeBytes(exitM);
                System.out.println("Peer " + peerProcess.SelfID + " sent exit message to peer " +OtherID +".");
                try{
                    Thread.sleep(500);
                }catch(Exception e){}
            } catch (IOException ex) {
                Logger.getLogger(worker.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        
//        try{
//            workerSocket.close();
//        }catch(Exception e){}
        System.out.println("The worker thread between peer"+peerProcess.SelfID+"and peer "+OtherID+" will exist now.\n");
        peerProcess.Peers.elementAt(OtherID%1000-1).setAll();
        peerProcess.Peers.elementAt(peerProcess.SelfID%1000-1).setAll();
    }//run

}// worker class
    

