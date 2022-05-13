package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.utils.EzUtil;
import bananaplus.utils.ServerUtils.BPlusPacketUtils;
import bananaplus.utils.StatsUtils;
import bananaplus.utils.Timer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutoEz extends Module {

    public AutoEz() {
        super(BananaPlus.COMBAT, "auto-monke", "(kills) = kill count, (KS) = killstreak, (enemy) = player u killed, (KD) = kills/deaths, u can also use starscript {} see doc down below");
    }

    private final SettingGroup sgAutoEz = settings.createGroup("Auto Ez");
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgAutoCope = settings.createGroup("Auto Cope");

    // Auto Ez
    public final Setting<Boolean> autoEz = sgAutoEz.add(new BoolSetting.Builder()
            .name("auto-ez")
            .description("Sends a message when you kill a player")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> randomEz = sgAutoEz.add(new BoolSetting.Builder()
            .name("randomise")
            .description("Selects a random message from your Auto Ez message list.")
            .defaultValue(false)
            .visible(autoEz::get)
            .build());

    private final Setting<List<String>> ezMessages = sgAutoEz.add(new StringListSetting.Builder()
            .name("auto-ez-messages")
            .description("Messages to use for autoEz.")
            .defaultValue(List.of(
                    "(enemy) got ezed by Banana+ | Killstreak: (KS)",
                    "(enemy) should have bought Banana+ | Killstreak: (KS)",
                    "{server.player_count} ppl saw u die to the power of Banana+ Killstreak: (KS)",
                    "Monke (enemy) down! Banana+ | Killstreak: (KS)",
                    "Currently on a (KS) killstreak thanks to Banana+",
                    "Currently on (KD) K/D thanks to Banana+",
                    "Buy B+ at: https://discord.gg/H9PdQfuAG3 Killstreak: (KS)"))
            .onChanged(strings -> recompileEz()).visible(autoEz::get)
            .build());

    // Totem Pops
    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder()
            .name("totem-pops")
            .description("Sends a message when u pop a players totem.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> msgDelay = sgTotemPops.add(new IntSetting.Builder()
            .name("msg-delay")
            .description("In ticks, 20 ticks = 1 sec")
            .defaultValue(60)
            .min(1)
            .sliderMax(200)
            .visible(totemPops::get)
            .build());

    private final Setting<Double> popRange = sgTotemPops.add(new DoubleSetting.Builder()
            .name("pop-range")
            .description("The radius in which it will announce totem pops.")
            .defaultValue(8)
            .range(1, 20)
            .sliderRange(1, 20)
            .visible(totemPops::get)
            .build());

    private final Setting<Boolean> randomPop = sgTotemPops.add(new BoolSetting.Builder()
            .name("randomise")
            .description("Selects a random message from your totem pop message list.")
            .defaultValue(true)
            .visible(totemPops::get)
            .build());

    private final Setting<List<String>> popMessages = sgTotemPops.add(new StringListSetting.Builder()
            .name("pop-messages")
            .description("Messages to use for totem pops, u can use (enemy) and startscript shortcuts in totem msgs")
            .defaultValue(List.of(
                    "Easily popped (enemy) with the power of Banana+",
                    "(enemy) needs a new totem :* | Banana+",
                    "(enemy) popping! Banana+",
                    "Monke (enemy) almost down! Banana+"))
            .onChanged(strings -> recompilePop())
            .visible(totemPops::get)
            .build());

    // Auto Cope
    public final Setting<Boolean> autoCope = sgAutoCope.add(new BoolSetting.Builder()
            .name("auto-cope")
            .description("Sends a message when you die")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> randomCope = sgAutoCope.add(new BoolSetting.Builder()
            .name("randomise")
            .description("Selects a random message from your autoCope message list.")
            .defaultValue(true)
            .visible(autoCope::get)
            .build());

    private final Setting<List<String>> copeMessages = sgAutoCope.add(new StringListSetting.Builder()
            .name("auto-cope-messages")
            .description("Messages to use for Auto Cope.")
            .defaultValue(List.of(
                    "Don't die like I did",
                    "I do not believe in my death",
                    "I lagged",
                    "Monke down!",
                    "My screen froze",
                    "I was typing"))
            .onChanged(strings -> recompileCope())
            .visible(autoCope::get)
            .build());

    private final List<Script> autoEzScripts = new ArrayList<>();
    private final List<Script> popScripts = new ArrayList<>();
    private final List<Script> autoCopeScripts = new ArrayList<>();

    String autoEzMsg;
    String autoPopMsg;
    String autoCopeMsg;

    private int messageEzI;
    private int messagePopI;
    private int messageCopeI;

    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();

    @Override
    public void onActivate() {
        messageEzI = 0;
        messagePopI = 0;
        messageCopeI = 0;
        totemPopMap.clear();
        chatIdMap.clear();
        recompilePop();
        recompileEz();
    }

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    MeteorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }
    }

    private void recompileEz() {
        recompile(ezMessages.get(), autoEzScripts);
    }
    private void recompilePop() {
        recompile(popMessages.get(), popScripts);
    }
    private void recompileCope() { recompile(copeMessages.get(), autoCopeScripts); }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (totemPops.get()) {
            if (!(event.packet instanceof EntityStatusS2CPacket)) return;
            EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
            if (p.getStatus() != 35) return;
            Entity entity = p.getEntity(mc.world);
            if (!(entity instanceof PlayerEntity)) return;
            if (entity.equals(mc.player)|| Friends.get().isFriend((PlayerEntity) entity)) return;

            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), pops++);

            int i;
            if (randomPop.get()) {
                i = Utils.random(0, popScripts.size());
            } else {
                if (messagePopI >= popScripts.size()) messagePopI = 0;
                i = messagePopI++;
            }

            if (mc.player.distanceTo(entity) <= popRange.get() && !mc.player.isDead() && msgTimer.passedTicks(msgDelay.get()) && !popMessages.get().isEmpty()) {
            autoPopMsg = MeteorStarscript.ss.run(popScripts.get(i));
            mc.player.sendChatMessage(autoPopMsg.replace("(enemy)", entity.getEntityName()));
            msgTimer.reset();
            }
        }
    }

    public void sendCopeMessage() {
        int i;
        if (randomCope.get()) {
            i = Utils.random(0, autoCopeScripts.size());
        } else {
            if (messageCopeI >= autoCopeScripts.size()) messageCopeI = 0;
            i = messageCopeI++;
        }

        autoCopeMsg = MeteorStarscript.ss.run(autoCopeScripts.get(i));

        MeteorClient.mc.player.sendChatMessage(autoCopeMsg); }

    public void sendEzMessage() {
        int i;
        if (randomEz.get()) {
            i = Utils.random(0, autoEzScripts.size());
        } else {
            if (messageEzI >= autoEzScripts.size()) messageEzI = 0;
            i = messageEzI++;
        }

        autoEzMsg = MeteorStarscript.ss.run(autoEzScripts.get(i));

        mc.player.sendChatMessage(autoEzMsg.replace("(enemy)", BPlusPacketUtils.deadEntity.getEntityName()).replace("(KS)", StatsUtils.killStreak.toString()).replace("(KD)", EzUtil.stringKD()).replace("(kills)", StatsUtils.kills.toString()).replace("(deaths)", StatsUtils.deaths.toString()));
    }

    private Timer msgTimer = new Timer();

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton help = theme.button("Open documentation.");
        help.action = () -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Starscript");

        return help;
    }
}