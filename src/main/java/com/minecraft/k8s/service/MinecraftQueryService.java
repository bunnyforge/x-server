package com.minecraft.k8s.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minecraft Query 协议客户端
 * 用于获取服务器状态和在线玩家数
 */
@Slf4j
@Service
public class MinecraftQueryService {

    private static final int TIMEOUT_MS = 3000;
    private static final byte HANDSHAKE_TYPE = 9;
    private static final byte STAT_TYPE = 0;

    /**
     * 获取在线玩家数(带缓存)
     * 
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 在线玩家数,如果获取失败返回 null
     */
    @Cacheable(value = "serverMetrics", key = "#host + ':' + #port + ':players'")
    public Integer getOnlinePlayerCount(String host, int port) {

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            InetAddress address = InetAddress.getByName(host);

            // 1. 发送握手请求
            int sessionId = (int) (System.currentTimeMillis() & 0x0F0F0F0F);
            byte[] handshakeRequest = createHandshakeRequest(sessionId);
            DatagramPacket handshakePacket = new DatagramPacket(
                    handshakeRequest, handshakeRequest.length, address, port);
            socket.send(handshakePacket);

            // 2. 接收握手响应
            byte[] handshakeResponse = new byte[1024];
            DatagramPacket handshakeReceive = new DatagramPacket(handshakeResponse, handshakeResponse.length);
            socket.receive(handshakeReceive);

            // 3. 解析 challenge token
            String challengeToken = parseHandshakeResponse(handshakeResponse);
            if (challengeToken == null) {
                return null;
            }

            // 4. 发送状态请求
            byte[] statRequest = createStatRequest(sessionId, Integer.parseInt(challengeToken));
            DatagramPacket statPacket = new DatagramPacket(
                    statRequest, statRequest.length, address, port);
            socket.send(statPacket);

            // 5. 接收状态响应
            byte[] statResponse = new byte[4096];
            DatagramPacket statReceive = new DatagramPacket(statResponse, statResponse.length);
            socket.receive(statReceive);
            // 6. 解析在线玩家数
            Integer playerCount = parsePlayerCount(statResponse, statReceive.getLength());

            if (playerCount != null) {
                log.info("✅ Query successful - {}:{} has {} player(s) online", host, port, playerCount);
            } else {
                log.warn("❌ Query failed - Could not parse player count from {}:{}", host, port);
            }

            return playerCount;

        } catch (SocketTimeoutException e) {
            log.warn("❌ Query timeout for {}:{} (server may not have Query enabled)", host, port);
            return null;
        } catch (IOException e) {
            log.error("❌ Query I/O error for {}:{} - {}", host, port, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Unexpected error querying server: {}:{}", host, port, e);
            return null;
        }
    }

    /**
     * 创建握手请求包
     */
    private byte[] createHandshakeRequest(int sessionId) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0xFEFD); // Magic
        buffer.put(HANDSHAKE_TYPE); // Type
        buffer.putInt(sessionId); // Session ID
        return buffer.array();
    }

    /**
     * 解析握手响应,提取 challenge token
     */
    private String parseHandshakeResponse(byte[] response) {
        try {
            // 跳过前 5 个字节 (type + session id)
            int offset = 5;
            StringBuilder token = new StringBuilder();
            while (offset < response.length && response[offset] != 0) {
                token.append((char) response[offset]);
                offset++;
            }
            return token.toString();
        } catch (Exception e) {
            log.error("Failed to parse handshake response", e);
            return null;
        }
    }

    /**
     * 创建状态请求包
     */
    private byte[] createStatRequest(int sessionId, int challengeToken) {
        ByteBuffer buffer = ByteBuffer.allocate(15);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 0xFEFD); // Magic
        buffer.put(STAT_TYPE); // Type
        buffer.putInt(sessionId); // Session ID
        buffer.putInt(challengeToken); // Challenge token
        buffer.putInt(0); // Padding for full stat
        return buffer.array();
    }

    /**
     * 解析状态响应,提取在线玩家数
     */
    private Integer parsePlayerCount(byte[] response, int length) {
        try {
            String data = new String(response, 0, length, StandardCharsets.UTF_8);

            // 查找 "numplayers" 字段
            String numPlayersKey = "numplayers";
            int index = data.indexOf(numPlayersKey);
            if (index == -1) {
                log.warn("numplayers field not found in response");
                return null;
            }

            // 跳过 key 和 null 字节,读取值
            int valueStart = index + numPlayersKey.length() + 1;
            StringBuilder value = new StringBuilder();
            while (valueStart < data.length() && data.charAt(valueStart) != '\0') {
                value.append(data.charAt(valueStart));
                valueStart++;
            }

            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.error("Failed to parse player count", e);
            return null;
        }
    }
}
