package net.blupillcosby.configsync.config;

import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import net.blupillcosby.configsync.ConfigSync;


import java.util.ArrayList;

@Version(version = 1)
public class ModConfig extends Config {

    public ValidatedBoolean enforceSync = new ValidatedBoolean(false);
    public ValidatedBoolean enableForAllMods = new ValidatedBoolean(false);
    
    public me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList<String> syncedMods = me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList.ofString();

    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public transient me.fzzyhmstrs.fzzy_config.config.ConfigAction selectMods = new me.fzzyhmstrs.fzzy_config.config.ConfigAction.Builder()
        .title(net.minecraft.network.chat.Component.literal("Select Mods"))
        .build(() -> {
            try {
                Class<?> clazz = Class.forName("net.blupillcosby.configsync.ConfigSyncClient");
                clazz.getMethod("openModConfigScreen").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    public ModConfig() {
        super(ConfigSync.id("main"));
    }

    @Override
    public int defaultPermLevel() {
        return 2; // Require OP level 2 for server settings
    }
}
