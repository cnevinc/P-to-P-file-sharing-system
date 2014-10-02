/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//package peerprocess;
import java.lang.Math;
/**
 *
 * @author pingying
 */
public class peer {
    public int[] bitField = null;
    public int[] checked = null;
    public int id;
    public String host;
    public int port;
    public int haveall;
    
    public peer(int tid, String thost, int tport, int thave){
        id = tid;
        host = thost;
        port = tport;
        haveall = thave;
        
        bitField = new int[peerProcess.Pieces];
        checked = new int[peerProcess.Pieces];
        for (int i = 0; i<bitField.length; i++) {
            if (thave == 1){
                bitField[i] = 1;
                checked[i] = 1;
            }else{
                bitField[i] = 0;
                checked[i] = 0;
            }//if else
        }//for
    }// peer constructor

    public boolean have(int ind){
        if (bitField[ind] == 1){
            return true;
        }else{
            return false;
        }
    }// query if this peer has piece ind.
    
    public int haveHowMany(){
        int result = 0;
        for (int i = 0; i<bitField.length; i++){
            result += bitField[i];
        }
        return result;
    }
    
    synchronized public void set(int ind) {
        bitField[ind] = 1;
        if (gotAll() == true){
            haveall = 1;
        }
    }// set the indth bit to 1.
    
    public boolean gotAll(){
        if (haveall == 1){
            return true;
        }
        for (int i = 0; i<peerProcess.Pieces; i++) {
            if (bitField[i] == 0) {
                return false;
            }
        }//for
        
        return true;
    }// got all
    
    synchronized public void setBitField(String field){
        if (field.length() != bitField.length){
            System.out.println("The input bit field must have same length as number of pieces");
            return;
        }
        for (int i = 0; i<field.length(); i++){
            if (field.charAt(i) == '1'){
                bitField[i] = 1;
            }else{
                bitField[i] = 0;
            }
        }
    }// set Bit Field
    
    public String getBitField(){
        StringBuilder field = new StringBuilder();
        for (int i = 0; i<bitField.length; i++){
            field.append(bitField[i]);
        }
        return field.toString();
    }// get Bit Field
    
    public int interest(int otherid){
        int blocksize = peerProcess.Pieces/(peerProcess.Peers.size()-1)+1;
        int offset = blocksize * (peerProcess.SelfID%peerProcess.Prefix-2);
        int result = -1;
        for (int i = 0; i<peerProcess.Pieces; i++) {
            int ind = (i+offset)%peerProcess.Pieces;
            if (peerProcess.Peers.elementAt(otherid%peerProcess.Prefix-1).have(ind) == true && bitField[ind] == 0 && checked[ind] == 0) {
                checked[ind] = 1;
                return ind;
            }
        }
        return result;
    }// interest
    
    synchronized public void setAll(){
        haveall = 1;
        for (int i = 0; i<peerProcess.Pieces; i++) {
            bitField[i] = 1;
        }
    }//set all
}// peer class
