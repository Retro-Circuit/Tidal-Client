package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CapeShare {
    private static final int PORT = 41982;
    private static final String GROUP = "239.255.41.1";
    private static final Map<UUID, String> REMOTE = new ConcurrentHashMap<>();
    private static final AtomicBoolean LISTENING = new AtomicBoolean();
    private static long lastSend;

    private CapeShare() {}

    public static void listen() {
        if (!LISTENING.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual().name("tidal-cape-udp").start(CapeShare::receiveLoop);
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
        REMOTE.put(uuid, capeId);
        sendUdp(uuid, capeId);
        sendPayload(minecraft, uuid, capeId);
    }

    public static void tick(Minecraft minecraft) {
        listen();
        if (minecraft.player == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastSend < 2_000_000_000L) {
            return;
        }
        lastSend = now;
        String id = TidalMods.equippedCape == null ? "" : TidalMods.equippedCape;
        if (id.startsWith("ms:")) {
            id = "";
        }
        publish(id);
    }

    public static ResourceLocation location(UUID uuid) {
        String id = REMOTE.get(uuid);
        if (id == null || id.isBlank() || id.startsWith("ms:")) {
            return null;
        }
        return Capes.locationById(id);
    }

    public static void receive(ResourceLocation ResourceLocation) {
        if (ResourceLocation == null || !"tidal-builtin".equals(ResourceLocation.getNamespace())) {
            return;
        }
        String path = ResourceLocation.getPath();
        if (!path.startsWith("cape/")) {
            return;
        }
        String[] parts = path.split("/");
        if (parts.length < 3) {
            return;
        }
        store(parts[2], parts[1]);
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
                DatagramPacket packet = new DatagramPacket(payload, payload.length, InetAddress.getByName(GROUP), PORT);
                socket.send(packet);
                socket.send(new DatagramPacket(payload, payload.length, InetAddress.getByName("255.255.255.255"), PORT));
            } catch (Exception ignored) {
            }
        });
    }

    private static void receiveLoop() {
        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            socket.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(GROUP);
            try {
                socket.joinGroup(new InetSocketAddress(group, PORT), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
            } catch (Exception ignored) {
                socket.joinGroup(group);
            }
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                parse(message);
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
        store(parts[1].replace("-", ""), parts[2]);
    }

    private static void store(String uuidHex, String capeId) {
        try {
            String dashed = uuidHex.contains("-") ? uuidHex : uuidHex.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(dashed);
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && uuid.equals(minecraft.getUser().getProfileId())) {
                return;
            }
            if (capeId == null || capeId.isBlank() || "none".equals(capeId)) {
                REMOTE.remove(uuid);
                return;
            }
            REMOTE.put(uuid, capeId);
        } catch (Exception ignored) {
        }
    }
}
