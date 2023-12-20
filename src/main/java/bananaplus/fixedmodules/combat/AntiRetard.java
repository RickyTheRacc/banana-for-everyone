package bananaplus.fixedmodules.combat;

import bananaplus.BananaPlus;
import bananaplus.enums.BlockType;
import bananaplus.enums.SwingMode;
import bananaplus.fixedutils.CombatUtil;
import bananaplus.fixedutils.DynamicUtil;
import bananaplus.system.BananaConfig;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AntiRetard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("How close a player must be to be targeted.")
        .defaultValue(5)
        .range(0,7)
        .sliderRange(0,7)
        .build()
    );

    private final Setting<Boolean> underneath = sgGeneral.add(new BoolSetting.Builder()
        .name("underneath")
        .description("Attack the blocks under the target's feet as well.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Render your own burrow block.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> attackDelay = sgGeneral.add(new IntSetting.Builder()
        .name("attack-delay")
        .description("Tick delay between hitting blocks.")
        .defaultValue(5)
        .range(0,20)
        .build()
    );

    public final Setting<Integer> attacksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("attacks-/-tick")
        .description("How many blocks to hit in one tick.")
        .defaultValue(1)
        .range(1,5)
        .build()
    );

    public final Setting<Integer> packetAmount = sgGeneral.add(new IntSetting.Builder()
        .name("packet-amount")
        .description("How many break attempts to send per hit.")
        .defaultValue(1)
        .range(1,3)
        .build()
    );

    // Render

    private final Setting<SwingMode> swingMode = sgRender.add(new EnumSetting.Builder<SwingMode>()
        .name("swing-mode")
        .description("How to swing your hand when attempting to break.")
        .defaultValue(SwingMode.Both)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render-blocks")
        .description("Renders the blocks in the target's surround.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the blocks.")
        .defaultValue(new SettingColor(204, 0, 0, 25))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the blocks.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    public AntiRetard() {
        super(BananaPlus.FIXED, "anti-retard", "Fucks with people who use smart (double) surround.");
    }

    private final List<BlockPos> surround = new ArrayList<>();
    private PlayerEntity target;
    private double delay = 0.0;

    @Override
    public void onActivate() {
        target = (PlayerEntity) TargetUtils.get(entity -> {
            if (!(entity instanceof PlayerEntity player)) return false;
            return isValidTarget(player);
        }, SortPriority.ClosestAngle);

        if (target == null) {
            toggle();
            return;
        }

        surround.addAll(DynamicUtil.feetPos(target));
        if (underneath.get()) surround.addAll(DynamicUtil.underPos(target));

        surround.removeIf(BlockType.Hardness::resists);
        double range = BananaConfig.get().blockRange.get();
        surround.removeIf(pos -> PlayerUtils.distanceTo(pos) > range);

        delay = 0.0;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        delay -= TickRate.INSTANCE.getTickRate() / 20;
        if (delay > 0) return;

        double range = BananaConfig.get().blockRange.get();
        surround.removeIf(pos -> PlayerUtils.distanceTo(pos) > range);

        if (surround.isEmpty() || !isValidTarget(target)) {
            toggle();
            return;
        }

        Iterator<BlockPos> iterator = surround.iterator();
        int blocksHit = 0;

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();

            if (BananaConfig.get().blockRotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));

            swingMode.get().swing(Hand.MAIN_HAND);
            for (int i = 0; i < packetAmount.get(); i++) {
                Direction direction = mc.player.getEyeY() > pos.getY() ? Direction.DOWN : Direction.UP;
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, direction));
            }

            RenderUtils.renderTickingBlock(
                pos, sideColor.get(), lineColor.get(), shapeMode.get(),
                0, 4, true, false
            );

            iterator.remove();
            if (++blocksHit == attacksPerTick.get()) break;
        }

        delay = attackDelay.get();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        surround.forEach(pos -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0));
    }

    private boolean isValidTarget(PlayerEntity player) {
        double distance = PlayerUtils.squaredDistanceTo(player);
        if (distance > targetRange.get() * targetRange.get()) return false;
        if (player.isDead() || player == mc.player) return false;
        if (!CombatUtil.isInHole(player, BlockType.Resistance)) return false;

        if (player instanceof FakePlayerEntity) return true;

        if (EntityUtils.getGameMode(player) != GameMode.SURVIVAL) return false;
        return !Friends.get().isFriend(player);
    }
}
