package com.tre.sololeveling.client.screen;

import com.tre.sololeveling.client.ClientHunterData;
import com.tre.sololeveling.client.ClientKeyMappings;
import com.tre.sololeveling.client.ui.SystemUi;
import com.tre.sololeveling.client.ui.component.SystemButton;
import com.tre.sololeveling.client.ui.component.SystemTabButton;
import com.tre.sololeveling.gameplay.ability.Ability;
import com.tre.sololeveling.gameplay.ability.AbilityDefinition;
import com.tre.sololeveling.gameplay.ability.AbilityService;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Responsive, server-authoritative entry point for the Solo Leveling System UI. */
public final class SystemScreen extends Screen {
    public enum Tab {
        HOME("Player", SystemUi.Icon.PLAYER),
        STATUS("Stats", SystemUi.Icon.STATS),
        SKILLS("Abilities", SystemUi.Icon.ABILITIES),
        INVENTORY("Inventory", SystemUi.Icon.INVENTORY),
        EQUIPMENT("Equipment", SystemUi.Icon.EQUIPMENT),
        QUESTS("Quests", SystemUi.Icon.QUESTS),
        STORE("Shop", SystemUi.Icon.SHOP),
        DUNGEONS("Dungeons", SystemUi.Icon.DUNGEONS),
        SHADOWS("Shadows", SystemUi.Icon.SHADOWS),
        SETTINGS("Settings", SystemUi.Icon.SETTINGS);

        private final String label;
        private final SystemUi.Icon icon;
        Tab(String label, SystemUi.Icon icon) { this.label = label; this.icon = icon; }
        public String label() { return label; }
        public SystemUi.Icon icon() { return icon; }
    }

    private static final String[] STATS = {"strength", "agility", "stamina", "intelligence", "sense"};
    private static final Map<String, String> STAT_DESCRIPTIONS = Map.of(
            "strength", "Increases physical and dagger damage.",
            "agility", "Improves speed, evasion, and movement abilities.",
            "stamina", "Raises maximum health and durability.",
            "intelligence", "Raises mana, regeneration, and shadow capacity.",
            "sense", "Improves critical chance, awareness, and precision."
    );
    private static final List<StoreProduct> STORE = List.of(
            new StoreProduct("healing_potion", 50, "Restores health during difficult encounters."),
            new StoreProduct("mana_potion", 60, "Restores mana for active abilities."),
            new StoreProduct("greater_healing_potion", 150, "A stronger emergency healing option."),
            new StoreProduct("greater_mana_potion", 175, "Restores a large amount of mana."),
            new StoreProduct("knight_killer", 1500, "A specialized anti-armor dagger."),
            new StoreProduct("blessed_random_box", 750, "Contains one server-selected System reward.")
    );
    private static final List<DungeonCard> DUNGEONS = List.of(
            new DungeonCard("Abyssal Necropolis", "A", 40,
                    "A vast buried kingdom with six regions, deep vertical routes, secrets, and the Abyssal Monarch.")
    );

    private final EnumMap<Tab, Integer> scrollOffsets = new EnumMap<>(Tab.class);
    private final Map<String, SystemButton> actionButtons = new HashMap<>();
    private final List<StoreButton> storeButtons = new ArrayList<>();
    private final List<AbilityDefinition> abilities;
    private Tab tab;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int navigationWidth;
    private boolean sideNavigation;
    private boolean lastAwakened;
    private int lastDataHash;
    private int inventoryPage;
    private String selectedAbility;

    public SystemScreen(Tab requestedTab) {
        super(Component.translatable("screen.sololeveling.system"));
        this.tab = requestedTab == null ? rememberedTab() : requestedTab;
        this.abilities = AbilityService.registry().all().stream()
                .map(Ability::definition)
                .sorted(Comparator.comparing(AbilityDefinition::category).thenComparing(AbilityDefinition::displayName))
                .toList();
        this.selectedAbility = abilities.isEmpty() ? "" : abilities.get(0).id();
        for (Tab value : Tab.values()) scrollOffsets.put(value, 0);
    }

    @Override
    protected void init() {
        actionButtons.clear();
        storeButtons.clear();
        panelWidth = Math.min(760, Math.max(272, width - 16));
        panelHeight = Math.min(430, Math.max(210, height - 16));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        sideNavigation = panelWidth >= 520 && panelHeight >= 270;
        navigationWidth = sideNavigation ? 126 : 0;
        contentX = left + (sideNavigation ? navigationWidth + 14 : 12);
        contentY = top + (sideNavigation ? 40 : 82);
        contentWidth = panelWidth - (sideNavigation ? navigationWidth + 26 : 24);
        contentHeight = panelHeight - (sideNavigation ? 52 : 94);
        lastAwakened = ClientHunterData.awakened();
        lastDataHash = ClientHunterData.get().hashCode();
        addNavigation();

        if (!lastAwakened) {
            SystemButton awaken = new SystemButton(left + panelWidth / 2 - 76, top + panelHeight / 2 + 10,
                    152, 22, Component.literal("AWAKEN SYSTEM"), () -> SystemUi.Actions.send("AWAKEN", 750L), true);
            actionButtons.put("awaken", awaken);
            addRenderableWidget(awaken);
            return;
        }

        switch (tab) {
            case HOME, EQUIPMENT, DUNGEONS -> { }
            case STATUS -> addStatButtons();
            case SKILLS -> addAbilityButtons();
            case INVENTORY -> addInventoryButtons();
            case QUESTS -> addQuestButtons();
            case STORE -> addStoreButtons();
            case SHADOWS -> addShadowButtons();
            case SETTINGS -> addSettingsButtons();
        }
        refreshButtonStates();
    }

    private void addNavigation() {
        if (sideNavigation) {
            int y = top + 39;
            for (Tab value : Tab.values()) {
                addTab(value, left + 9, y, navigationWidth - 18, 22);
                y += 24;
            }
        } else {
            int columns = 5;
            int gap = 2;
            int buttonWidth = (panelWidth - 24 - gap * (columns - 1)) / columns;
            for (int i = 0; i < Tab.values().length; i++) {
                int column = i % columns;
                int row = i / columns;
                addTab(Tab.values()[i], left + 12 + column * (buttonWidth + gap), top + 27 + row * 23, buttonWidth, 21);
            }
        }
    }

    private void addTab(Tab target, int x, int y, int width, int height) {
        SystemTabButton button = new SystemTabButton(x, y, width, height, Component.literal(target.label()), target.icon(), () -> switchTab(target));
        button.setSelected(target == tab);
        addRenderableWidget(button);
    }

