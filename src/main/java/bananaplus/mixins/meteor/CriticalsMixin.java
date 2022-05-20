package bananaplus.mixins.meteor;

import bananaplus.modules.combat.PostTickKA;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Criticals;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Criticals.class)
public abstract class CriticalsMixin {

    @Shadow(remap = false)
    @Final
    private Setting<Criticals.Mode> mode;


    @Shadow protected abstract boolean skipCrit();
    @Shadow protected abstract void sendPacket(double height);
    @Shadow private boolean sendPackets;
    @Shadow private int sendTimer;
    @Shadow private PlayerInteractEntityC2SPacket attackPacket;
    @Shadow private HandSwingC2SPacket swingPacket;
    @Shadow @Final private Setting<Boolean> ka;


    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof IPlayerInteractEntityC2SPacket packet && packet.getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (skipCrit()) return;

            Entity entity = packet.getEntity();

            if (!(entity instanceof LivingEntity) || ((entity != Modules.get().get(KillAura.class).getTarget() && entity != Modules.get().get(PostTickKA.class).getTarget() && ka.get())))
                return;

            switch (mode.get()) {
                case Packet -> {
                    sendPacket(0.0625);
                    sendPacket(0);
                }
                case Bypass -> {
                    sendPacket(0.11);
                    sendPacket(0.1100013579);
                    sendPacket(0.0000013579);
                }
                default -> {
                    if (!sendPackets) {
                        sendPackets = true;
                        sendTimer = mode.get() == Criticals.Mode.Jump ? 6 : 4;
                        attackPacket = (PlayerInteractEntityC2SPacket) event.packet;

                        if (mode.get() == Criticals.Mode.Jump) mc.player.jump();
                        else ((IVec3d) mc.player.getVelocity()).setY(0.25);
                        event.cancel();
                    }
                }
            }
        } else if (event.packet instanceof HandSwingC2SPacket && mode.get() != Criticals.Mode.Packet) {
            if (skipCrit()) return;

            if (sendPackets && swingPacket == null) {
                swingPacket = (HandSwingC2SPacket) event.packet;

                event.cancel();
            }
        }
    }
}
