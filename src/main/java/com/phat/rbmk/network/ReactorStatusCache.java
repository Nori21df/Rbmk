package com.phat.rbmk.network;

import javax.annotation.Nullable;

/**
 * Nơi lưu gói trạng thái mới nhất phía client. Cố ý không import class client nào,
 * để handler đăng ký ở phía common an toàn trên dedicated server.
 */
public final class ReactorStatusCache {
    @Nullable
    private static volatile ReactorStatusPayload latest;

    private ReactorStatusCache() {}

    public static void set(ReactorStatusPayload payload) { latest = payload; }

    @Nullable
    public static ReactorStatusPayload get() { return latest; }

    public static void clear() { latest = null; }
}