    private void switchTab(Tab target) {
        if (target == tab) return;
        tab = target;
        inventoryPage = 0;
        SystemUi.Settings.setLastTab(target.name());
        rebuildWidgets();
    }

    private void addStatButtons() {
        int step = statRowStep();
        for (int i = 0; i < STATS.length; i++) {
            String stat = STATS[i];
            int y = contentY + 22 + i * step;
            SystemButton plus = new SystemButton(contentX + contentWidth - 48, y, 20, 18, Component.literal("+"),
                    () -> SystemUi.Actions.send("ALLOCATE:" + stat, 180L));
            SystemButton plusFive = new SystemButton(contentX + contentWidth - 25, y, 24, 18, Component.literal("+5"),
                    () -> confirm("SPEND FIVE POINTS", "Increase " + SystemUi.titleCase(stat) + " by five points?",
                            () -> SystemUi.Actions.send("ALLOCATE5:" + stat, 400L)), true);
            plus.disabledReason("No unspent stat points are available.");
            plusFive.disabledReason("Five unspent stat points are required.");
            actionButtons.put("stat:" + stat, plus);
            actionButtons.put("stat5:" + stat, plusFive);
            addRenderableWidget(plus);
            addRenderableWidget(plusFive);
        }
    }

    private void addAbilityButtons() {
        int bottom = contentY + contentHeight;
        int half = Math.max(72, (contentWidth - 4) / 2);
        SystemButton activate = new SystemButton(contentX, bottom - 20, half, 18, Component.literal("ACTIVATE"), () -> {
            AbilityDefinition definition = selectedDefinition();
            if (definition != null) SystemUi.Actions.send(actionForAbility(definition.id()), 220L);
        }, true);
        activate.disabledReason("The ability is locked, cooling down, passive, or lacks enough mana.");
        actionButtons.put("ability:activate", activate);
        addRenderableWidget(activate);

        SystemButton controls = new SystemButton(contentX + half + 4, bottom - 20, contentWidth - half - 4, 18,
                Component.literal("EDIT KEYBINDS"), () -> {
                    if (minecraft != null) minecraft.setScreen(new ControlsScreen(this, minecraft.options));
                });
        addRenderableWidget(controls);

        int slotWidth = Math.max(22, Math.min(30, (contentWidth - 10) / SystemUi.ACTION_SLOT_COUNT));
        int slotY = bottom - 42;
        for (int slot = 0; slot < SystemUi.ACTION_SLOT_COUNT; slot++) {
            int index = slot;
            SystemButton button = new SystemButton(contentX + slot * (slotWidth + 2), slotY, slotWidth, 18,
                    Component.literal(Integer.toString(slot + 1)), () -> toggleAbilitySlot(index));
            actionButtons.put("ability:slot:" + slot, button);
            addRenderableWidget(button);
        }
        SystemButton reset = new SystemButton(contentX + contentWidth - 68, slotY, 68, 18, Component.literal("RESET KEYS"), () -> {
            ClientKeyMappings.resetAbilityBindings();
            SystemUi.Notifications.pushInfo("KEYBINDS RESET", "Ability controls restored to defaults");
            rebuildWidgets();
        });
        addRenderableWidget(reset);
    }

    private void toggleAbilitySlot(int slot) {
        if (selectedAbility.isBlank()) return;
        if (selectedAbility.equals(SystemUi.Settings.abilitySlot(slot))) SystemUi.Settings.clearAbility(slot);
        else SystemUi.Settings.assignAbility(slot, selectedAbility);
        rebuildWidgets();
    }

    private void addInventoryButtons() {
        int y = contentY + contentHeight - 20;
        SystemButton store = new SystemButton(contentX, y, Math.min(108, contentWidth / 2), 18,
                Component.literal("STORE HELD ITEM"), () -> SystemUi.Actions.send("STORE_HELD", 300L));
        store.disabledReason("Hold an item and ensure System storage has room.");
        SystemButton previous = new SystemButton(contentX + contentWidth - 50, y, 23, 18, Component.literal("<"), () -> {
            inventoryPage = Math.max(0, inventoryPage - 1); refreshButtonStates();
        });
        SystemButton next = new SystemButton(contentX + contentWidth - 25, y, 23, 18, Component.literal(">"), () -> {
            inventoryPage = Math.min(maxInventoryPage(ClientHunterData.view()), inventoryPage + 1); refreshButtonStates();
        });
        actionButtons.put("inventory:store", store);
        actionButtons.put("inventory:previous", previous);
        actionButtons.put("inventory:next", next);
        addRenderableWidget(store); addRenderableWidget(previous); addRenderableWidget(next);
    }

    private void addQuestButtons() {
        SystemButton claim = new SystemButton(contentX, contentY + contentHeight - 20, Math.min(166, contentWidth), 18,
                Component.literal("CLAIM DAILY REWARD"), () -> SystemUi.Actions.send("CLAIM_DAILY", 600L), true);
        claim.disabledReason("Complete every daily objective before claiming the reward.");
        actionButtons.put("quest:claim", claim);
        addRenderableWidget(claim);
    }

    private void addStoreButtons() {
        int columns = contentWidth >= 420 ? 2 : 1;
        int buttonWidth = (contentWidth - (columns - 1) * 6) / columns;
        for (int i = 0; i < STORE.size(); i++) {
            StoreProduct product = STORE.get(i);
            int column = i % columns;
            int row = i / columns;
            int y = contentY + 34 + row * 38;
            SystemButton button = new SystemButton(contentX + column * (buttonWidth + 6), y, buttonWidth, 24,
                    Component.literal(product.name() + "  •  " + product.price + "G"), () -> buy(product));
            button.disabledReason("Requires " + product.price + " gold and free System storage.");
            storeButtons.add(new StoreButton(button, product));
            addRenderableWidget(button);
        }
    }

    private void buy(StoreProduct product) {
        Runnable purchase = () -> SystemUi.Actions.send("BUY:" + product.id, 650L);
        if (product.price >= 500) confirm("CONFIRM PURCHASE", product.name() + " costs " + product.price + " gold. Continue?", purchase);
        else purchase.run();
    }

