package org.example;

import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServiceRequest implements Runnable {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final Socket socket;

    public ServiceRequest(Socket connection) {
        this.socket = connection;
    }

    //@Override
    public void run() {
        try (
                final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final byte[] buffer = new byte[limit];
            final var read = in.read(buffer);
            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                socket.close();
                return;
            }

            // читаем request line
            final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                socket.close();
                return;
            }

            Request request = new Request(requestLine[0], requestLine[1], requestLine[2]);
            System.out.println(request);
            for (Map.Entry<String, String> pair: request.getQueryParams().entrySet()) {
                System.out.println(pair.getKey() + "=" + pair.getValue());
            }
            System.out.println(request.getQueryParam("title"));
            System.out.println(request.getQueryParam("value"));
            System.out.println(request.getQueryParam("image"));
            if (!validPaths.contains(request.getPath())) {
                notFoundOut(out);
                socket.close();
                return;
            }

            final Path filePath = Path.of("public", request.getPath());
            final String mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (request.getPath().equals("/classic.html")) {
                classicCaseOut(out, filePath, mimeType);
                socket.close();
                return;
            }

            closeOut(out, filePath, mimeType);
            try {
                socket.close();
            } catch (IOException ioe) {
                System.out.println("Error closing client connection");
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    private void notFoundOut(BufferedOutputStream out) {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void classicCaseOut(BufferedOutputStream out, Path filePath, String mimeType) {
        try {
            final String template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeOut(BufferedOutputStream out, Path filePath, String mimeType) {
        try {
            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
