package com.tre.sololeveling.client.screen;

import com.tre.sololeveling.client.ClientHunterData;
import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SystemScreen extends Screen {
    public enum Tab { HOME, STATUS, SKILLS, QUESTS, STORE, EQUIPMENT, INVENTORY, SHADOWS, SETTINGS }

    private static final String[] STATS = {"strength", "agility", "stamina", "intelligence", "sense"};
    private static final String[] ACTIVE_SKILLS = {"stealth", "bloodlust", "quicksilver", "mutilation", "dagger_rush", "rulers_authority", "dragons_fear"};
    private static final String[] STORE_IDS = {"healing_potion", "mana_potion", "greater_healing_potion", "greater_mana_potion", "knight_killer", "blessed_random_box"};
    private static final int[] STORE_PRICES = {50, 60, 150, 175, 1500, 750};

    private final EnumMap<Tab, Integer> scrollOffsets = new EnumMap<>(Tab.class);
    private final Map<String, Button> actionButtons = new HashMap<>();
    private final List<StoreButton> storeButtons = new ArrayList<>();
    private Tab tab;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private boolean wideNavigation;
    private boolean lastAwakened;
    private int inventoryPage;
    private String selectedSkill = "mutilation";

    public SystemScreen(Tab tab) {
        super(Component.translatable("screen.sololeveling.system"));
        this.tab = tab == null ? Tab.HOME : tab;
        for (Tab value : Tab.values()) scrollOffsets.put(value, 0);
    }

    @Override
    protected void init() {
        actionButtons.clear();
        storeButtons.clear();
        panelWidth = Math.min(Math.max(220, width - 24), Math.min(520, Math.max(220, width - 8)));
        panelHeight = Math.min(Math.max(176, height - 24), Math.min(300, Math.max(176, height - 8)));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        wideNavigation = panelWidth >= 410 && panelHeight >= 224;
        contentX = left + (wideNavigation ? 108 : 12);
        contentY = top + (wideNavigation ? 38 : 84);
        contentWidth = panelWidth - (wideNavigation ? 120 : 24);
        contentHeight = panelHeight - (wideNavigation ? 50 : 96);
        lastAwakened = ClientHunterData.awakened();
        addNavigation();

        if (!lastAwakened) {
            Button awaken = Button.builder(Component.literal("AWAKEN SYSTEM"), button -> send("AWAKEN"))
                    .bounds(left + panelWidth / 2 - 66, top + panelHeight / 2 - 10, 132, 20).build();
            addRenderableWidget(awaken);
            return;
        }

        switch (tab) {
            case HOME -> { }
            case STATUS -> addStatsButtons();
            case SKILLS -> addSkillButtons();
            case QUESTS -> addQuestButtons();
            case STORE -> addStoreButtons();
            case EQUIPMENT -> { }
            case INVENTORY -> addInventoryButtons();
            case SHADOWS -> addShadowButtons();
            case SETTINGS -> addSettingsButtons();
        }
        refreshButtonStates();
    }

    private void addNavigation() {
        Tab[] values = Tab.values();
        if (wideNavigation) {
            int y = top + 35;
            for (Tab value : values) {
                addTabButton(value, left + 10, y, 86, 18);
                y += 20;
            }
        } else {
            int gap = 2;
            int buttonWidth = (panelWidth - 24 - gap * 2) / 3;
            for (int i = 0; i < values.length; i++) {
                int column = i % 3;
                int row = i / 3;
                addTabButton(values[i], left + 12 + column * (buttonWidth + gap), top + 25 + row * 18, buttonWidth, 16);
            }
        }
    }

    private void addTabButton(Tab target, int x, int y, int width, int height) {
        Button button = Button.builder(Component.literal(tabName(target)), ignored -> {
            tab = target;
            inventoryPage = 0;
            rebuildWidgets();
        }).bounds(x, y, width, height).build();
        button.active = target != tab;
        addRenderableWidget(button);
    }

    private void addStatsButtons() {
        for (int i = 0; i < STATS.length; i++) {
            String stat = STATS[i];
            int y = contentY + 18 + i * statRowStep();
            Button button = Button.builder(Component.literal("+"), ignored -> send("ALLOCATE:" + stat))
                    .bounds(contentX + contentWidth - 22, y - 3, 18, 16).build();
            actionButtons.put("stat:" + stat, button);
            addRenderableWidget(button);
        }
    }

    private void addSkillButtons() {
        Button activate = Button.builder(Component.literal("ACTIVATE"), ignored -> {
            if (isActiveSkill(selectedSkill)) send("ABILITY:" + selectedSkill);
            else SystemUi.Notifications.pushFailure("This skill is passive or controlled elsewhere");
        }).bounds(contentX, contentY + contentHeight - 20, Math.min(105, contentWidth), 18).build();
        actionButtons.put("activate_skill", activate);
        addRenderableWidget(activate);
    }

    private void addQuestButtons() {
        int y = contentY + contentHeight - 20;
        int claimWidth = Math.min(72, Math.max(56, contentWidth / 4));
        int remaining = contentWidth - claimWidth - 6;
        int exerciseWidth = Math.max(42, remaining / 3);
        String[] names = {"Push-up", "Sit-up", "Squat"};
        String[] ids = {"pushup", "situp", "squat"};
        for (int i = 0; i < 3; i++) {
            int index = i;
            Button button = Button.builder(Component.literal(names[i]), ignored -> send("EXERCISE:" + ids[index]))
                    .bounds(contentX + i * exerciseWidth, y, exerciseWidth - 2, 18).build();
            addRenderableWidget(button);
        }
        Button claim = Button.builder(Component.literal("CLAIM"), ignored -> send("CLAIM_DAILY"))
                .bounds(contentX + contentWidth - claimWidth, y, claimWidth, 18).build();
        actionButtons.put("claim_daily", claim);
        addRenderableWidget(claim);
    }

    private void addStoreButtons() {
        int columns = contentWidth >= 270 ? 2 : 1;
        int buttonWidth = (contentWidth - (columns - 1) * 5) / columns;
        int rowHeight = 22;
        for (int i = 0; i < STORE_IDS.length; i++) {
            int column = i % columns;
            int row = i / columns;
            String id = STORE_IDS[i];
            int price = STORE_PRICES[i];
            Button button = Button.builder(Component.literal(SystemUi.titleCase(id) + "  " + price + "G"), ignored -> send("BUY:" + id))
                    .bounds(contentX + column * (buttonWidth + 5), contentY + 20 + row * rowHeight, buttonWidth, 20).build();
            storeButtons.add(new StoreButton(button, price));
            addRenderableWidget(button);
        }
    }

    private void addInventoryButtons() {
        int y = contentY + contentHeight - 20;
        Button store = Button.builder(Component.literal("STORE HELD"), ignored -> send("STORE_HELD"))
                .bounds(contentX, y, Math.min(102, contentWidth / 2), 18).build();
        Button previous = Button.builder(Component.literal("<"), ignored -> {
            inventoryPage = Math.max(0, inventoryPage - 1);
            refreshButtonStates();
        }).bounds(contentX + contentWidth - 48, y, 22, 18).build();
        Button next = Button.builder(Component.literal(">"), ignored -> {
            inventoryPage = Math.min(maxInventoryPage(ClientHunterData.view()), inventoryPage + 1);
            refreshButtonStates();
        }).bounds(contentX + contentWidth - 23, y, 22, 18).build();
        actionButtons.put("store_held", store);
        actionButtons.put("inventory_previous", previous);
        actionButtons.put("inventory_next", next);
        addRenderableWidget(store);
        addRenderableWidget(previous);
        addRenderableWidget(next);
    }

    private void addShadowButtons() {
        String[] labels = {"EXTRACT", "SUMMON", "DISMISS", "EXCHANGE", "DOMAIN", "MODE"};
        String[] actions = {"EXTRACT", "SUMMON_SHADOW", "DISMISS_SHADOWS", "SHADOW_EXCHANGE", "TOGGLE_DOMAIN", "SHADOW_MODE"};
        int columns = contentWidth >= 270 ? 3 : 2;
        int buttonWidth = (contentWidth - (columns - 1) * 4) / columns;
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            int column = i % columns;
            int row = i / columns;
            Button button = Button.builder(Component.literal(labels[i]), ignored -> send(actions[index]))
                    .bounds(contentX + column * (buttonWidth + 4), contentY + contentHeight - 44 + row * 22, buttonWidth, 18).build();
            actionButtons.put("shadow:" + actions[i], button);
            addRenderableWidget(button);
        }
    }

    private void addSettingsButtons() {
        int columns = contentWidth >= 270 ? 2 : 1;
        int buttonWidth = (contentWidth - (columns - 1) * 5) / columns;
        addSettingButton(0, columns, buttonWidth, () -> "SERVER HUD: " + onOff(ClientHunterData.view().hudEnabled()), () -> send("TOGGLE_HUD"));
        addSettingButton(1, columns, buttonWidth, () -> "QUEST TRACKER: " + onOff(SystemUi.Settings.showQuestTracker()), SystemUi.Settings::toggleQuestTracker);
        addSettingButton(2, columns, buttonWidth, () -> "COOLDOWNS: " + onOff(SystemUi.Settings.showCooldowns()), SystemUi.Settings::toggleCooldowns);
        addSettingButton(3, columns, buttonWidth, () -> "NOTIFICATIONS: " + onOff(SystemUi.Settings.showNotifications()), SystemUi.Settings::toggleNotifications);
        addSettingButton(4, columns, buttonWidth, () -> "COMPACT: " + onOff(SystemUi.Settings.compactHud()), SystemUi.Settings::toggleCompactHud);
        addSettingButton(5, columns, buttonWidth, () -> "ANCHOR: " + SystemUi.titleCase(SystemUi.Settings.anchor().name()), SystemUi.Settings::cycleAnchor);
        addSettingButton(6, columns, buttonWidth, () -> "SCALE -", () -> SystemUi.Settings.adjustScale(-5));
        addSettingButton(7, columns, buttonWidth, () -> "SCALE +", () -> SystemUi.Settings.adjustScale(5));
    }

    private void addSettingButton(int index, int columns, int buttonWidth, LabelSupplier label, Runnable action) {
        int column = index % columns;
        int row = index / columns;
        Button button = Button.builder(Component.literal(label.get()), ignored -> {
            action.run();
            rebuildWidgets();
        }).bounds(contentX + column * (buttonWidth + 5), contentY + 14 + row * 19, buttonWidth, 17).build();
        addRenderableWidget(button);
    }

    @Override
    public void tick() {
        if (lastAwakened != ClientHunterData.awakened()) {
            rebuildWidgets();
            return;
        }
        refreshButtonStates();
    }

    private void refreshButtonStates() {
        SystemUi.Data data = ClientHunterData.view();
        for (String stat : STATS) {
            Button button = actionButtons.get("stat:" + stat);
            if (button != null) button.active = data.statPoints() > 0;
        }
        Button activate = actionButtons.get("activate_skill");
        if (activate != null) {
            boolean activeSkill = isActiveSkill(selectedSkill);
            boolean unlocked = data.skillUnlocked(selectedSkill);
            activate.active = activeSkill && unlocked && data.cooldownRemaining(selectedSkill) <= 0L;
            activate.setMessage(Component.literal(activeSkill ? (data.cooldownRemaining(selectedSkill) > 0L ? SystemUi.Cooldowns.remainingText(data, selectedSkill) : "ACTIVATE") : "PASSIVE"));
        }
        Button claim = actionButtons.get("claim_daily");
        if (claim != null) claim.active = data.dailyComplete() && !data.dailyClaimed();
        for (StoreButton entry : storeButtons) entry.button.active = data.gold() >= entry.price && data.compounds("system_inventory").size() < 108;
        Button storeHeld = actionButtons.get("store_held");
        if (storeHeld != null) {
            Minecraft minecraft = Minecraft.getInstance();
            storeHeld.active = minecraft.player != null && !minecraft.player.getMainHandItem().isEmpty()
                    && data.compounds("system_inventory").size() < 108;
        }
        Button previous = actionButtons.get("inventory_previous");
        Button next = actionButtons.get("inventory_next");
        int maximumPage = maxInventoryPage(data);
        inventoryPage = Math.min(inventoryPage, maximumPage);
        if (previous != null) previous.active = inventoryPage > 0;
        if (next != null) next.active = inventoryPage < maximumPage;

        setActive("shadow:EXTRACT", data.skillUnlocked("shadow_extraction") && data.cooldownRemaining("shadow_extraction") <= 0L);
        setActive("shadow:SUMMON_SHADOW", data.skillUnlocked("shadow_preservation") && !data.compounds("shadows").isEmpty());
        setActive("shadow:DISMISS_SHADOWS", !data.strings("active_shadows").isEmpty());
        setActive("shadow:SHADOW_EXCHANGE", data.skillUnlocked("shadow_exchange") && !data.strings("active_shadows").isEmpty() && data.cooldownRemaining("shadow_exchange") <= 0L);
        setActive("shadow:TOGGLE_DOMAIN", data.skillUnlocked("monarch_domain"));
        setActive("shadow:SHADOW_MODE", !data.compounds("shadows").isEmpty());
    }

    private void setActive(String key, boolean active) {
        Button button = actionButtons.get(key);
        if (button != null) button.active = active;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        SystemUi.Theme.panel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawCenteredString(font, "SYSTEM", left + panelWidth / 2, top + 10, SystemUi.Theme.CYAN);
        if (!ClientHunterData.awakened()) {
            graphics.drawCenteredString(font, "A dormant interface is awaiting authorization.", left + panelWidth / 2,
                    top + panelHeight / 2 - 32, SystemUi.Theme.TEXT_DIM);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        renderSectionTitle(graphics);
        SystemUi.Data data = ClientHunterData.view();
        switch (tab) {
            case HOME -> renderHome(graphics, data);
            case STATUS -> renderStats(graphics, data);
            case SKILLS -> renderSkills(graphics, data, mouseX, mouseY);
            case QUESTS -> renderQuests(graphics, data);
            case STORE -> renderStore(graphics, data);
            case EQUIPMENT -> renderEquipment(graphics, data, mouseX, mouseY);
            case INVENTORY -> renderInventory(graphics, data, mouseX, mouseY);
            case SHADOWS -> renderShadows(graphics, data);
            case SETTINGS -> renderSettings(graphics, data);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSectionTitle(GuiGraphics graphics) {
        graphics.drawString(font, tabName(tab).toUpperCase(Locale.ROOT), contentX, contentY - 15, SystemUi.Theme.TEXT, false);
        graphics.fill(contentX, contentY - 5, contentX + contentWidth, contentY - 4, 0x6635D9FF);
    }

    private void renderHome(GuiGraphics graphics, SystemUi.Data data) {
        int cardHeight = Math.max(24, Math.min(44, (contentHeight - 8) / 3));
        homeCard(graphics, contentY, "HUNTER", "Level " + data.level() + "  •  " + data.rank());
        homeCard(graphics, contentY + cardHeight + 4, "ACTIVE QUEST", data.activeQuestName() + " — " + data.activeQuestProgress());
        homeCard(graphics, contentY + (cardHeight + 4) * 2, "SYSTEM", data.compounds("system_inventory").size() + "/108 items  •  "
                + data.compounds("shadows").size() + "/" + data.shadowCapacity() + " shadows");
    }

    private void homeCard(GuiGraphics graphics, int y, String heading, String detail) {
        int height = Math.max(24, Math.min(44, (contentHeight - 8) / 3));
        SystemUi.Theme.inset(graphics, contentX, y, contentWidth, height);
        graphics.drawString(font, heading, contentX + 7, y + 7, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, detail, contentWidth - 14), contentX + 7, y + 20, SystemUi.Theme.TEXT_DIM, false);
    }

    private void renderStats(GuiGraphics graphics, SystemUi.Data data) {
        graphics.drawString(font, "Level " + data.level() + "   " + data.rank(), contentX, contentY + 2, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, "Unspent points: " + data.statPoints(), contentX + contentWidth - font.width("Unspent points: " + data.statPoints()),
                contentY + 2, data.statPoints() > 0 ? SystemUi.Theme.WARNING : SystemUi.Theme.TEXT_DIM, false);
        for (int i = 0; i < STATS.length; i++) {
            String stat = STATS[i];
            int y = contentY + 18 + i * statRowStep();
            SystemUi.Theme.inset(graphics, contentX, y - 4, contentWidth, 18);
            graphics.drawString(font, SystemUi.titleCase(stat), contentX + 7, y, SystemUi.Theme.TEXT, false);
            String value = Integer.toString(data.stat(stat));
            graphics.drawString(font, value, contentX + contentWidth - 48 - font.width(value), y, SystemUi.Theme.CYAN, false);
        }
        if (contentHeight >= 150) {
            graphics.drawString(font, "Mana " + data.mana() + " / " + data.maxMana(), contentX, contentY + 129, SystemUi.Theme.VIOLET, false);
            graphics.drawString(font, "Job: " + data.text("job", "None") + "   Title: " + data.text("title", "The Player"), contentX, contentY + 143, SystemUi.Theme.TEXT_DIM, false);
        }
    }

    private void renderSkills(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        int listBottom = contentY + contentHeight - 25;
        int rowHeight = 21;
        int visible = Math.max(1, (listBottom - contentY) / rowHeight);
        int maximum = Math.max(0, HunterData.SKILLS.length - visible);
        int scroll = clampScroll(maximum);
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, listBottom);
        for (int row = 0; row < visible && row + scroll < HunterData.SKILLS.length; row++) {
            String skill = HunterData.SKILLS[row + scroll];
            int y = contentY + row * rowHeight;
            boolean selected = skill.equals(selectedSkill);
            boolean unlocked = data.skillUnlocked(skill);
            graphics.fill(contentX, y, contentX + contentWidth, y + 19, selected ? 0x88365A86 : 0x66070D1B);
            graphics.fill(contentX, y, contentX + 2, y + 19, unlocked ? SystemUi.Theme.CYAN : 0xFF39465A);
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, SystemUi.titleCase(skill), contentWidth - 85), contentX + 7, y + 6,
                    unlocked ? SystemUi.Theme.TEXT : SystemUi.Theme.TEXT_DIM, false);
            String state = !unlocked ? "LOCKED" : (data.cooldownRemaining(skill) > 0L ? SystemUi.Cooldowns.remainingText(data, skill) : (isActiveSkill(skill) ? "READY" : "PASSIVE"));
            graphics.drawString(font, state, contentX + contentWidth - font.width(state) - 6, y + 6,
                    !unlocked ? SystemUi.Theme.TEXT_DIM : data.cooldownRemaining(skill) > 0L ? SystemUi.Theme.WARNING : SystemUi.Theme.SUCCESS, false);
        }
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, contentY, listBottom - contentY, scroll, maximum);
    }

    private void renderQuests(GuiGraphics graphics, SystemUi.Data data) {
        int viewportHeight = Math.max(30, contentHeight - 25);
        int totalHeight = 154;
        int maximum = Math.max(0, totalHeight - viewportHeight);
        int scroll = Math.max(0, Math.min(maximum, scrollOffsets.getOrDefault(tab, 0)));
        scrollOffsets.put(tab, scroll);
        int y = contentY - scroll;
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + viewportHeight);
        SystemUi.Theme.inset(graphics, contentX, y, contentWidth, 34);
        graphics.drawString(font, data.activeQuestName(), contentX + 7, y + 6, SystemUi.Theme.VIOLET, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, data.activeQuestProgress(), contentWidth - 14), contentX + 7, y + 19, SystemUi.Theme.TEXT_DIM, false);
        y += 40;
        graphics.drawString(font, "DAILY — PREPARATION TO BECOME POWERFUL", contentX, y, SystemUi.Theme.CYAN, false);
        y += 14;
        objective(graphics, y, "Hostile mobs", data.raw().getInt("daily_kills"), 10); y += 14;
        objective(graphics, y, "Run blocks", data.raw().getInt("daily_run"), 1000); y += 14;
        objective(graphics, y, "Push-ups", data.raw().getInt("daily_pushups"), 30); y += 14;
        objective(graphics, y, "Sit-ups", data.raw().getInt("daily_situps"), 30); y += 14;
        objective(graphics, y, "Squats", data.raw().getInt("daily_squats"), 30); y += 16;
        String state = data.dailyClaimed() ? "REWARD CLAIMED" : data.dailyComplete() ? "COMPLETE — CLAIM AVAILABLE" : "IN PROGRESS";
        graphics.drawString(font, state, contentX, y, data.dailyComplete() ? SystemUi.Theme.SUCCESS : SystemUi.Theme.WARNING, false);
        if (data.raw().getBoolean("emergency_active")) {
            graphics.drawString(font, "EMERGENCY QUEST: " + data.raw().getInt("emergency_kills") + " / 3 kills", contentX, y + 14, SystemUi.Theme.FAILURE, false);
        }
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, contentY, viewportHeight, scroll, maximum);
    }

    private void objective(GuiGraphics graphics, int y, String label, int value, int target) {
        int safe = Math.max(0, Math.min(value, target));
        String text = label + "  " + safe + " / " + target;
        graphics.drawString(font, text, contentX + 6, y, safe >= target ? SystemUi.Theme.SUCCESS : SystemUi.Theme.TEXT, false);
    }

    private void renderStore(GuiGraphics graphics, SystemUi.Data data) {
        graphics.drawString(font, "System gold: " + data.gold(), contentX, contentY + 4, SystemUi.Theme.WARNING, false);
        graphics.drawString(font, "Purchases are validated and stored by the server.", contentX, contentY + 17, SystemUi.Theme.TEXT_DIM, false);
        if (data.compounds("system_inventory").size() >= 108) {
            graphics.drawString(font, "System inventory is full.", contentX, contentY + contentHeight - 10, SystemUi.Theme.FAILURE, false);
        }
    }

    private void renderEquipment(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            graphics.drawString(font, "Player equipment is unavailable.", contentX, contentY + 8, SystemUi.Theme.FAILURE, false);
            return;
        }
        boolean sideBySide = contentWidth >= 310;
        int totalHeight = sideBySide ? 116 : 196;
        int maximum = Math.max(0, totalHeight - contentHeight);
        int scroll = Math.max(0, Math.min(maximum, scrollOffsets.getOrDefault(tab, 0)));
        scrollOffsets.put(tab, scroll);
        int originY = contentY - scroll;
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        String[] labels = {"Helmet", "Chest", "Legs", "Boots"};
        int[] armorSlots = {3, 2, 1, 0};
        for (int i = 0; i < labels.length; i++) {
            int y = originY + i * 28;
            ItemStack stack = minecraft.player.getInventory().armor.get(armorSlots[i]);
            SystemUi.Theme.inset(graphics, contentX, y, Math.min(220, contentWidth), 24);
            graphics.renderItem(stack, contentX + 4, y + 4);
            String name = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
            graphics.drawString(font, labels[i] + ": " + SystemUi.Theme.ellipsize(font, name, Math.min(220, contentWidth) - 60), contentX + 25, y + 8,
                    stack.isEmpty() ? SystemUi.Theme.TEXT_DIM : SystemUi.Theme.TEXT, false);
            if (!stack.isEmpty() && mouseX >= contentX && mouseX < contentX + Math.min(220, contentWidth)
                    && mouseY >= Math.max(contentY, y) && mouseY < Math.min(contentY + contentHeight, y + 24)) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
        }
        int accessoryX = sideBySide ? contentX + 232 : contentX;
        int accessoryY = sideBySide ? originY : originY + 116;
        String[] slots = {"hands", "earring", "necklace", "ring"};
        for (int i = 0; i < slots.length; i++) {
            String value = data.raw().getString("accessory_" + slots[i]);
            int availableWidth = Math.max(80, contentWidth - (accessoryX - contentX) - 10);
            graphics.drawString(font, SystemUi.titleCase(slots[i]) + ": " + SystemUi.Theme.ellipsize(font,
                    value.isBlank() ? "Empty" : SystemUi.titleCase(value), availableWidth),
                    accessoryX, accessoryY + i * 18, value.isBlank() ? SystemUi.Theme.TEXT_DIM : SystemUi.Theme.VIOLET, false);
        }
        graphics.drawString(font, "Accessories equip when their item is used.", contentX, originY + totalHeight - 12, SystemUi.Theme.TEXT_DIM, false);
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, contentY, contentHeight, scroll, maximum);
    }

    private void renderInventory(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        ListTag list = data.compounds("system_inventory");
        int columns = inventoryColumns();
        int rows = inventoryRows();
        int pageSize = columns * rows;
        int start = inventoryPage * pageSize;
        int hovered = -1;
        for (int slot = 0; slot < pageSize; slot++) {
            int x = contentX + (slot % columns) * 20;
            int y = contentY + (slot / columns) * 20;
            SystemUi.Theme.inset(graphics, x, y, 18, 18);
            int index = start + slot;
            if (index < list.size()) {
                ItemStack stack = ItemStack.of(list.getCompound(index));
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 1, y + 1);
                    graphics.renderItemDecorations(font, stack, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) hovered = index;
                }
            }
        }
        int summaryX = contentX + columns * 20 + 5;
        if (summaryX + 70 < contentX + contentWidth) {
            graphics.drawString(font, list.size() + " / 108", summaryX, contentY + 2, SystemUi.Theme.CYAN, false);
            graphics.drawString(font, "Page " + (inventoryPage + 1) + " / " + (maxInventoryPage(data) + 1), summaryX, contentY + 17, SystemUi.Theme.TEXT_DIM, false);
            graphics.drawString(font, "Click an item", summaryX, contentY + 40, SystemUi.Theme.TEXT_DIM, false);
            graphics.drawString(font, "to retrieve it.", summaryX, contentY + 53, SystemUi.Theme.TEXT_DIM, false);
        }
        if (list.isEmpty()) graphics.drawCenteredString(font, "System inventory is empty", contentX + Math.min(contentWidth, columns * 20) / 2, contentY + rows * 10 - 4, SystemUi.Theme.TEXT_DIM);
        if (hovered >= 0) graphics.renderTooltip(font, ItemStack.of(list.getCompound(hovered)), mouseX, mouseY);
    }

    private void renderShadows(GuiGraphics graphics, SystemUi.Data data) {
        ListTag shadows = data.compounds("shadows");
        int headerHeight = 35;
        graphics.drawString(font, "Stored " + shadows.size() + " / " + data.shadowCapacity() + "   Active " + data.strings("active_shadows").size(),
                contentX, contentY + 2, SystemUi.Theme.VIOLET, false);
        String[] modes = {"Follow", "Guard", "Passive", "Aggressive"};
        graphics.drawString(font, "Mode: " + modes[Math.floorMod(data.raw().getInt("shadow_mode"), modes.length)], contentX, contentY + 16, SystemUi.Theme.TEXT_DIM, false);
        int listTop = contentY + headerHeight;
        int listBottom = contentY + contentHeight - 48;
        int rowHeight = 24;
        int visible = Math.max(1, (listBottom - listTop) / rowHeight);
        int maximum = Math.max(0, shadows.size() - visible);
        int scroll = clampScroll(maximum);
        graphics.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        for (int row = 0; row < visible && row + scroll < shadows.size(); row++) {
            CompoundTag shadow = shadows.getCompound(row + scroll);
            int y = listTop + row * rowHeight;
            SystemUi.Theme.inset(graphics, contentX, y, contentWidth, 21);
            String name = shadow.getString("name").isBlank() ? "Unnamed Shadow" : shadow.getString("name");
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, name, contentWidth - 95), contentX + 6, y + 6, SystemUi.Theme.TEXT, false);
            String details = "Lv" + Math.max(1, shadow.getInt("level")) + " " + (shadow.getString("rank").isBlank() ? "Normal" : shadow.getString("rank"));
            graphics.drawString(font, details, contentX + contentWidth - font.width(details) - 6, y + 6, SystemUi.Theme.CYAN, false);
        }
        graphics.disableScissor();
        if (shadows.isEmpty()) graphics.drawCenteredString(font, "No preserved shadows", contentX + contentWidth / 2, listTop + 18, SystemUi.Theme.TEXT_DIM);
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, listTop, listBottom - listTop, scroll, maximum);
    }

    private void renderSettings(GuiGraphics graphics, SystemUi.Data data) {
        graphics.drawString(font, "Local HUD presentation settings are stored in the client config folder.", contentX, contentY + 4, SystemUi.Theme.TEXT_DIM, false);
        graphics.drawString(font, "Scale: " + SystemUi.Settings.hudScale() + "%", contentX, contentY + contentHeight - 25, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, "Server HUD controls whether the synchronized overlay is visible.", contentX, contentY + contentHeight - 12, SystemUi.Theme.TEXT_DIM, false);
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y, int height, int scroll, int maximum) {
        graphics.fill(x, y, x + 2, y + height, 0x552A3B52);
        int thumb = Math.max(8, height / Math.max(2, maximum + 1));
        int offset = maximum == 0 ? 0 : Math.round((height - thumb) * (scroll / (float)maximum));
        graphics.fill(x, y + offset, x + 2, y + offset + thumb, SystemUi.Theme.CYAN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && ClientHunterData.awakened()) {
            if (tab == Tab.SKILLS && clickSkill(mouseX, mouseY)) return true;
            if (tab == Tab.INVENTORY && clickInventory(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickSkill(double mouseX, double mouseY) {
        int listBottom = contentY + contentHeight - 25;
        if (mouseX < contentX || mouseX >= contentX + contentWidth || mouseY < contentY || mouseY >= listBottom) return false;
        int visible = Math.max(1, (listBottom - contentY) / 21);
        int maximum = Math.max(0, HunterData.SKILLS.length - visible);
        int index = clampScroll(maximum) + ((int)mouseY - contentY) / 21;
        if (index < 0 || index >= HunterData.SKILLS.length) return false;
        selectedSkill = HunterData.SKILLS[index];
        refreshButtonStates();
        return true;
    }

    private boolean clickInventory(double mouseX, double mouseY) {
        int columns = inventoryColumns();
        int rows = inventoryRows();
        int gridWidth = columns * 20;
        int gridHeight = rows * 20;
        int relativeX = (int)mouseX - contentX;
        int relativeY = (int)mouseY - contentY;
        if (relativeX < 0 || relativeY < 0 || relativeX >= gridWidth || relativeY >= gridHeight) return false;
        int column = relativeX / 20;
        int row = relativeY / 20;
        if (relativeX % 20 >= 18 || relativeY % 20 >= 18) return false;
        int index = inventoryPage * columns * rows + row * columns + column;
        if (index >= ClientHunterData.view().compounds("system_inventory").size()) return false;
        send("RETRIEVE_SLOT:" + index);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == Tab.SKILLS || tab == Tab.SHADOWS) {
            int current = scrollOffsets.getOrDefault(tab, 0);
            scrollOffsets.put(tab, Math.max(0, current + (delta > 0 ? -1 : 1)));
            return true;
        }
        if (tab == Tab.QUESTS || tab == Tab.EQUIPMENT) {
            int current = scrollOffsets.getOrDefault(tab, 0);
            scrollOffsets.put(tab, Math.max(0, current + (delta > 0 ? -12 : 12)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int clampScroll(int maximum) {
        int value = Math.max(0, Math.min(maximum, scrollOffsets.getOrDefault(tab, 0)));
        scrollOffsets.put(tab, value);
        return value;
    }

    private int statRowStep() {
        return Math.max(14, Math.min(20, Math.max(14, contentHeight - 24) / 5));
    }

    private int inventoryColumns() {
        int available = Math.max(80, contentWidth - (contentWidth >= 260 ? 90 : 0));
        return Math.max(4, Math.min(9, available / 20));
    }

    private int inventoryRows() {
        return Math.max(2, Math.min(6, Math.max(40, contentHeight - 25) / 20));
    }

    private int maxInventoryPage(SystemUi.Data data) {
        int pageSize = Math.max(1, inventoryColumns() * inventoryRows());
        int count = data.compounds("system_inventory").size();
        return Math.max(0, (count - 1) / pageSize);
    }

    private static boolean isActiveSkill(String skill) {
        for (String value : ACTIVE_SKILLS) if (value.equals(skill)) return true;
        return false;
    }

    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

    private static String tabName(Tab tab) {
        return switch (tab) {
            case HOME -> "Main";
            case STATUS -> "Stats";
            case SKILLS -> "Skills";
            case QUESTS -> "Quests";
            case STORE -> "Store";
            case EQUIPMENT -> "Equipment";
            case INVENTORY -> "Inventory";
            case SHADOWS -> "Shadows";
            case SETTINGS -> "HUD Settings";
        };
    }

    private static void send(String action) {
        ModNetwork.CHANNEL.sendToServer(new ActionPacket(action));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record StoreButton(Button button, int price) { }
    @FunctionalInterface private interface LabelSupplier { String get(); }
}