    private void addShadowButtons() {
        String[] labels = {"EXTRACT", "SUMMON", "DISMISS", "EXCHANGE", "DOMAIN", "MODE"};
        String[] actions = {"EXTRACT", "SUMMON_SHADOW", "DISMISS_SHADOWS", "SHADOW_EXCHANGE", "TOGGLE_DOMAIN", "SHADOW_MODE"};
        int columns = contentWidth >= 360 ? 3 : 2;
        int buttonWidth = (contentWidth - (columns - 1) * 4) / columns;
        int rows = (labels.length + columns - 1) / columns;
        int startY = contentY + contentHeight - rows * 22;
        for (int i = 0; i < labels.length; i++) {
            int column = i % columns;
            int row = i / columns;
            String action = actions[i];
            SystemButton button = new SystemButton(contentX + column * (buttonWidth + 4), startY + row * 22,
                    buttonWidth, 18, Component.literal(labels[i]), () -> SystemUi.Actions.send(action, 250L));
            button.disabledReason("This shadow action is currently unavailable.");
            actionButtons.put("shadow:" + action, button);
            addRenderableWidget(button);
        }
    }

    private void addSettingsButtons() {
        List<SettingAction> settings = List.of(
                new SettingAction(() -> "VITALS: " + onOff(SystemUi.Settings.showVitals()), SystemUi.Settings::toggleVitals),
                new SettingAction(() -> "GOLD: " + onOff(SystemUi.Settings.showGold()), SystemUi.Settings::toggleGold),
                new SettingAction(() -> "QUEST TRACKER: " + onOff(SystemUi.Settings.showQuestTracker()), SystemUi.Settings::toggleQuestTracker),
                new SettingAction(() -> "ABILITY BAR: " + onOff(SystemUi.Settings.showAbilityBar()), SystemUi.Settings::toggleAbilityBar),
                new SettingAction(() -> "ABILITY NAMES: " + onOff(SystemUi.Settings.showAbilityNames()), SystemUi.Settings::toggleAbilityNames),
                new SettingAction(() -> "NOTIFICATIONS: " + onOff(SystemUi.Settings.showNotifications()), SystemUi.Settings::toggleNotifications),
                new SettingAction(() -> "COMPACT: " + onOff(SystemUi.Settings.compactHud()), SystemUi.Settings::toggleCompactHud),
                new SettingAction(() -> "MINIMAL: " + onOff(SystemUi.Settings.minimalHud()), SystemUi.Settings::toggleMinimalHud),
                new SettingAction(() -> "ANCHOR: " + SystemUi.titleCase(SystemUi.Settings.anchor().name()), SystemUi.Settings::cycleAnchor),
                new SettingAction(() -> "SCALE -", () -> SystemUi.Settings.adjustScale(-5)),
                new SettingAction(() -> "SCALE +", () -> SystemUi.Settings.adjustScale(5)),
                new SettingAction(() -> "OPACITY -", () -> SystemUi.Settings.adjustOpacity(-5)),
                new SettingAction(() -> "OPACITY +", () -> SystemUi.Settings.adjustOpacity(5)),
                new SettingAction(() -> "MOVE LEFT", () -> SystemUi.Settings.adjustOffset(-5, 0)),
                new SettingAction(() -> "MOVE RIGHT", () -> SystemUi.Settings.adjustOffset(5, 0)),
                new SettingAction(() -> "MOVE UP", () -> SystemUi.Settings.adjustOffset(0, -5)),
                new SettingAction(() -> "MOVE DOWN", () -> SystemUi.Settings.adjustOffset(0, 5)),
                new SettingAction(() -> "RESET DEFAULTS", SystemUi.Settings::resetDefaults)
        );
        int columns = contentWidth >= 380 ? 3 : 2;
        int gap = 4;
        int buttonWidth = (contentWidth - gap * (columns - 1)) / columns;
        for (int i = 0; i < settings.size(); i++) {
            SettingAction entry = settings.get(i);
            int column = i % columns;
            int row = i / columns;
            int y = contentY + row * 21;
            SystemButton button = new SystemButton(contentX + column * (buttonWidth + gap), y, buttonWidth, 18,
                    Component.literal(entry.label.get()), () -> { entry.action.run(); rebuildWidgets(); });
            addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        int currentHash = ClientHunterData.get().hashCode();
        if (lastAwakened != ClientHunterData.awakened() || currentHash != lastDataHash) {
            lastAwakened = ClientHunterData.awakened();
            lastDataHash = currentHash;
            refreshButtonStates();
        } else {
            refreshButtonStates();
        }
    }

    private void refreshButtonStates() {
        SystemUi.Data data = ClientHunterData.view();
        for (String stat : STATS) {
            setActive("stat:" + stat, data.statPoints() > 0);
            setActive("stat5:" + stat, data.statPoints() >= 5);
        }
        AbilityDefinition selected = selectedDefinition();
        SystemButton activate = actionButtons.get("ability:activate");
        if (activate != null) {
            boolean unlocked = selected != null && isUnlocked(data, selected);
            long remaining = selected == null ? 0L : data.cooldownRemaining(selected.id());
            boolean enoughMana = selected != null && data.mana() >= selected.manaCost();
            activate.active = selected != null && unlocked && remaining <= 0L && enoughMana;
            activate.setMessage(Component.literal(selected == null ? "NO ABILITY" : remaining > 0L
                    ? SystemUi.Cooldowns.remainingText(data, selected.id()) : "ACTIVATE"));
        }
        for (int slot = 0; slot < SystemUi.ACTION_SLOT_COUNT; slot++) {
            SystemButton button = actionButtons.get("ability:slot:" + slot);
            if (button != null) {
                boolean selectedHere = selectedAbility.equals(SystemUi.Settings.abilitySlot(slot));
                button.setMessage(Component.literal(selectedHere ? "✓" + (slot + 1) : Integer.toString(slot + 1)));
            }
        }
        setActive("quest:claim", data.dailyComplete() && !data.dailyClaimed());
        for (StoreButton entry : storeButtons) entry.button.active = data.gold() >= entry.product.price && data.compounds("system_inventory").size() < 108;
        SystemButton store = actionButtons.get("inventory:store");
        if (store != null) {
            Minecraft client = Minecraft.getInstance();
            store.active = client.player != null && !client.player.getMainHandItem().isEmpty() && data.compounds("system_inventory").size() < 108;
        }
        int maximumPage = maxInventoryPage(data);
        inventoryPage = Math.min(inventoryPage, maximumPage);
        setActive("inventory:previous", inventoryPage > 0);
        setActive("inventory:next", inventoryPage < maximumPage);
        setActive("shadow:EXTRACT", data.skillUnlocked("shadow_extraction") && data.cooldownRemaining("shadow_extraction") <= 0L);
        setActive("shadow:SUMMON_SHADOW", data.skillUnlocked("shadow_preservation") && !data.compounds("shadows").isEmpty());
        setActive("shadow:DISMISS_SHADOWS", !data.strings("active_shadows").isEmpty());
        setActive("shadow:SHADOW_EXCHANGE", data.skillUnlocked("shadow_exchange") && !data.strings("active_shadows").isEmpty() && data.cooldownRemaining("shadow_exchange") <= 0L);
        setActive("shadow:TOGGLE_DOMAIN", data.skillUnlocked("monarch_domain"));
        setActive("shadow:SHADOW_MODE", !data.compounds("shadows").isEmpty());
    }

    private void setActive(String key, boolean active) {
        SystemButton button = actionButtons.get(key);
        if (button != null) button.active = active;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, SystemUi.Theme.BACKDROP);
        SystemUi.Theme.panel(graphics, left, top, panelWidth, panelHeight);
        SystemUi.Theme.icon(graphics, SystemUi.Icon.PLAYER, left + 11, top + 10, 16);
        graphics.drawString(font, "SYSTEM", left + 32, top + 14, SystemUi.Theme.CYAN, false);
        String subtitle = ClientHunterData.awakened() ? "PLAYER INTERFACE  •  " + tab.label().toUpperCase(Locale.ROOT) : "AWAKENING REQUIRED";
        graphics.drawString(font, subtitle, left + panelWidth - font.width(subtitle) - 12, top + 14, SystemUi.Theme.TEXT_DIM, false);
        if (sideNavigation) graphics.fill(left + navigationWidth, top + 34, left + navigationWidth + 1, top + panelHeight - 10, 0x6635D9FF);

        if (!ClientHunterData.awakened()) {
            graphics.drawCenteredString(font, "The System is dormant.", left + panelWidth / 2, top + panelHeight / 2 - 24, SystemUi.Theme.TEXT);
            graphics.drawCenteredString(font, "Authorize awakening to begin Hunter progression.", left + panelWidth / 2, top + panelHeight / 2 - 10, SystemUi.Theme.TEXT_DIM);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        SystemUi.Theme.section(graphics, font, tab.label(), contentX, contentY - 15, contentWidth);
        SystemUi.Data data = ClientHunterData.view();
        switch (tab) {
            case HOME -> renderHome(graphics, data);
            case STATUS -> renderStats(graphics, data, mouseX, mouseY);
            case SKILLS -> renderAbilities(graphics, data, mouseX, mouseY);
            case INVENTORY -> renderInventory(graphics, data, mouseX, mouseY);
            case EQUIPMENT -> renderEquipment(graphics, data, mouseX, mouseY);
            case QUESTS -> renderQuests(graphics, data);
            case STORE -> renderStore(graphics, data, mouseX, mouseY);
            case DUNGEONS -> renderDungeons(graphics, data);
            case SHADOWS -> renderShadows(graphics, data);
            case SETTINGS -> renderSettings(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderDisabledTooltip(graphics, mouseX, mouseY);
    }

    private void renderHome(GuiGraphics graphics, SystemUi.Data data) {
        int gap = 6;
        int topHeight = Math.max(82, Math.min(112, contentHeight / 2));
        int leftWidth = contentWidth >= 430 ? (contentWidth * 3) / 5 : contentWidth;
        SystemUi.Theme.inset(graphics, contentX, contentY, leftWidth, topHeight);
        graphics.drawString(font, "HUNTER PROFILE", contentX + 9, contentY + 9, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, "Level " + data.level() + "  •  " + data.rank(), contentX + 9, contentY + 24, SystemUi.Theme.TEXT, false);
        graphics.drawString(font, "Title: " + data.text("title", "The Player"), contentX + 9, contentY + 38, SystemUi.Theme.TEXT_DIM, false);
        graphics.drawString(font, "Unspent stat points: " + data.statPoints(), contentX + 9, contentY + 52,
                data.statPoints() > 0 ? SystemUi.Theme.WARNING : SystemUi.Theme.TEXT_DIM, false);
        SystemUi.Theme.bar(graphics, font, contentX + 9, contentY + topHeight - 25, leftWidth - 18, 11,
                data.xp() / (float)Math.max(1, data.xpRequired()), SystemUi.Theme.CYAN, "XP " + data.xp() + " / " + data.xpRequired());

        if (contentWidth >= 430) {
            int rightX = contentX + leftWidth + gap;
            int rightWidth = contentWidth - leftWidth - gap;
            SystemUi.Theme.inset(graphics, rightX, contentY, rightWidth, topHeight);
            graphics.drawString(font, "RESOURCES", rightX + 9, contentY + 9, SystemUi.Theme.VIOLET, false);
            graphics.drawString(font, "Gold  " + data.gold() + " G", rightX + 9, contentY + 26, SystemUi.Theme.WARNING, false);
            graphics.drawString(font, "Mana  " + data.mana() + " / " + data.maxMana(), rightX + 9, contentY + 41, SystemUi.Theme.CYAN, false);
            graphics.drawString(font, "Shadows  " + data.compounds("shadows").size() + " / " + data.shadowCapacity(), rightX + 9, contentY + 56, SystemUi.Theme.TEXT, false);
        }

        int lowerY = contentY + topHeight + gap;
        int lowerHeight = contentHeight - topHeight - gap;
        int columns = contentWidth >= 430 ? 3 : 1;
        int cardWidth = (contentWidth - gap * (columns - 1)) / columns;
        overviewCard(graphics, contentX, lowerY, cardWidth, lowerHeight, "ACTIVE QUEST", data.activeQuestName(), data.activeQuestProgress(), SystemUi.Icon.QUESTS);
        if (columns > 1) {
            overviewCard(graphics, contentX + cardWidth + gap, lowerY, cardWidth, lowerHeight, "EQUIPMENT", heldWeapon(), "Armor and accessories synchronized", SystemUi.Icon.EQUIPMENT);
            overviewCard(graphics, contentX + (cardWidth + gap) * 2, lowerY, cardWidth, lowerHeight, "DUNGEON", data.dungeonStatus(), "Enter through a generated gate", SystemUi.Icon.DUNGEONS);
        }
    }

    private void overviewCard(GuiGraphics graphics, int x, int y, int width, int height, String heading, String primary, String detail, SystemUi.Icon icon) {
        SystemUi.Theme.inset(graphics, x, y, width, height);
        SystemUi.Theme.icon(graphics, icon, x + 8, y + 8, 16);
        graphics.drawString(font, heading, x + 29, y + 12, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, primary, width - 16), x + 8, y + 34, SystemUi.Theme.TEXT, false);
        List<String> lines = SystemUi.wrap(font, detail, width - 16, Math.max(1, (height - 52) / 11));
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), x + 8, y + 49 + i * 11, SystemUi.Theme.TEXT_DIM, false);
    }

