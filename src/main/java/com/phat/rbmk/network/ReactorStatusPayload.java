package com.phat.rbmk.network;

import com.phat.rbmk.RbmkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: toàn bộ trạng thái lò để vẽ GUI. Gửi mỗi 10 tick cho người đang mở GUI. */
public record ReactorStatusPayload(
        BlockPos pos, boolean formed, Component message,
        int radius, int height, int loadedFuel, int fuelSlots, int coolantPorts, int outputPorts,
        boolean az5, boolean manual,
        float setpoint, float power, float maxTemp, float ruptureTemp, float voidFrac, float xenon, float rod,
        float waterFrac, float waterPerTick, float steamPerTick, float fePerTick, float backpressure, float ormWarning,
        byte[] qs, byte[] rs, byte[] types, float[] temps, float[] fluxes
) implements CustomPacketPayload {

    public static final Type<ReactorStatusPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RbmkMod.MODID, "reactor_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorStatusPayload> STREAM_CODEC =
            StreamCodec.ofMember(ReactorStatusPayload::write, ReactorStatusPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(formed);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, message);
        buf.writeVarInt(radius);
        buf.writeVarInt(height);
        buf.writeVarInt(loadedFuel);
        buf.writeVarInt(fuelSlots);
        buf.writeVarInt(coolantPorts);
        buf.writeVarInt(outputPorts);
        buf.writeBoolean(az5);
        buf.writeBoolean(manual);
        buf.writeFloat(setpoint);
        buf.writeFloat(power);
        buf.writeFloat(maxTemp);
        buf.writeFloat(ruptureTemp);
        buf.writeFloat(voidFrac);
        buf.writeFloat(xenon);
        buf.writeFloat(rod);
        buf.writeFloat(waterFrac);
        buf.writeFloat(waterPerTick);
        buf.writeFloat(steamPerTick);
        buf.writeFloat(fePerTick);
        buf.writeFloat(backpressure);
        buf.writeFloat(ormWarning);
        buf.writeByteArray(qs);
        buf.writeByteArray(rs);
        buf.writeByteArray(types);
        buf.writeVarInt(temps.length);
        for (float t : temps) buf.writeFloat(t);
        buf.writeVarInt(fluxes.length);
        for (float f : fluxes) buf.writeFloat(f);
    }

    private static ReactorStatusPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean formed = buf.readBoolean();
        Component message = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        int radius = buf.readVarInt();
        int height = buf.readVarInt();
        int loadedFuel = buf.readVarInt();
        int fuelSlots = buf.readVarInt();
        int coolantPorts = buf.readVarInt();
        int outputPorts = buf.readVarInt();
        boolean az5 = buf.readBoolean();
        boolean manual = buf.readBoolean();
        float setpoint = buf.readFloat();
        float power = buf.readFloat();
        float maxTemp = buf.readFloat();
        float ruptureTemp = buf.readFloat();
        float voidFrac = buf.readFloat();
        float xenon = buf.readFloat();
        float rod = buf.readFloat();
        float waterFrac = buf.readFloat();
        float waterPerTick = buf.readFloat();
        float steamPerTick = buf.readFloat();
        float fePerTick = buf.readFloat();
        float backpressure = buf.readFloat();
        float ormWarning = buf.readFloat();
        byte[] qs = buf.readByteArray();
        byte[] rs = buf.readByteArray();
        byte[] types = buf.readByteArray();
        float[] temps = new float[buf.readVarInt()];
        for (int i = 0; i < temps.length; i++) temps[i] = buf.readFloat();
        float[] fluxes = new float[buf.readVarInt()];
        for (int i = 0; i < fluxes.length; i++) fluxes[i] = buf.readFloat();
        return new ReactorStatusPayload(pos, formed, message, radius, height, loadedFuel, fuelSlots, coolantPorts,
                outputPorts, az5, manual, setpoint, power, maxTemp, ruptureTemp, voidFrac, xenon, rod, waterFrac,
                waterPerTick, steamPerTick, fePerTick, backpressure, ormWarning, qs, rs, types, temps, fluxes);
    }
}
