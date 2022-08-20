import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;


public class ServerTest {

    private Server server;
    private Thread serverThread;

    @Before
    public void setUp() throws InterruptedException {
        server = new Server();
        serverThread = new Thread(() -> {
            server.run();
        });
        serverThread.start();
        Thread.sleep(5000);
    }

    @Test
    public void testNumClientsAndPort() throws InterruptedException {
        int maxClients;
        int portNumber;
        maxClients = server.getMaxClients();
        portNumber = server.getPortNumber();
        Assert.assertEquals(5, maxClients);
        Assert.assertEquals(4000, portNumber);
    }

    @Test
    public void testAcceptsTerminate() throws IOException, InterruptedException {
        Socket clientSocket = new Socket(InetAddress.getLocalHost(), 4000);
        OutputStream output = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("terminate");
        Thread.sleep(30000); //waits for 30 seconds for server to shut down
        Assert.assertTrue(server.getServerSocket().isClosed());
        Assert.assertTrue(server.getThreadPool().isTerminated());
    }

    @Test
    public void testAcceptsNineDigits() throws IOException, InterruptedException {
        Socket clientSocket = new Socket(InetAddress.getLocalHost(), 4000);
        OutputStream output = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("012345678");
        Thread.sleep(5000);
        Assert.assertFalse(clientSocket.isClosed());
    }


    @Test
    public void testServerReceivedARandomString() throws IOException, InterruptedException {
        Assert.assertEquals(4, server.getServerAccess().availablePermits());
        Socket clientSocket = new Socket(InetAddress.getLocalHost(), 4000);
        OutputStream output = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);
        writer.println("test");
        Thread.sleep(5000);
        Assert.assertEquals(4, server.getServerAccess().availablePermits());
    }

    @Test
    public void testThroughput() throws IOException {
        Socket clientSocket = new Socket(InetAddress.getLocalHost(), 4000);
        OutputStream output = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        Socket clientSocket2 = new Socket(InetAddress.getLocalHost(), 4000);
        OutputStream output2 = clientSocket2.getOutputStream();
        PrintWriter writer2 = new PrintWriter(output2, true);
        Date startTime = new Date();
        for (int i = 0; i < 2_000_000; i++) {
            writer.println(String.format("%09d", i));
            writer2.println(String.format("%09d", i));
        }
        Date endTime = new Date();
        writer.println("terminate");
        System.out.println("Milliseconds: " + (double)((endTime.getTime() - startTime.getTime())));
    }

}