    private void renderStats(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        graphics.drawString(font, "Available points: " + data.statPoints(), contentX, contentY + 2,
                data.statPoints() > 0 ? SystemUi.Theme.WARNING : SystemUi.Theme.TEXT_DIM, false);
        int step = statRowStep();
        for (int i = 0; i < STATS.length; i++) {
            String stat = STATS[i];
            int y = contentY + 20 + i * step;
            SystemUi.Theme.inset(graphics, contentX, y, contentWidth, step - 3);
            int base = data.stat(stat);
            int bonus = data.bonus(stat);
            graphics.drawString(font, SystemUi.titleCase(stat), contentX + 8, y + 6, SystemUi.Theme.TEXT, false);
            String value = Integer.toString(base) + (bonus == 0 ? "" : (bonus > 0 ? " +" : " ") + bonus);
            graphics.drawString(font, value, contentX + contentWidth - 58 - font.width(value), y + 6,
                    bonus == 0 ? SystemUi.Theme.CYAN : SystemUi.Theme.SUCCESS, false);
            if (step >= 31) graphics.drawString(font, SystemUi.Theme.ellipsize(font, STAT_DESCRIPTIONS.get(stat), contentWidth - 78), contentX + 8, y + 18, SystemUi.Theme.TEXT_DIM, false);
        }
    }

