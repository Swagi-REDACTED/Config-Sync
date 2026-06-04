package net.blupillcosby.configsync;

import net.blupillcosby.configsync.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConfigSyncClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                try {
                    unpackConfigs(payload.zippedData());
                    ConfigSync.LOGGER.info("Successfully received and applied synced configs from server.");
                } catch (Exception e) {
                    ConfigSync.LOGGER.error("Failed to unpack synced configs", e);
                }
            });
        });
    }

    private void unpackConfigs(byte[] zippedData) throws IOException {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zippedData);
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File targetFile = configDir.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) >= 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static void openModConfigScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new net.blupillcosby.configsync.gui.ModConfigScreen(net.minecraft.client.Minecraft.getInstance().screen));
    }
}
