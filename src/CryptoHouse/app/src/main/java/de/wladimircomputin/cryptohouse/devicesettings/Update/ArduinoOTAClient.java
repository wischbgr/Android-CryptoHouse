package de.wladimircomputin.cryptohouse.devicesettings.Update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Implements the sender side of the ArduinoOTA wire protocol (as used by espota.py):
 * a UDP invitation carrying the local TCP port/size/md5, followed by the device
 * connecting back over TCP to pull the firmware in acknowledged chunks.
 */
public class ArduinoOTAClient {

    public interface Callback {
        void onProgress(int progress);
        void onSuccess();
        void onError(String message);
    }

    private static final int OTA_PORT = 3232;
    private static final int OTA_CMD_FLASH = 0;
    private static final int INVITE_TRIES = 10;
    private static final int INVITE_TIMEOUT_MS = 3000;
    private static final int TCP_ACCEPT_TIMEOUT_MS = 10000;
    private static final int CHUNK_TIMEOUT_MS = 10000;
    private static final int CHUNK_SIZE = 1024;
    private static final int RESULT_TRIES = 10;

    private final String deviceIp;
    private final Callback callback;

    public ArduinoOTAClient(String deviceIp, Callback callback) {
        this.deviceIp = deviceIp;
        this.callback = callback;
    }

    // Blocking; call from a background thread.
    public void update(byte[] firmware) {
        try {
            String md5 = md5Hex(firmware);

            try (ServerSocket serverSocket = new ServerSocket(0);
                 DatagramSocket udpSocket = new DatagramSocket()) {

                serverSocket.setSoTimeout(TCP_ACCEPT_TIMEOUT_MS);
                udpSocket.setSoTimeout(INVITE_TIMEOUT_MS);

                String response = invite(udpSocket, serverSocket.getLocalPort(), firmware.length, md5);
                if (response == null) {
                    callback.onError("No response from device");
                    return;
                }
                if (response.startsWith("AUTH")) {
                    callback.onError("Device requires an OTA password, which is not supported yet");
                    return;
                }
                if (!response.equals("OK")) {
                    callback.onError("Unexpected response: " + response);
                    return;
                }

                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    callback.onError("Device did not connect");
                    return;
                }

                socket.setSoTimeout(CHUNK_TIMEOUT_MS);
                transfer(socket, firmware);
            }
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private String invite(DatagramSocket udpSocket, int localPort, int size, String md5) throws IOException {
        InetAddress addr = InetAddress.getByName(deviceIp);
        String message = OTA_CMD_FLASH + " " + localPort + " " + size + " " + md5 + "\n";
        byte[] msgBytes = message.getBytes(StandardCharsets.US_ASCII);

        for (int i = 0; i < INVITE_TRIES; i++) {
            udpSocket.send(new DatagramPacket(msgBytes, msgBytes.length, addr, OTA_PORT));
            byte[] buf = new byte[128];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            try {
                udpSocket.receive(resp);
                return new String(resp.getData(), 0, resp.getLength(), StandardCharsets.US_ASCII);
            } catch (SocketTimeoutException ignored) {
            }
        }
        return null;
    }

    private void transfer(Socket socket, byte[] firmware) throws IOException {
        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();
        byte[] ackBuf = new byte[32];

        int offset = 0;
        while (offset < firmware.length) {
            int len = Math.min(CHUNK_SIZE, firmware.length - offset);
            out.write(firmware, offset, len);
            out.flush();
            offset += len;

            if (in.read(ackBuf) < 0) {
                throw new IOException("Device closed the connection");
            }
            callback.onProgress((int) (1000L * offset / firmware.length));
        }

        String result = readResult(in, ackBuf);
        socket.close();

        if (result.contains("OK")) {
            callback.onSuccess();
        } else {
            callback.onError("Device reported an error: " + result);
        }
    }

    private String readResult(InputStream in, byte[] buf) throws IOException {
        for (int i = 0; i < RESULT_TRIES; i++) {
            try {
                int n = in.read(buf);
                if (n > 0) {
                    return new String(buf, 0, n, StandardCharsets.US_ASCII).trim();
                }
            } catch (SocketTimeoutException ignored) {
            }
        }
        return "";
    }

    private static String md5Hex(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
