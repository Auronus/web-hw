package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public void runServer() {
        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    threadPool.submit(new ServiceRequest(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
