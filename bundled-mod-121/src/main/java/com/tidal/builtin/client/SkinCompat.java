package com.tidal.builtin.client;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class SkinCompat {
    private SkinCompat() {}

    public static ResourceLocation bodyId(Object skin) {
        return asId(call(skin, "texture", "body"));
    }

    public static PlayerSkin replace(PlayerSkin skin, ResourceLocation bodyOverride, ResourceLocation capeOverride, Object modelOverride) {
        if (skin == null) {
            return null;
        }
        try {
            Object body = bodyOverride != null ? wrap(skin, bodyOverride, "texture", "body") : call(skin, "texture", "body");
            Object cape = capeOverride != null ? wrap(skin, capeOverride, "capeTexture", "cape") : call(skin, "capeTexture", "cape");
            Object elytra = call(skin, "elytraTexture", "elytra");
            Object url = call(skin, "textureUrl");
            Object model = modelOverride != null ? modelOverride : call(skin, "model");
            Object secure = call(skin, "secure");
            PlayerSkin next = construct(skin, body, url, cape, elytra, model, secure);
            return next != null ? next : skin;
        } catch (Throwable ignored) {
            return skin;
        }
    }

    private static PlayerSkin construct(PlayerSkin sample, Object body, Object url, Object cape, Object elytra, Object model, Object secure) {
        Constructor<?>[] constructors = sample.getClass().getDeclaredConstructors();
        Object[] six = {body, url, cape, elytra, model, secure};
        Object[] five = {body, cape, elytra, model, secure};
        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
            int n = constructor.getParameterCount();
            try {
                if (n == 6) {
                    return (PlayerSkin) constructor.newInstance(adapt(constructor, six));
                }
                if (n == 5) {
                    return (PlayerSkin) constructor.newInstance(adapt(constructor, five));
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object[] adapt(Constructor<?> constructor, Object[] values) {
        Class<?>[] types = constructor.getParameterTypes();
        Object[] out = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = coerce(values[i], types[i]);
        }
        return out;
    }

    private static Object coerce(Object value, Class<?> type) {
        if (value == null || type.isInstance(value)) {
            return value;
        }
        if (type == boolean.class || type == Boolean.class) {
            return value instanceof Boolean boxed ? boxed : Boolean.FALSE;
        }
        ResourceLocation id = asId(value);
        if (id != null && type.isAssignableFrom(id.getClass())) {
            return id;
        }
        if (id != null) {
            Object wrapped = wrapType(id, type);
            if (wrapped != null) {
                return wrapped;
            }
        }
        return value;
    }

    private static Object wrap(Object sampleSkin, ResourceLocation id, String... sampleMethods) {
        Object sample = call(sampleSkin, sampleMethods);
        if (sample != null && !(sample instanceof ResourceLocation)) {
            Object wrapped = wrapLike(sample, id);
            if (wrapped != null) {
                return wrapped;
            }
        }
        return id;
    }

    private static Object wrapLike(Object sample, ResourceLocation id) {
        Class<?> type = sample.getClass();
        Object wrapped = wrapType(id, type);
        if (wrapped != null) {
            return wrapped;
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 1) {
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(id);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object wrapType(ResourceLocation id, Class<?> type) {
        if (type.isAssignableFrom(id.getClass())) {
            return id;
        }
        for (String name : new String[] {
            "net.minecraft.core.ClientAsset$ResourceTexture",
            "net.minecraft.core.ClientAsset$Texture",
            "net.minecraft.client.resources.PlayerSkin$Patch"
        }) {
            try {
                Class<?> cls = Class.forName(name);
                if (!type.isAssignableFrom(cls)) {
                    continue;
                }
                for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 1) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(id);
                    }
                    if (constructor.getParameterCount() == 2) {
                        constructor.setAccessible(true);
                        return constructor.newInstance(id, id);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static ResourceLocation asId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ResourceLocation id) {
            return id;
        }
        Object nested = call(value, "texturePath", "id", "texture", "location");
        return nested instanceof ResourceLocation id ? id : null;
    }

    private static Object call(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
