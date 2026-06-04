package net.blupillcosby.configsync.gui;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.blupillcosby.configsync.ConfigSync;
import net.blupillcosby.configsync.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModConfigGui extends LightweightGuiDescription {
    public static final int COLOR_BACKGROUND = 0xFF0A1A0E;
    public static final int COLOR_CARD = 0xFF152B1B;
    public static final int COLOR_ACCENT = 0xFF3DFF92;
    public static final int COLOR_DANGER = 0xFFFF4444;
    public static final int COLOR_TEXT = 0xFFF5F5F5;
    public static final int COLOR_TEXT_DIM = 0xFFB0B0B0;

    private final WPlainPanel root;
    private WPanel currentDialog;
    private final net.minecraft.client.gui.screens.Screen parentScreen;

    @SuppressWarnings("this-escape")
    public ModConfigGui(net.minecraft.client.gui.screens.Screen parentScreen) {
        this.parentScreen = parentScreen;
        root = new WPlainPanel();
        setRootPanel(root);
        root.setSize(320, 260);
        root.setInsets(Insets.ROOT_PANEL);

        WLabel title = new WLabel(Component.literal("CONFIG SYNC"), COLOR_ACCENT);
        title.setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(title, 0, 10, 320, 20);

        ModConfig config = ConfigSync.CONFIG;

        // General Settings Card
        WPlainPanel generalCard = createCard(300, 45);
        root.add(generalCard, 10, 35);

        WToggleButton allModsButton = new GreenToggleButton(Component.literal("Enable for All Mods"));
        allModsButton.setToggle(config.enableForAllMods.get());
        allModsButton.setOnToggle(b -> config.enableForAllMods.validateAndSet(b));
        generalCard.add(allModsButton, 10, 10, 280, 30);

        // Whitelist Section
        WLabel whitelistTitle = new WLabel(Component.literal("MOD WHITELIST"), COLOR_TEXT_DIM);
        root.add(whitelistTitle, 15, 85, 300, 15);

        WPlainPanel whitelistCard = createCard(300, 120);
        root.add(whitelistCard, 10, 100);

        // List entries
        WBox listContainer = new WBox(Axis.VERTICAL);
        listContainer.setSpacing(2);

        // Add Plus Button at top of list
        FlatButton openPickerButton = new FlatButton(Component.literal("+ ADD MOD"), COLOR_ACCENT);
        openPickerButton.setOnClick(() -> openDialog(new ModPicker(keys -> {
            List<String> updatedMods = new ArrayList<>(config.syncedMods.get());
            for (String key : keys) {
                if (!updatedMods.contains(key)) {
                    updatedMods.add(key);
                }
            }
            config.syncedMods.validateAndSet(updatedMods);
            Minecraft.getInstance().setScreen(new ModConfigScreen(parentScreen));
        })));
        listContainer.add(openPickerButton, 280, 22);

        // Current entries
        for (String modId : new ArrayList<>(config.syncedMods.get())) {
            WPlainPanel entry = new WPlainPanel();
            entry.setSize(280, 24);

            Optional<ModContainer> opt = FabricLoader.getInstance().getModContainer(modId);
            String labelStr = opt.isPresent() ? opt.get().getMetadata().getName() : modId;
            
            WLabel idLabel = new WLabel(Component.literal(labelStr), COLOR_TEXT);
            entry.add(idLabel, 30, 4, 200, 15);

            WItem itemIcon = new WItem(Items.COMMAND_BLOCK.getDefaultInstance());
            entry.add(itemIcon, 5, 4, 16, 16);

            FlatButton removeButton = new FlatButton(Component.literal("-"), COLOR_DANGER);
            removeButton.setOnClick(() -> {
                List<String> updatedMods = new ArrayList<>(config.syncedMods.get());
                updatedMods.remove(modId);
                config.syncedMods.validateAndSet(updatedMods);
                Minecraft.getInstance().setScreen(new ModConfigScreen(parentScreen));
            });
            entry.add(removeButton, 255, 3, 18, 18);

            listContainer.add(entry);
        }

        CustomScrollPanel scrollPanel = new CustomScrollPanel(listContainer);
        whitelistCard.add(scrollPanel, 5, 5, 290, 110);

        // Save Button
        FlatButton saveButton = new FlatButton(Component.literal("SAVE & CLOSE"), COLOR_ACCENT);
        saveButton.setOnClick(() -> {
            config.save();
            Minecraft.getInstance().setScreen(parentScreen);
        });
        root.add(saveButton, 10, 230, 300, 22);

        root.validate(this);
    }

    private WPlainPanel createCard(int width, int height) {
        WPlainPanel card = new WPlainPanel();
        card.setSize(width, height);
        card.setBackgroundPainter((context, x, y, panel) -> {
            ScreenDrawing.drawBeveledPanel(context, x, y, panel.getWidth(), panel.getHeight(), COLOR_CARD, COLOR_CARD,
                    COLOR_CARD);
        });
        return card;
    }

    @Override
    public void addPainters() {
        this.rootPanel.setBackgroundPainter((context, x, y, panel) -> {
            ScreenDrawing.drawGuiPanel(context, x, y, panel.getWidth(), panel.getHeight(), COLOR_BACKGROUND);
        });
    }

    public void openDialog(WPanel dialog) {
        if (currentDialog == null) {
            currentDialog = dialog;
            int dx = (root.getWidth() - dialog.getWidth()) / 2;
            int dy = (root.getHeight() - dialog.getHeight()) / 2;
            root.add(dialog, dx, dy, dialog.getWidth(), dialog.getHeight());
            root.validate(this);
        }
    }

    private class FlatButton extends WButton {
        private final int color;
        private boolean isDialogWidget = false;

        public FlatButton(Component label, int color) {
            super(label);
            this.color = color;
        }

        public FlatButton setDialogWidget(boolean dialogWidget) {
            isDialogWidget = dialogWidget;
            return this;
        }

        @Override
        public void paint(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
            boolean hovered = (mouseX >= 0 && mouseY >= 0 && mouseX < getWidth() && mouseY < getHeight())
                    && (isDialogWidget || currentDialog == null);
            int drawColor = isEnabled() ? (hovered ? lighten(color, 30) : color) : 0xFF444444;
            ScreenDrawing.drawGuiPanel(context, x, y, getWidth(), getHeight(), drawColor);

            int textX = (getWidth() - Minecraft.getInstance().font.width(getLabel())) / 2;
            int textY = (getHeight() - 8) / 2;
            ScreenDrawing.drawString(context, getLabel().getVisualOrderText(), HorizontalAlignment.LEFT, x + textX,
                    y + textY, getWidth(), COLOR_TEXT);
        }

        @Override
        public InputResult onClick(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            if (!isDialogWidget && currentDialog != null)
                return InputResult.IGNORED;
            return super.onClick(click, doubled);
        }

        private int lighten(int color, int amount) {
            int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
            int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
            int b = Math.min(255, (color & 0xFF) + amount);
            return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
    }

    private class GreenToggleButton extends WToggleButton {
        public GreenToggleButton(Component label) {
            super(label);
        }

        @Override
        public void paint(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
            boolean hovered = (mouseX >= 0 && mouseY >= 0 && mouseX < getWidth() && mouseY < getHeight())
                    && currentDialog == null;
            int boxColor = getToggle() ? COLOR_ACCENT : (hovered ? 0xFF444444 : 0xFF333333);

            ScreenDrawing.drawBeveledPanel(context, x, y + 1, 18, 18, boxColor, boxColor, boxColor);
            if (getToggle()) {
                ScreenDrawing.drawString(context, "✔", HorizontalAlignment.CENTER, x + 1, y + 6, 16, COLOR_TEXT);
            }

            ScreenDrawing.drawString(context, label.getVisualOrderText(), HorizontalAlignment.LEFT, x + 24, y + 6,
                    getWidth() - 24, COLOR_TEXT);
        }

        @Override
        public InputResult onClick(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
            if (currentDialog != null)
                return InputResult.IGNORED;
            return super.onClick(click, doubled);
        }
    }

    private class GreenScrollBar extends WScrollBar {
        public GreenScrollBar(Axis axis) {
            super(axis);
        }

        @Override
        public WScrollBar setValue(int value) {
            super.setValue(value);
            if (parent != null) parent.layout();
            return this;
        }

        @Override
        public void paint(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
            ScreenDrawing.coloredRect(context, x, y, width, height, 0x44FFFFFF);
            if (maxValue <= 0)
                return;

            boolean hovered = (mouseX >= 0 && mouseY >= 0 && mouseX < getWidth() && mouseY < getHeight());
            int handleColor = sliding ? 0xFF888888 : (hovered ? 0xFFAAAAAA : 0xFF666666);

            ScreenDrawing.coloredRect(context, x + 1, y + 1 + getHandlePosition(), width - 2, getHandleSize(),
                    handleColor);
        }
    }

    private class CustomScrollPanel extends WPanel {
        private final WWidget widget;
        private final WScrollBar scrollBar;

        public CustomScrollPanel(WWidget widget) {
            this.widget = widget;
            this.scrollBar = new GreenScrollBar(Axis.VERTICAL);
            this.widget.setParent(this);
            this.scrollBar.setParent(this);
            this.children.add(widget);
            this.children.add(scrollBar);
        }

        @Override
        public void layout() {
            scrollBar.setSize(8, height);
            scrollBar.setLocation(width - 8, 0);
            scrollBar.setWindow(height);
            int max = Math.max(0, widget.getHeight() - height);
            scrollBar.setMaxValue(max);
            
            if (scrollBar.getValue() > max) {
                scrollBar.setValue(max);
            }

            boolean showScroll = scrollBar.getMaxValue() > 0;
            int widgetWidth = showScroll ? width - 10 : width;

            widget.setSize(widgetWidth, widget.getHeight());
            widget.setLocation(0, -scrollBar.getValue());
            if (widget instanceof WPanel)
                ((WPanel) widget).layout();
        }

        @Override
        public void paint(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
            widget.setLocation(0, -scrollBar.getValue()); 
            
            boolean showScroll = scrollBar.getMaxValue() > 0;
            int clipWidth = showScroll ? width - 8 : width;

            context.enableScissor(x, y, x + clipWidth, y + height);
            widget.paint(context, x + widget.getX(), y + widget.getY(), mouseX - widget.getX(), mouseY - widget.getY());
            context.disableScissor();

            if (showScroll) {
                scrollBar.paint(context, x + scrollBar.getX(), y + scrollBar.getY(), mouseX - scrollBar.getX(),
                        mouseY - scrollBar.getY());
            }
        }

        @Override
        public InputResult onMouseScroll(int x, int y, double horizontalAmount, double verticalAmount) {
            if (scrollBar.getMaxValue() <= 0)
                return InputResult.IGNORED;
            return scrollBar.onMouseScroll(0, 0, horizontalAmount, verticalAmount);
        }
    }

    private class ModPicker extends WPlainPanel {
        private final java.util.function.Consumer<List<String>> callback;
        private final List<String> selected = new ArrayList<>();
        private final ClearableBox pickerList;
        private final CustomScrollPanel pScroll;
        private final List<ModGroup> allGroups = new ArrayList<>();

        private record ModGroup(String label, String id) {}

        public ModPicker(java.util.function.Consumer<List<String>> callback) {
            this.callback = callback;
            setSize(240, 240);
            setBackgroundPainter((context, x, y, panel) -> {
                ScreenDrawing.drawGuiPanel(context, x, y, panel.getWidth(), panel.getHeight(), 0xFF262626);
                ScreenDrawing.drawBeveledPanel(context, x, y, panel.getWidth(), panel.getHeight(), 0xFF666666,
                        0xFF222222, 0xFF666666);
            });

            WLabel pTitle = new WLabel(Component.literal("Select Mods"), COLOR_ACCENT);
            pTitle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            add(pTitle, 0, 8, 240, 15);

            WTextField searchField = new WTextField(Component.literal("Search..."));
            add(searchField, 10, 25, 220, 20);

            pickerList = new ClearableBox(Axis.VERTICAL);
            pickerList.setSpacing(2);

            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                String id = mod.getMetadata().getId();
                String name = mod.getMetadata().getName();
                allGroups.add(new ModGroup(name, id));
            }

            searchField.setChangedListener(this::updateFilter);

            pScroll = new CustomScrollPanel(pickerList);
            add(pScroll, 5, 50, 230, 155);

            updateFilter(""); // Initial populate

            FlatButton closeBtn = new FlatButton(Component.literal("CANCEL"), 0xFF666666).setDialogWidget(true);
            closeBtn.setOnClick(() -> {
                currentDialog = null;
                root.remove(this);
            });
            add(closeBtn, 10, 210, 220, 22);
        }

        private void updateFilter(String filter) {
            pickerList.clear();
            String lowerFilter = filter.toLowerCase();
            int count = 0;
            for (ModGroup group : allGroups) {
                if (group.label().toLowerCase().contains(lowerFilter) || group.id().toLowerCase().contains(lowerFilter)) {
                    addEntry(pickerList, group);
                    count++;
                }
            }
            pickerList.setSize(220, count * 26);
            pickerList.layout();
            pScroll.layout();
            pScroll.scrollBar.setValue(0);
        }

        private void addEntry(ClearableBox list, ModGroup group) {
            WPlainPanel entry = new WPlainPanel();
            entry.setSize(220, 24);

            WLabel label = new WLabel(Component.literal(group.label()), COLOR_TEXT);
            entry.add(label, 30, 4, 150, 15);

            WItem icon = new WItem(Items.COMMAND_BLOCK.getDefaultInstance());
            entry.add(icon, 5, 4, 16, 16);

            FlatButton selectBtn = new FlatButton(Component.literal("+"), COLOR_ACCENT).setDialogWidget(true);
            selectBtn.setOnClick(() -> {
                selected.add(group.id());
                callback.accept(selected);
                currentDialog = null;
                root.remove(this);
            });
            entry.add(selectBtn, 195, 3, 18, 18);

            list.add(entry);
        }
    }

    private static class ClearableBox extends WBox {
        public ClearableBox(Axis axis) {
            super(axis);
        }
        public void clear() {
            this.children.clear();
        }
    }
}
