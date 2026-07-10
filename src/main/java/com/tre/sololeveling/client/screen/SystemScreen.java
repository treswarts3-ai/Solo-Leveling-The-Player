package com.tre.sololeveling.client.screen;

import com.tre.sololeveling.client.ClientHunterData;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.network.packet.ActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class SystemScreen extends Screen {
    private static final int INVENTORY_COLUMNS = 9;
    private static final int INVENTORY_ROWS = 6;
    private static final int INVENTORY_PAGE_SIZE = INVENTORY_COLUMNS * INVENTORY_ROWS;
    public enum Tab { STATUS, SKILLS, QUESTS, STORE, SHADOWS, INVENTORY }
    private Tab tab;
    private int left, top;
    private int inventoryPage;
    public SystemScreen(Tab tab) { super(Component.translatable("screen.sololeveling.system")); this.tab = tab; }

    @Override protected void init() {
        left = (width - 340) / 2; top = (height - 214) / 2;
        int tx = left + 8;
        for (Tab value : Tab.values()) {
            Tab target = value;
            addRenderableWidget(Button.builder(Component.literal(shortName(value)), b -> { tab = target; rebuildWidgets(); }).bounds(tx, top + 9, 51, 18).build());
            tx += 54;
        }
        CompoundTag t = ClientHunterData.get();
        if (!t.getBoolean("awakened")) {
            addRenderableWidget(Button.builder(Component.literal("AWAKEN SYSTEM"), b -> send("awaken", "", 0)).bounds(left+105, top+90, 130, 22).build());
            return;
        }
        if (tab == Tab.STATUS) addStatusButtons();
        if (tab == Tab.SKILLS) addSkillButtons();
        if (tab == Tab.QUESTS) addQuestButtons();
        if (tab == Tab.STORE) addStoreButtons();
        if (tab == Tab.SHADOWS) addShadowButtons();
        if (tab == Tab.INVENTORY) addInventoryButtons();
    }

    @Override
    protected void rebuildWidgets() { clearWidgets(); init(); }
    private void addStatusButtons() {
        String[] stats={"strength","agility","stamina","intelligence","sense"}; int y=top+63;
        for(String stat:stats){ addRenderableWidget(Button.builder(Component.literal("+"),b->send("allocate",stat,1)).bounds(left+288,y-3,18,16).build()); y+=22; }
    }
    private void addSkillButtons() {
        String[] modes={"pull","push","hold","throw","dash","flight"};
        for(int i=0;i<modes.length;i++){
            String mode=modes[i];
            int x=left+20+(i%3)*102,y=top+158+(i/3)*23;
            addRenderableWidget(Button.builder(Component.literal("Authority: "+capitalize(mode)),b->send("authority",mode,0)).bounds(x,y,96,19).build());
        }
    }
    private void addQuestButtons() {
        addRenderableWidget(Button.builder(Component.literal("Push-up"), b -> send("exercise","pushup",0)).bounds(left+20,top+184,70,18).build());
        addRenderableWidget(Button.builder(Component.literal("Sit-up"), b -> send("exercise","situp",0)).bounds(left+94,top+184,65,18).build());
        addRenderableWidget(Button.builder(Component.literal("Squat"), b -> send("exercise","squat",0)).bounds(left+163,top+184,65,18).build());
        addRenderableWidget(Button.builder(Component.literal("Claim"), b -> send("quest","claim_daily",0)).bounds(left+232,top+184,75,18).build());
    }
    private void addStoreButtons() {
        store("Healing Potion", "healing_potion", 50, 55); store("Mana Potion", "mana_potion", 60, 80); store("Knight Killer", "knight_killer", 1500, 105); store("Random Box", "blessed_random_box", 750, 130);
    }
    private void store(String label,String id,int price,int y){ addRenderableWidget(Button.builder(Component.literal("Buy " + label + " - " + price + "G"),b->send("buy",id,price)).bounds(left+65,top+y,210,20).build()); }
    private void addShadowButtons() {
        addRenderableWidget(Button.builder(Component.literal("Extract"),b->send("shadow","extract",0)).bounds(left+28,top+162,70,18).build());
        addRenderableWidget(Button.builder(Component.literal("Summon"),b->send("shadow","summon",0)).bounds(left+102,top+162,70,18).build());
        addRenderableWidget(Button.builder(Component.literal("Dismiss All"),b->send("shadow","dismiss",0)).bounds(left+176,top+162,75,18).build());
        addRenderableWidget(Button.builder(Component.literal("Exchange"),b->send("shadow","exchange",0)).bounds(left+255,top+162,62,18).build());
        addRenderableWidget(Button.builder(Component.literal("Domain"),b->send("shadow","domain",0)).bounds(left+88,top+185,75,18).build());
        addRenderableWidget(Button.builder(Component.literal("Cycle Mode"),b->send("shadow","mode",0)).bounds(left+168,top+185,85,18).build());
    }
    private void addInventoryButtons() {
        addRenderableWidget(Button.builder(Component.literal("Store Held Item"),b->send("inventory","store",0)).bounds(left+20,top+184,105,18).build());
        addRenderableWidget(Button.builder(Component.literal("< Prev"),b->{ inventoryPage=Math.max(0,inventoryPage-1); rebuildWidgets(); }).bounds(left+217,top+184,48,18).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"),b->{
            int count=ClientHunterData.get().getList("system_inventory",Tag.TAG_COMPOUND).size();
            int maxPage=Math.max(0,(count-1)/INVENTORY_PAGE_SIZE);
            inventoryPage=Math.min(maxPage,inventoryPage+1);
            rebuildWidgets();
        }).bounds(left+270,top+184,48,18).build());
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); panel(g,left,top,340,214); super.render(g,mouseX,mouseY,partialTick);
        CompoundTag t=ClientHunterData.get();
        g.drawCenteredString(font, title, width/2, top+34, 0xFF78E9FF);
        if(!t.getBoolean("awakened")){ g.drawCenteredString(font,"A strange blue interface waits for your response.",width/2,top+70,0xFFBBD6EA); return; }
        switch(tab){case STATUS->status(g,t);case SKILLS->skills(g,t);case QUESTS->quests(g,t);case STORE->store(g,t);case SHADOWS->shadows(g,t);case INVENTORY->inventory(g,t,mouseX,mouseY);}
    }
    private void status(GuiGraphics g,CompoundTag t){
        int level=Math.max(1,t.getInt("level")),xp=t.getInt("xp"),need=Math.max(100,(int)Math.floor(100*Math.pow(level,1.55)));
        String displayRank=blank(t.getString("rank_override"),rank(level,t.getBoolean("black_heart")));
        text(g,"LEVEL  " + level + "       RANK  " + displayRank,left+22,top+50,0xFFFFFFFF);
        text(g,"XP  " + xp + " / " + need + "       GOLD  " + t.getInt("gold"),left+22,top+65,0xFF8EEBFF);
        text(g,"JOB  " + blank(t.getString("job"),"None") + "       TITLE  " + blank(t.getString("title"),"The Player"),left+22,top+80,0xFFD9C6FF);
        String[] stats={"strength","agility","stamina","intelligence","sense"};int y=top+105;
        for(String s:stats){text(g,s.toUpperCase(Locale.ROOT)+"  "+Math.max(1,t.getInt(s)),left+35,y,0xFFEAF7FF);y+=22;}
        text(g,"UNSPENT STAT POINTS: "+t.getInt("stat_points"),left+178,top+105,0xFFFFD66B);
        int maxMana=100+level*2+Math.max(1,t.getInt("intelligence"))*8+(t.getBoolean("black_heart")?1000:0);
        text(g,"MANA: "+t.getInt("mana")+" / "+maxMana,left+178,top+127,0xFF9C82FF);
        text(g,"SYSTEM INVENTORY: "+t.getList("system_inventory",10).size()+" / 108",left+178,top+149,0xFF8EEBFF);
        text(g,"SHADOW CAPACITY: "+shadowCap(t),left+178,top+171,0xFFC8A7FF);
    }
    private void skills(GuiGraphics g,CompoundTag t){
        for(int i=0;i<HunterData.SKILLS.length;i++){
            String skill=HunterData.SKILLS[i];boolean on=t.getBoolean("skill_"+skill);
            int column=i/8,row=i%8,x=left+16+column*166,y=top+50+row*13;
            text(g,(on?"[+] ":"[-] ")+skill.replace('_',' ').toUpperCase(Locale.ROOT),x,y,on?0xFF7DEBFF:0xFF657188);
        }
        text(g,"RULER'S AUTHORITY CONTROLS",left+82,top+144,0xFFD9C6FF);
    }
    private void quests(GuiGraphics g,CompoundTag t){
        String main=t.getString("active_main_quest").replace('_',' ').toUpperCase(Locale.ROOT);
        text(g,"MAIN QUEST: "+main,left+20,top+50,0xFFD9C6FF);
        text(g,mainProgress(t),left+20,top+64,0xFF9AB6C8);
        text(g,"DAILY: PREPARATION TO BECOME POWERFUL",left+20,top+82,0xFF7DEBFF);
        objective(g,"Hostile mobs",t.getInt("daily_kills"),10,top+98);
        objective(g,"Run blocks",t.getInt("daily_run"),1000,top+113);
        objective(g,"Push-ups",t.getInt("daily_pushups"),30,top+128);
        objective(g,"Sit-ups",t.getInt("daily_situps"),30,top+143);
        objective(g,"Squats",t.getInt("daily_squats"),30,top+158);
        text(g,t.getBoolean("daily_complete")?"COMPLETE - CLAIM":"IN PROGRESS",left+218,top+82,t.getBoolean("daily_complete")?0xFF61FFAD:0xFFFFD66B);
        if(t.getBoolean("emergency_active")) text(g,"EMERGENCY: "+t.getInt("emergency_kills")+"/3 KILLS",left+205,top+158,0xFFFF6B7A);
    }
    private static String mainProgress(CompoundTag t){return switch(t.getInt("progression_stage")){case 0->"Open System, allocate a stat, claim Daily";case 1->"Dagger damage "+t.getInt("quest_dagger_damage")+" / 500";case 2->"Reach Lv40; kills "+t.getInt("job_change_kills")+" / 25";case 3->"Extract 3 shadows and summon one";case 4->"Reach Lv80 and preserve 10 shadows";default->"All current progression completed";};}
    private void store(GuiGraphics g,CompoundTag t){text(g,"SYSTEM GOLD: "+t.getInt("gold"),left+20,top+52,0xFFFFD66B);text(g,"Purchases are validated by the server.",left+20,top+68,0xFF9AB6C8);}
    private void shadows(GuiGraphics g,CompoundTag t){int stored=t.getList("shadows",10).size(),active=t.getList("active_shadows",8).size();String[] modes={"Follow","Guard","Passive","Aggressive"};int mode=Math.floorMod(t.getInt("shadow_mode"),modes.length);text(g,"STORED SHADOWS: "+stored+" / "+shadowCap(t),left+24,top+60,0xFFB69BFF);text(g,"ACTIVE SHADOWS: "+active+"     MODE: "+modes[mode],left+24,top+79,0xFF7DEBFF);text(g,"Nearest death imprint can be extracted with G.",left+24,top+102,0xFFB8CAD8);text(g,"Owner-safe AI prevents friendly fire and duplicate summons.",left+24,top+120,0xFF8795A6);}
    private void inventory(GuiGraphics g,CompoundTag t,int mouseX,int mouseY){
        ListTag list=t.getList("system_inventory",Tag.TAG_COMPOUND);
        int count=list.size(),pages=Math.max(1,(count+INVENTORY_PAGE_SIZE-1)/INVENTORY_PAGE_SIZE);
        inventoryPage=Math.min(inventoryPage,pages-1);
        int gridX=left+20,gridY=top+55,hovered=-1;
        for(int slot=0;slot<INVENTORY_PAGE_SIZE;slot++){
            int x=gridX+(slot%INVENTORY_COLUMNS)*18,y=gridY+(slot/INVENTORY_COLUMNS)*18;
            g.fill(x,y,x+18,y+18,0xFF31425C); g.fill(x+1,y+1,x+17,y+17,0xD90B1427);
            int index=inventoryPage*INVENTORY_PAGE_SIZE+slot;
            if(index<count){
                ItemStack stack=ItemStack.of(list.getCompound(index));
                g.renderItem(stack,x+1,y+1); g.renderItemDecorations(font,stack,x+1,y+1);
                if(mouseX>=x&&mouseX<x+18&&mouseY>=y&&mouseY<y+18) hovered=index;
            }
        }
        text(g,"SYSTEM INVENTORY",left+197,top+58,0xFF7DEBFF);
        text(g,count+" / 108 slots",left+197,top+78,0xFFFFFFFF);
        text(g,"PAGE "+(inventoryPage+1)+" / "+pages,left+197,top+94,0xFFD9C6FF);
        text(g,"Click an item",left+197,top+119,0xFF9AB6C8);
        text(g,"to retrieve it.",left+197,top+133,0xFF9AB6C8);
        text(g,"Storage persists",left+197,top+153,0xFF6FA9C7);
        text(g,"with hunter data.",left+197,top+167,0xFF6FA9C7);
        if(hovered>=0) g.renderTooltip(font,ItemStack.of(list.getCompound(hovered)),mouseX,mouseY);
    }

    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button==0&&tab==Tab.INVENTORY&&ClientHunterData.get().getBoolean("awakened")){
            int gridX=left+20,gridY=top+55;
            int relX=(int)mouseX-gridX,relY=(int)mouseY-gridY;
            if(relX>=0&&relY>=0&&relX<INVENTORY_COLUMNS*18&&relY<INVENTORY_ROWS*18){
                int index=inventoryPage*INVENTORY_PAGE_SIZE+(relY/18)*INVENTORY_COLUMNS+(relX/18);
                if(index<ClientHunterData.get().getList("system_inventory",Tag.TAG_COMPOUND).size()){
                    ModNetwork.CHANNEL.sendToServer(new ActionPacket("RETRIEVE_SLOT:"+index));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    private void objective(GuiGraphics g,String name,int value,int target,int y){text(g,name+": "+Math.min(value,target)+" / "+target,left+35,y,value>=target?0xFF61FFAD:0xFFFFFFFF);}
    private static void panel(GuiGraphics g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,0xE7081024);g.fill(x,y,x+w,y+2,0xFF3ADFFF);g.fill(x,y+h-2,x+w,y+h,0xFF724CFF);g.fill(x,y,x+2,y+h,0xFF3ADFFF);g.fill(x+w-2,y,x+w,y+h,0xFF724CFF);}
    private void text(GuiGraphics g,String s,int x,int y,int color){g.drawString(font,s,x,y,color,false);}
    private static int shadowCap(CompoundTag t){return Math.min(100,3+Math.max(1,t.getInt("intelligence"))/5+Math.max(1,t.getInt("level"))/10+t.getInt("shadow_capacity_bonus")+(t.getBoolean("black_heart")?20:0));}
    private static String rank(int l,boolean heart){if(l>=100&&heart)return"SHADOW MONARCH";if(l>=80)return"NATIONAL";if(l>=60)return"S";if(l>=40)return"A";if(l>=30)return"B";if(l>=20)return"C";if(l>=10)return"D";return"E";}
    private static String blank(String s,String fallback){return s==null||s.isBlank()?fallback:s;}
    private static String capitalize(String s){return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private static String shortName(Tab t){return switch(t){case STATUS->"Status";case SKILLS->"Skills";case QUESTS->"Quests";case STORE->"Store";case SHADOWS->"Shadows";case INVENTORY->"Items";};}
    private static void send(String action,String value,int amount){
        String packet = switch(action) {
            case "awaken" -> "AWAKEN";
            case "allocate" -> "ALLOCATE:" + value;
            case "exercise" -> "EXERCISE:" + value;
            case "quest" -> "CLAIM_DAILY";
            case "buy" -> "BUY:" + value;
            case "inventory" -> value.equals("store") ? "STORE_HELD" : "RETRIEVE_FIRST";
            case "shadow" -> switch(value) { case "extract" -> "EXTRACT"; case "summon" -> "SUMMON_SHADOW"; case "dismiss" -> "DISMISS_SHADOWS"; case "exchange" -> "SHADOW_EXCHANGE"; case "mode" -> "SHADOW_MODE"; default -> "TOGGLE_DOMAIN"; };
            case "authority" -> "AUTHORITY:" + value;
            default -> action.toUpperCase(Locale.ROOT) + (value.isEmpty() ? "" : ":" + value);
        };
        ModNetwork.CHANNEL.sendToServer(new ActionPacket(packet));
    }
    @Override public boolean isPauseScreen(){return false;}
}
