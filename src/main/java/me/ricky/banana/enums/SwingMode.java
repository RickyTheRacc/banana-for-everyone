package me.ricky.banana.enums;

import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum SwingMode {
    Both,
    Packet,
    Client,
    None;

    public void swing(Hand hand) {
        if (this == Both || this == Client) mc.player.swingHand(hand, false);
        if (this == Both || this == Packet) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }
}
