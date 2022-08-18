package com.cnettech;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Properties;

import com.cnettech.util.Common;
import com.cnettech.util.Log4j;

public class UdpServerApp extends Thread {
    
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    
    private static Properties pros = Common.getProperties();
    private String MON_APP_Port = "4001";

    public UdpServerApp() throws SocketException {
        MON_APP_Port = pros.getProperty("port.app");
        socket = new DatagramSocket(Integer.parseInt(MON_APP_Port));
    }

    @Override
    public void run() {
        Log4j.log.info("---- Program App START ----");
        
        running = true;

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received = new String(packet.getData(), 0, packet.getLength());

                // if (received.equals("end")) {
                // running = false;
                // continue;
                // }
                ProcessMsg processMsg = new ProcessMsg();
                processMsg.Msg(address.getHostAddress(),received);
                // socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

}
