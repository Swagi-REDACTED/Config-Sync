package net.blupillcosby.configsync;

import net.blupillcosby.configsync.config.ModConfig;
import net.blupillcosby.configsync.network.ConfigSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConfigSync implements ModInitializer {
    public static final String MOD_ID = "config_sync";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModConfig CONFIG = me.fzzyhmstrs.fzzy_config.api.ConfigApiJava.registerAndLoadConfig(ModConfig::new);

    public static Identifier id(String path) {
        return Identifier.parse(MOD_ID + ":" + path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (CONFIG.enforceSync.get()) {
                byte[] zippedConfigs = getZippedConfigs();
                if (zippedConfigs != null && zippedConfigs.length > 0) {
                    ServerPlayNetworking.send(handler.player, new ConfigSyncPayload(zippedConfigs));
                    LOGGER.info("Sent enforced configs to player {}", handler.player.getName().getString());
                }
            }
        });
    }

    private byte[] getZippedConfigs() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Path configDir = FabricLoader.getInstance().getConfigDir();
            List<String> modsToSync = new ArrayList<>();
            if (CONFIG.enableForAllMods.get()) {
                FabricLoader.getInstance().getAllMods().forEach(mod -> modsToSync.add(mod.getMetadata().getId()));
            } else {
                modsToSync.addAll(CONFIG.syncedMods.get());
            }

            for (String modId : modsToSync) {
                File modConfigFile = configDir.resolve(modId + ".json").toFile();
                if (modConfigFile.exists() && modConfigFile.isFile()) {
                    addFileToZip(zos, modConfigFile, modConfigFile.getName());
                }

                File modConfigDir = configDir.resolve(modId).toFile();
                if (modConfigDir.exists() && modConfigDir.isDirectory()) {
                    Files.walk(modConfigDir.toPath())
                            .filter(Files::isRegularFile)
                            .forEach(path -> addFileToZip(zos, path.toFile(), configDir.relativize(path).toString().replace('\\', '/')));
                }
            }
            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Failed to zip configs for sync", e);
            return null;
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String zipEntryName) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            LOGGER.error("Failed to add file {} to config zip", file.getName(), e);
        }
    }
}
