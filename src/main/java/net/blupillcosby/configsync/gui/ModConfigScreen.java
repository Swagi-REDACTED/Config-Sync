package net.blupillcosby.configsync.gui;

import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import net.blupillcosby.configsync.ConfigSync;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;

public class ModConfigScreen extends CottonClientScreen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(new ModConfigGui(parent));
        this.parent = parent;
    }

    @Override
    public void removed() {
        super.removed();
        ConfigSync.CONFIG.save();
    }
}