    private void renderAbilities(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        int listBottom = contentY + contentHeight - 46;
        boolean split = contentWidth >= 430;
        int listWidth = split ? Math.max(180, contentWidth / 2 - 4) : contentWidth;
        int rowHeight = 25;
        int visible = Math.max(1, (listBottom - contentY) / rowHeight);
        int maximum = Math.max(0, abilities.size() - visible);
        int scroll = clampScroll(maximum);
        graphics.enableScissor(contentX, contentY, contentX + listWidth, listBottom);
        for (int row = 0; row < visible && row + scroll < abilities.size(); row++) {
            AbilityDefinition definition = abilities.get(row + scroll);
            int y = contentY + row * rowHeight;
            boolean selected = definition.id().equals(selectedAbility);
            boolean unlocked = isUnlocked(data, definition);
            boolean conflict = ClientKeyMappings.hasConflict(definition.id());
            graphics.fill(contentX, y, contentX + listWidth - 4, y + 22, selected ? 0xEE173758 : 0xC8070D1B);
            graphics.fill(contentX, y, contentX + 2, y + 22, unlocked ? SystemUi.Theme.CYAN : 0xFF39465A);
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, definition.displayName(), listWidth - 90), contentX + 7, y + 4,
                    unlocked ? SystemUi.Theme.TEXT : SystemUi.Theme.TEXT_DIM, false);
            String key = ClientKeyMappings.keyName(definition.id());
            graphics.drawString(font, key, contentX + listWidth - font.width(key) - 9, y + 4,
                    conflict ? SystemUi.Theme.FAILURE : SystemUi.Theme.VIOLET, false);
            String state = !unlocked ? "LOCKED" : data.cooldownRemaining(definition.id()) > 0L
                    ? SystemUi.Cooldowns.remainingText(data, definition.id()) : data.mana() < definition.manaCost() ? "NO MANA" : "READY";
            graphics.drawString(font, state, contentX + 7, y + 13,
                    !unlocked ? SystemUi.Theme.TEXT_DIM : "READY".equals(state) ? SystemUi.Theme.SUCCESS : SystemUi.Theme.WARNING, false);
        }
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + listWidth - 3, contentY, listBottom - contentY, scroll, maximum);

        AbilityDefinition selected = selectedDefinition();
        if (split && selected != null) renderAbilityDetails(graphics, data, selected, contentX + listWidth + 4, contentY, contentWidth - listWidth - 4, listBottom - contentY);
        else if (selected != null && mouseX >= contentX && mouseX < contentX + listWidth && mouseY >= contentY && mouseY < listBottom) {
            List<Component> tooltip = List.of(Component.literal(selected.displayName()), Component.literal(selected.description()),
                    Component.literal("Mana " + selected.manaCost() + "  •  Cooldown " + formatSeconds(selected.cooldownSeconds()) + "s"),
                    Component.literal(selected.unlock().description()), Component.literal("Key: " + ClientKeyMappings.keyName(selected.id())));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderAbilityDetails(GuiGraphics graphics, SystemUi.Data data, AbilityDefinition definition, int x, int y, int width, int height) {
        SystemUi.Theme.inset(graphics, x, y, width, height);
        SystemUi.Theme.icon(graphics, SystemUi.Icon.ABILITIES, x + 8, y + 8, 16);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, definition.displayName(), width - 35), x + 29, y + 11, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, SystemUi.titleCase(definition.category()) + " • " + (isUnlocked(data, definition) ? "UNLOCKED" : "LOCKED"), x + 8, y + 31,
                isUnlocked(data, definition) ? SystemUi.Theme.SUCCESS : SystemUi.Theme.FAILURE, false);
        List<String> lines = SystemUi.wrap(font, definition.description(), width - 16, 4);
        for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), x + 8, y + 47 + i * 11, SystemUi.Theme.TEXT, false);
        int detailsY = y + 96;
        graphics.drawString(font, "Mana cost: " + definition.manaCost(), x + 8, detailsY, data.mana() >= definition.manaCost() ? SystemUi.Theme.CYAN : SystemUi.Theme.FAILURE, false);
        graphics.drawString(font, "Cooldown: " + formatSeconds(definition.cooldownSeconds()) + "s", x + 8, detailsY + 13, SystemUi.Theme.TEXT_DIM, false);
        graphics.drawString(font, "Range: " + (definition.maximumRange() <= 0 ? "Self" : formatSeconds(definition.maximumRange()) + " blocks"), x + 8, detailsY + 26, SystemUi.Theme.TEXT_DIM, false);
        graphics.drawString(font, "Key: " + ClientKeyMappings.keyName(definition.id()), x + 8, detailsY + 39,
                ClientKeyMappings.hasConflict(definition.id()) ? SystemUi.Theme.FAILURE : SystemUi.Theme.VIOLET, false);
        List<String> unlock = SystemUi.wrap(font, definition.unlock().description(), width - 16, 2);
        for (int i = 0; i < unlock.size(); i++) graphics.drawString(font, unlock.get(i), x + 8, detailsY + 55 + i * 11, SystemUi.Theme.TEXT_DIM, false);
    }

    private void renderInventory(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        ListTag list = data.compounds("system_inventory");
        int columns = inventoryColumns();
        int rows = inventoryRows();
        int pageSize = columns * rows;
        int start = inventoryPage * pageSize;
        int hovered = -1;
        for (int slot = 0; slot < pageSize; slot++) {
            int x = contentX + (slot % columns) * 22;
            int y = contentY + (slot / columns) * 22;
            SystemUi.Theme.slot(graphics, x, y, 20, false, true);
            int index = start + slot;
            if (index < list.size()) {
                ItemStack stack = ItemStack.of(list.getCompound(index));
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 2, y + 2);
                    graphics.renderItemDecorations(font, stack, x + 2, y + 2);
                    if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20) hovered = index;
                }
            }
        }
        int summaryX = contentX + columns * 22 + 8;
        if (summaryX + 90 < contentX + contentWidth) {
            graphics.drawString(font, "SYSTEM STORAGE", summaryX, contentY + 2, SystemUi.Theme.CYAN, false);
            graphics.drawString(font, list.size() + " / 108 slots", summaryX, contentY + 18, SystemUi.Theme.TEXT, false);
            graphics.drawString(font, "Page " + (inventoryPage + 1) + " / " + (maxInventoryPage(data) + 1), summaryX, contentY + 32, SystemUi.Theme.TEXT_DIM, false);
            List<String> help = SystemUi.wrap(font, "Click an item to retrieve it to your normal inventory.", contentX + contentWidth - summaryX, 4);
            for (int i = 0; i < help.size(); i++) graphics.drawString(font, help.get(i), summaryX, contentY + 55 + i * 11, SystemUi.Theme.TEXT_DIM, false);
        }
        if (list.isEmpty()) graphics.drawCenteredString(font, "System inventory is empty", contentX + Math.min(contentWidth, columns * 22) / 2, contentY + rows * 11 - 4, SystemUi.Theme.TEXT_DIM);
        if (hovered >= 0) graphics.renderTooltip(font, ItemStack.of(list.getCompound(hovered)), mouseX, mouseY);
    }

    private void renderEquipment(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            graphics.drawString(font, "Player equipment is unavailable.", contentX, contentY + 8, SystemUi.Theme.FAILURE, false);
            return;
        }
        int cardWidth = contentWidth >= 430 ? (contentWidth - 8) / 2 : contentWidth;
        SystemUi.Theme.inset(graphics, contentX, contentY, cardWidth, contentHeight);
        graphics.drawString(font, "ARMOR", contentX + 8, contentY + 8, SystemUi.Theme.CYAN, false);
        String[] labels = {"Helmet", "Chest", "Legs", "Boots"};
        int[] armorSlots = {3, 2, 1, 0};
        for (int i = 0; i < labels.length; i++) {
            int y = contentY + 27 + i * 34;
            ItemStack stack = client.player.getInventory().armor.get(armorSlots[i]);
            SystemUi.Theme.slot(graphics, contentX + 8, y, 24, false, !stack.isEmpty());
            graphics.renderItem(stack, contentX + 12, y + 4);
            String name = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
            graphics.drawString(font, labels[i], contentX + 39, y + 3, SystemUi.Theme.TEXT_DIM, false);
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, name, cardWidth - 48), contentX + 39, y + 15,
                    stack.isEmpty() ? SystemUi.Theme.TEXT_DIM : SystemUi.Theme.TEXT, false);
            if (!stack.isEmpty() && mouseX >= contentX + 8 && mouseX < contentX + cardWidth && mouseY >= y && mouseY < y + 24) graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
        if (contentWidth >= 430) {
            int rightX = contentX + cardWidth + 8;
            int rightWidth = contentWidth - cardWidth - 8;
            SystemUi.Theme.inset(graphics, rightX, contentY, rightWidth, contentHeight);
            graphics.drawString(font, "WEAPON & ACCESSORIES", rightX + 8, contentY + 8, SystemUi.Theme.VIOLET, false);
            ItemStack held = client.player.getMainHandItem();
            SystemUi.Theme.slot(graphics, rightX + 8, contentY + 27, 28, false, !held.isEmpty());
            graphics.renderItem(held, rightX + 14, contentY + 33);
            graphics.drawString(font, "Primary weapon", rightX + 44, contentY + 29, SystemUi.Theme.TEXT_DIM, false);
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, held.isEmpty() ? "Empty" : held.getHoverName().getString(), rightWidth - 54), rightX + 44, contentY + 42,
                    held.isEmpty() ? SystemUi.Theme.TEXT_DIM : SystemUi.Theme.TEXT, false);
            String[] slots = {"hands", "earring", "necklace", "ring"};
            for (int i = 0; i < slots.length; i++) {
                String value = data.raw().getString("accessory_" + slots[i]);
                graphics.drawString(font, SystemUi.titleCase(slots[i]) + ": " + SystemUi.Theme.ellipsize(font, value.isBlank() ? "Empty" : SystemUi.titleCase(value), rightWidth - 30),
                        rightX + 10, contentY + 76 + i * 18, value.isBlank() ? SystemUi.Theme.TEXT_DIM : SystemUi.Theme.VIOLET, false);
            }
            graphics.drawString(font, "Equipment remains server-authoritative.", rightX + 8, contentY + contentHeight - 16, SystemUi.Theme.TEXT_DIM, false);
        }
    }

    private void renderQuests(GuiGraphics graphics, SystemUi.Data data) {
        int viewportHeight = contentHeight - 24;
        int totalHeight = 190;
        int maximum = Math.max(0, totalHeight - viewportHeight);
        int scroll = clampPixelScroll(maximum);
        int y = contentY - scroll;
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + viewportHeight);
        SystemUi.Theme.inset(graphics, contentX, y, contentWidth, 48);
        graphics.drawString(font, "ACTIVE MAIN QUEST", contentX + 8, y + 7, SystemUi.Theme.VIOLET, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, data.activeQuestName(), contentWidth - 16), contentX + 8, y + 21, SystemUi.Theme.TEXT, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, data.activeQuestProgress(), contentWidth - 16), contentX + 8, y + 34, SystemUi.Theme.TEXT_DIM, false);
        y += 58;
        graphics.drawString(font, "DAILY TRAINING", contentX, y, SystemUi.Theme.CYAN, false); y += 16;
        objective(graphics, y, "Defeat enemies", data.raw().getInt("daily_kills"), 8); y += 20;
        objective(graphics, y, "Sprint distance", data.raw().getInt("daily_run"), 600); y += 20;
        objective(graphics, y, "Land attacks", data.raw().getInt("daily_attacks"), 20); y += 20;
        objective(graphics, y, "Jump and traverse", data.raw().getInt("daily_jumps"), 15); y += 20;
        objective(graphics, y, "Use abilities", data.raw().getInt("daily_abilities"), 5); y += 24;
        String state = data.dailyClaimed() ? "REWARD CLAIMED" : data.dailyComplete() ? "COMPLETE — CLAIM AVAILABLE" : "IN PROGRESS";
        graphics.drawString(font, state, contentX, y, data.dailyComplete() ? SystemUi.Theme.SUCCESS : SystemUi.Theme.WARNING, false);
        if (data.raw().getBoolean("emergency_active")) graphics.drawString(font, "EMERGENCY QUEST: " + data.raw().getInt("emergency_kills") + " / 3 kills", contentX, y + 15, SystemUi.Theme.FAILURE, false);
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, contentY, viewportHeight, scroll, maximum);
    }

    private void objective(GuiGraphics graphics, int y, String label, int value, int target) {
        int safe = Math.max(0, Math.min(value, target));
        SystemUi.Theme.bar(graphics, font, contentX, y, contentWidth - 6, 14, safe / (float)Math.max(1, target),
                safe >= target ? SystemUi.Theme.SUCCESS : SystemUi.Theme.CYAN, label + "  " + safe + " / " + target);
    }

    private void renderStore(GuiGraphics graphics, SystemUi.Data data, int mouseX, int mouseY) {
        graphics.drawString(font, "Available gold: " + data.gold() + " G", contentX, contentY + 3, SystemUi.Theme.WARNING, false);
        graphics.drawString(font, "Purchases are validated and delivered by the server.", contentX, contentY + 17, SystemUi.Theme.TEXT_DIM, false);
        for (StoreButton entry : storeButtons) {
            if (entry.button.isMouseOver(mouseX, mouseY)) {
                List<Component> tooltip = List.of(Component.literal(entry.product.name()), Component.literal(entry.product.description),
                        Component.literal("Price: " + entry.product.price + " G"), Component.literal(entry.button.active ? "Available" : entry.button.disabledReason()));
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
        if (data.compounds("system_inventory").size() >= 108) graphics.drawString(font, "System storage is full. Purchases are disabled.", contentX, contentY + contentHeight - 12, SystemUi.Theme.FAILURE, false);
    }

    private void renderDungeons(GuiGraphics graphics, SystemUi.Data data) {
        int viewportHeight = contentHeight;
        int cardHeight = 50;
        int totalHeight = DUNGEONS.size() * (cardHeight + 5) + 40;
        int maximum = Math.max(0, totalHeight - viewportHeight);
        int scroll = clampPixelScroll(maximum);
        int y = contentY - scroll;
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + viewportHeight);
        SystemUi.Theme.inset(graphics, contentX, y, contentWidth, 34);
        graphics.drawString(font, "CURRENT STATUS", contentX + 8, y + 6, SystemUi.Theme.VIOLET, false);
        graphics.drawString(font, SystemUi.Theme.ellipsize(font, data.dungeonStatus(), contentWidth - 16), contentX + 8, y + 19, SystemUi.Theme.TEXT, false);
        y += 40;
        for (DungeonCard dungeon : DUNGEONS) {
            SystemUi.Theme.inset(graphics, contentX, y, contentWidth, cardHeight);
            graphics.drawString(font, dungeon.name, contentX + 9, y + 7, SystemUi.Theme.CYAN, false);
            String meta = dungeon.rank + "-RANK  •  RECOMMENDED LEVEL " + dungeon.level;
            graphics.drawString(font, meta, contentX + contentWidth - font.width(meta) - 9, y + 7,
                    data.level() >= dungeon.level ? SystemUi.Theme.SUCCESS : SystemUi.Theme.WARNING, false);
            List<String> lines = SystemUi.wrap(font, dungeon.description, contentWidth - 18, 2);
            for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), contentX + 9, y + 23 + i * 11, SystemUi.Theme.TEXT_DIM, false);
            y += cardHeight + 5;
        }
        graphics.disableScissor();
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, contentY, viewportHeight, scroll, maximum);
    }

    private void renderShadows(GuiGraphics graphics, SystemUi.Data data) {
        ListTag shadows = data.compounds("shadows");
        int actionRows = contentWidth >= 360 ? 2 : 3;
        int listTop = contentY + 34;
        int listBottom = contentY + contentHeight - actionRows * 22 - 3;
        graphics.drawString(font, "Army capacity: " + shadows.size() + " / " + data.shadowCapacity() + "  •  Active: " + data.strings("active_shadows").size(),
                contentX, contentY + 2, SystemUi.Theme.VIOLET, false);
        String[] modes = {"Follow", "Guard", "Passive", "Aggressive"};
        graphics.drawString(font, "Command mode: " + modes[Math.floorMod(data.raw().getInt("shadow_mode"), modes.length)], contentX, contentY + 16, SystemUi.Theme.TEXT_DIM, false);
        int rowHeight = 27;
        int visible = Math.max(1, (listBottom - listTop) / rowHeight);
        int maximum = Math.max(0, shadows.size() - visible);
        int scroll = clampScroll(maximum);
        graphics.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        for (int row = 0; row < visible && row + scroll < shadows.size(); row++) {
            CompoundTag shadow = shadows.getCompound(row + scroll);
            int y = listTop + row * rowHeight;
            SystemUi.Theme.inset(graphics, contentX, y, contentWidth, 24);
            String name = shadow.getString("name").isBlank() ? "Unnamed Shadow" : shadow.getString("name");
            String type = shadow.getString("type").isBlank() ? "Shadow Soldier" : SystemUi.titleCase(shadow.getString("type"));
            graphics.drawString(font, SystemUi.Theme.ellipsize(font, name, contentWidth - 115), contentX + 7, y + 4, SystemUi.Theme.TEXT, false);
            graphics.drawString(font, type, contentX + 7, y + 14, SystemUi.Theme.TEXT_DIM, false);
            String details = "Lv" + Math.max(1, shadow.getInt("level")) + " " + (shadow.getString("rank").isBlank() ? "Normal" : shadow.getString("rank"));
            graphics.drawString(font, details, contentX + contentWidth - font.width(details) - 7, y + 8, SystemUi.Theme.CYAN, false);
        }
        graphics.disableScissor();
        if (shadows.isEmpty()) graphics.drawCenteredString(font, "No preserved shadows", contentX + contentWidth / 2, listTop + 18, SystemUi.Theme.TEXT_DIM);
        if (maximum > 0) renderScrollBar(graphics, contentX + contentWidth - 3, listTop, listBottom - listTop, scroll, maximum);
    }

    private void renderSettings(GuiGraphics graphics) {
        int bottom = contentY + contentHeight;
        graphics.drawString(font, "HUD scale: " + SystemUi.Settings.hudScale() + "%  •  Opacity: " + SystemUi.Settings.hudOpacity() + "%", contentX, bottom - 28, SystemUi.Theme.CYAN, false);
        graphics.drawString(font, "Offset: " + SystemUi.Settings.offsetX() + ", " + SystemUi.Settings.offsetY() + "  •  Stored locally", contentX, bottom - 15, SystemUi.Theme.TEXT_DIM, false);
    }

    private void renderDisabledTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : children()) {
            if (listener instanceof SystemButton button && !button.active && button.isMouseOver(mouseX, mouseY) && !button.disabledReason().isBlank()) {
                graphics.renderTooltip(font, Component.literal(button.disabledReason()), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && ClientHunterData.awakened()) {
            if (tab == Tab.SKILLS && clickAbility(mouseX, mouseY)) return true;
            if (tab == Tab.INVENTORY && clickInventory(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickAbility(double mouseX, double mouseY) {
        int listBottom = contentY + contentHeight - 46;
        int listWidth = contentWidth >= 430 ? Math.max(180, contentWidth / 2 - 4) : contentWidth;
        if (mouseX < contentX || mouseX >= contentX + listWidth || mouseY < contentY || mouseY >= listBottom) return false;
        int visible = Math.max(1, (listBottom - contentY) / 25);
        int maximum = Math.max(0, abilities.size() - visible);
        int index = clampScroll(maximum) + ((int)mouseY - contentY) / 25;
        if (index < 0 || index >= abilities.size()) return false;
        selectedAbility = abilities.get(index).id();
        refreshButtonStates();
        return true;
    }

    private boolean clickInventory(double mouseX, double mouseY) {
        int columns = inventoryColumns();
        int rows = inventoryRows();
        int relativeX = (int)mouseX - contentX;
        int relativeY = (int)mouseY - contentY;
        if (relativeX < 0 || relativeY < 0 || relativeX >= columns * 22 || relativeY >= rows * 22) return false;
        int column = relativeX / 22;
        int row = relativeY / 22;
        if (relativeX % 22 >= 20 || relativeY % 22 >= 20) return false;
        int index = inventoryPage * columns * rows + row * columns + column;
        if (index >= ClientHunterData.view().compounds("system_inventory").size()) return false;
        SystemUi.Actions.send("RETRIEVE_SLOT:" + index, 260L);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < contentX || mouseX >= contentX + contentWidth || mouseY < contentY || mouseY >= contentY + contentHeight) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (tab == Tab.SKILLS || tab == Tab.SHADOWS) {
            scrollOffsets.put(tab, Math.max(0, scrollOffsets.getOrDefault(tab, 0) + (delta > 0 ? -1 : 1)));
            return true;
        }
        if (tab == Tab.QUESTS || tab == Tab.DUNGEONS || tab == Tab.SETTINGS) {
            scrollOffsets.put(tab, Math.max(0, scrollOffsets.getOrDefault(tab, 0) + (delta > 0 ? -14 : 14)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        SystemUi.Settings.setLastTab(tab.name());
        super.onClose();
    }

    private void confirm(String heading, String detail, Runnable action) {
        if (minecraft != null) minecraft.setScreen(new SystemConfirmScreen(this, heading, detail, action));
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y, int height, int scroll, int maximum) {
        graphics.fill(x, y, x + 2, y + height, 0x552A3B52);
        int thumb = Math.max(8, height / Math.max(2, maximum + 1));
        int offset = maximum == 0 ? 0 : Math.round((height - thumb) * (scroll / (float)maximum));
        graphics.fill(x, y + offset, x + 2, y + offset + thumb, SystemUi.Theme.CYAN);
    }

    private int clampScroll(int maximum) {
        int value = Math.max(0, Math.min(maximum, scrollOffsets.getOrDefault(tab, 0)));
        scrollOffsets.put(tab, value);
        return value;
    }

    private int clampPixelScroll(int maximum) { return clampScroll(maximum); }
    private int statRowStep() { return Math.max(22, Math.min(36, Math.max(22, contentHeight - 22) / 5)); }
    private int inventoryColumns() {
        int available = Math.max(88, contentWidth - (contentWidth >= 300 ? 110 : 0));
        return Math.max(4, Math.min(9, available / 22));
    }
    private int inventoryRows() { return Math.max(2, Math.min(7, Math.max(44, contentHeight - 24) / 22)); }
    private int maxInventoryPage(SystemUi.Data data) {
        int pageSize = Math.max(1, inventoryColumns() * inventoryRows());
        int count = data.compounds("system_inventory").size();
        return Math.max(0, (count - 1) / pageSize);
    }
    private AbilityDefinition selectedDefinition() {
        for (AbilityDefinition definition : abilities) if (definition.id().equals(selectedAbility)) return definition;
        return abilities.isEmpty() ? null : abilities.get(0);
    }
    private static boolean isUnlocked(SystemUi.Data data, AbilityDefinition definition) {
        if (!data.awakened()) return false;
        String skill = definition.unlock().skillId();
        boolean hasSkill = !skill.isBlank() && data.skillUnlocked(skill);
        return definition.unlock().skillRequired() ? hasSkill : hasSkill || data.level() >= definition.unlock().minimumLevel();
    }
    private static String actionForAbility(String id) {
        return switch (id) {
            case "shadow_extraction" -> "EXTRACT";
            case "shadow_exchange" -> "SHADOW_EXCHANGE";
            case "shadow_summoning" -> "SUMMON_SHADOW";
            default -> "ABILITY:" + id;
        };
    }
    private static String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private static String formatSeconds(double value) { return String.format(Locale.ROOT, value == Math.rint(value) ? "%.0f" : "%.1f", value); }
    private static Tab rememberedTab() {
        try { return Tab.valueOf(SystemUi.Settings.lastTab()); }
        catch (IllegalArgumentException ignored) { return Tab.HOME; }
    }
    private String heldWeapon() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.getMainHandItem().isEmpty()) return "No weapon equipped";
        return client.player.getMainHandItem().getHoverName().getString();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record StoreProduct(String id, int price, String description) {
        String name() { return SystemUi.titleCase(id); }
    }
    private record StoreButton(SystemButton button, StoreProduct product) { }
    private record DungeonCard(String name, String rank, int level, String description) { }
    private record SettingAction(LabelSupplier label, Runnable action) { }
    @FunctionalInterface private interface LabelSupplier { String get(); }
}
