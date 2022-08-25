package com.cnettech;

import java.net.SocketException;
import java.util.Properties;

import com.cnettech.util.Common;
import com.cnettech.util.Log4j;

public class Mongate {
    public static void main(String[] args) {
        Log4j.log.info("---- Program START ----");
        try {
            final Properties pros = Common.getProperties();
            Log4j.log.info("---- Program START ----");
            
            // Record 프로그램 UDP
            //MON_APP_Port = pros.getProperty("port.rec");
            Log4j.log.info("Record UDP Server Start ");
            UdpServer serverRec = new UdpServer(pros.getProperty("port.rec"));
            serverRec.start();
            
            // Check 프로그램 UDP
            Log4j.log.info("Application UDP Server Start ");
            UdpServer serverApp = new UdpServer(pros.getProperty("port.app"));
            serverApp.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

}
