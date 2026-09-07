package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayDeque;
import java.util.Deque;

public final class HudRuntime {
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean wasLeft;
    private static boolean wasRight;
    private static int combo;
    private static int lastHurtTime;
    private static boolean dead;
    private static boolean sentGg;
    private static int ggDelay;

    private HudRuntime() {}

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            combo = 0;
            dead = false;
            sentGg = false;
            ggDelay = 0;
            return;
        }
        long now = System.currentTimeMillis();
        boolean left = minecraft.mouseHandler.isLeftPressed();
        boolean right = minecraft.mouseHandler.isRightPressed();
        if (left && !wasLeft) {
            leftClicks.addLast(now);
        }
        if (right && !wasRight) {
            rightClicks.addLast(now);
        }
        wasLeft = left;
        wasRight = right;
        prune(leftClicks, now);
        prune(rightClicks, now);

        int hurt = minecraft.player.hurtTime;
        if (hurt > lastHurtTime && hurt > 0) {
            combo = 0;
        }
        lastHurtTime = hurt;

        boolean nowDead = !minecraft.player.isAlive() || minecraft.player.getHealth() <= 0.0f;
        if (nowDead && !dead && TidalMods.autoGg && !sentGg) {
            ggDelay = 12;
            sentGg = true;
        }
        if (!nowDead) {
            sentGg = false;
        }
        dead = nowDead;
        if (ggDelay > 0) {
            ggDelay--;
            if (ggDelay == 0 && minecraft.getConnection() != null) {
                minecraft.getConnection().sendChat("gg");
            }
        }
    }

    public static int leftCps() {
        prune(leftClicks, System.currentTimeMillis());
        return leftClicks.size();
    }

    public static int rightCps() {
        prune(rightClicks, System.currentTimeMillis());
        return rightClicks.size();
    }

    public static int combo() {
        return combo;
    }

    public static void onAttack(Player player, Entity target) {
        if (!(target instanceof LivingEntity living) || player != Minecraft.getInstance().player) {
            return;
        }
        combo++;
        Minecraft minecraft = Minecraft.getInstance();
        if (TidalMods.customParticles && minecraft.level != null) {
            for (int i = 0; i < 10; i++) {
                double ox = (Math.random() - 0.5) * living.getBbWidth();
                double oy = Math.random() * living.getBbHeight();
                double oz = (Math.random() - 0.5) * living.getBbWidth();
                minecraft.level.addParticle(
                    i % 2 == 0 ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT,
                    living.getX() + ox,
                    living.getY() + oy,
                    living.getZ() + oz,
                    0.0,
                    0.08,
                    0.0
                );
            }
        }
        if (TidalMods.autoGg && living.getHealth() <= 0.0f && minecraft.getConnection() != null && !sentGg) {
            minecraft.getConnection().sendChat("gg");
            sentGg = true;
        }
    }

    public static int ping(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return 0;
        }
        var info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.removeFirst();
        }
    }
}
