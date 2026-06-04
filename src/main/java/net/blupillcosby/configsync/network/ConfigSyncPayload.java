package net.blupillcosby.configsync.network;

import net.blupillcosby.configsync.ConfigSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigSyncPayload(byte[] zippedData) implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(ConfigSync.id("config_sync_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = CustomPacketPayload.codec(
        ConfigSyncPayload::write, ConfigSyncPayload::new
    );

    private ConfigSyncPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readByteArray());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeByteArray(this.zippedData);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
