package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CapeShare {
    private static final int PORT = 41982;
    private static final String GROUP = "239.255.41.1";
    private static final Map<UUID, String> REMOTE = new ConcurrentHashMap<>();
    private static final Set<UUID> TIDAL = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> POLLED = new ConcurrentHashMap<>();
    private static final AtomicBoolean LISTENING = new AtomicBoolean();
    private static final java.util.concurrent.atomic.AtomicInteger HTTP_INFLIGHT = new java.util.concurrent.atomic.AtomicInteger();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static long lastLan;
    private static long lastHttp;
    private static long lastPoll;

    private CapeShare() {}

    public static void listen() {
        if (!LISTENING.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual().name("tidal-cape-udp").start(CapeShare::receiveLoop);
    }

    public static boolean isTidal(UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && uuid != null && uuid.equals(minecraft.getUser().getProfileId())) {
            return true;
        }
        return uuid != null && TIDAL.contains(uuid);
    }

    public static void publish(String capeId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        UUID uuid = minecraft.getUser().getProfileId();
        if (capeId == null) {
            capeId = "";
        }
        TIDAL.add(uuid);
        REMOTE.put(uuid, capeId);
        sendUdp(uuid, capeId);
        sendPayload(minecraft, uuid, capeId);
        String published = capeId;
        Thread.ofVirtual().name("tidal-cape-http").start(() -> sendHttp(uuid, published));
    }

    public static void tick(Minecraft minecraft) {
        listen();
        if (minecraft.player == null) {
            return;
        }
        String id = TidalMods.equippedCape == null ? "" : TidalMods.equippedCape;
        if (id.startsWith("ms:")) {
            id = "";
        }
        UUID uuid = minecraft.getUser().getProfileId();
        TIDAL.add(uuid);
        long now = System.nanoTime();
        if (now - lastLan >= 3_000_000_000L) {
            lastLan = now;
            sendUdp(uuid, id);
            sendPayload(minecraft, uuid, id);
        }
        if (now - lastHttp >= 15_000_000_000L) {
            lastHttp = now;
            String published = id;
            Thread.ofVirtual().name("tidal-cape-http").start(() -> sendHttp(uuid, published));
        }
        if (now - lastPoll >= 10_000_000_000L) {
            lastPoll = now;
            pollOthers(minecraft);
        }
    }

    public static ResourceLocation location(UUID uuid) {
        String id = REMOTE.get(uuid);
        if (id == null || id.isBlank() || id.startsWith("ms:")) {
            return null;
        }
        return Capes.locationById(id);
    }

    public static void receive(ResourceLocation id) {
        if (id == null || !"tidal-builtin".equals(id.getNamespace())) {
            return;
        }
        String path = id.getPath();
        if (!path.startsWith("cape/")) {
            return;
        }
        String[] parts = path.split("/");
        if (parts.length < 3) {
            return;
        }
        store(parts[2], parts[1]);
    }

    private static void pollOthers(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }
        UUID self = minecraft.getUser().getProfileId();
        long now = System.currentTimeMillis();
        int started = 0;
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            if (started >= 4) {
                break;
            }
            UUID uuid = info.getProfile().getId();
            if (uuid.equals(self)) {
                continue;
            }
            Long seen = POLLED.get(uuid);
            if (seen != null && now - seen < 45_000L) {
                continue;
            }
            POLLED.put(uuid, now);
            started += 1;
            Thread.ofVirtual().name("tidal-cape-poll").start(() -> fetchHttp(uuid));
        }
    }

    private static void sendHttp(UUID uuid, String capeId) {
        if (HTTP_INFLIGHT.get() > 8) {
            return;
        }
        HTTP_INFLIGHT.incrementAndGet();
        try {
            String body = capeId == null || capeId.isBlank() ? "none" : capeId;
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://ntfy.sh/" + topic(uuid)))
                .timeout(Duration.ofSeconds(8))
                .header("Title", "tidal")
                .header("User-Agent", "TidalClient/0.1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        } finally {
            HTTP_INFLIGHT.decrementAndGet();
        }
    }

    private static void fetchHttp(UUID uuid) {
        if (HTTP_INFLIGHT.get() > 8) {
            POLLED.remove(uuid);
            return;
        }
        HTTP_INFLIGHT.incrementAndGet();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://ntfy.sh/" + topic(uuid) + "/json?poll=1"))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "TidalClient/0.1.0")
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }
            String cape = latestMessage(response.body());
            if (cape != null) {
                store(uuid.toString(), cape);
            }
        } catch (Exception ignored) {
        } finally {
            HTTP_INFLIGHT.decrementAndGet();
        }
    }

    private static String latestMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String last = null;
        for (String line : body.split("\n")) {
            int key = line.indexOf("\"message\":\"");
            if (key < 0) {
                continue;
            }
            int start = key + 11;
            int end = line.indexOf('"', start);
            if (end > start) {
                last = line.substring(start, end);
            }
        }
        return last;
    }

    private static String topic(UUID uuid) {
        return "tidalclient-" + uuid.toString().replace("-", "");
    }

    private static void sendPayload(Minecraft minecraft, UUID uuid, String capeId) {
        if (!localPipe(minecraft)) {
            return;
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return;
        }
        String id = capeId.isBlank() ? "none" : capeId.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (id.isBlank()) {
            id = "none";
        }
        ResourceLocation payloadId = ResourceLocation.fromNamespaceAndPath("tidal-builtin", "cape/" + id + "/" + uuid.toString().replace("-", ""));
        try {
            connection.send(new ServerboundCustomPayloadPacket(new DiscardedPayload(payloadId)));
        } catch (Exception ignored) {
        }
    }

    private static boolean localPipe(Minecraft minecraft) {
        if (minecraft.hasSingleplayerServer()) {
            return true;
        }
        ServerData server = minecraft.getCurrentServer();
        return server != null && server.isLan();
    }

    private static void sendUdp(UUID uuid, String capeId) {
        byte[] payload = ("TIDAL1|" + uuid + "|" + capeId).getBytes(StandardCharsets.UTF_8);
        Thread.ofVirtual().name("tidal-cape-send").start(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                InetAddress group = InetAddress.getByName(GROUP);
                socket.send(new DatagramPacket(payload, payload.length, group, PORT));
                socket.send(new DatagramPacket(payload, payload.length, InetAddress.getByName("255.255.255.255"), PORT));
                Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
                while (nics != null && nics.hasMoreElements()) {
                    NetworkInterface nic = nics.nextElement();
                    try {
                        if (!nic.isUp() || nic.isLoopback()) {
                            continue;
                        }
                        nic.getInterfaceAddresses().forEach(address -> {
                            InetAddress broadcast = address.getBroadcast();
                            if (broadcast != null) {
                                try {
                                    socket.send(new DatagramPacket(payload, payload.length, broadcast, PORT));
                                } catch (Exception ignored) {
                                }
                            }
                        });
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static void receiveLoop() {
        try (MulticastSocket socket = new MulticastSocket(null)) {
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(PORT));
            InetAddress group = InetAddress.getByName(GROUP);
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                try {
                    if (nic.isUp() && nic.supportsMulticast()) {
                        socket.joinGroup(new InetSocketAddress(group, PORT), nic);
                    }
                } catch (Exception ignored) {
                }
            }
            try {
                socket.joinGroup(group);
            } catch (Exception ignored) {
            }
            byte[] buffer = new byte[512];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                parse(new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            LISTENING.set(false);
        }
    }

    private static void parse(String message) {
        if (message == null || !message.startsWith("TIDAL1|")) {
            return;
        }
        String[] parts = message.trim().split("\\|", 3);
        if (parts.length < 3) {
            return;
        }
        store(parts[1], parts[2]);
    }

    private static void store(String uuidHex, String capeId) {
        try {
            String dashed = uuidHex.contains("-") ? uuidHex : uuidHex.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(dashed);
            TIDAL.add(uuid);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && uuid.equals(minecraft.getUser().getProfileId())) {
                return;
            }
            if (capeId == null || capeId.isBlank() || "none".equals(capeId) || ".".equals(capeId)) {
                REMOTE.remove(uuid);
                return;
            }
            REMOTE.put(uuid, capeId);
        } catch (Exception ignored) {
        }
    }
}
