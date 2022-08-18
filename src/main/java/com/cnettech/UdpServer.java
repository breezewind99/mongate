package com.cnettech;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import com.cnettech.util.Log4j;

public class UdpServer extends Thread {

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];

    public UdpServer(String sPort) throws SocketException {
        Log4j.log.info("---- UDP Server START ----");
        System.out.printf("(Port : %s Waiting)\r\n", sPort);
        socket = new DatagramSocket(Integer.parseInt(sPort));
    }

    @Override
    public void run() {
        running = true;
        ProcessMsg processMsg = new ProcessMsg();
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received = new String(packet.getData(), 0, packet.getLength());
                processMsg.Msg(address.getHostAddress(), received);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }
}
