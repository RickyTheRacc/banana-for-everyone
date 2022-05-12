package bananaplus.modules.combat;

import bananaplus.modules.AddModule;
import bananaplus.utils.BPlusEntityUtils;
import bananaplus.utils.BPlusWorldUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class CevBreaker extends Module {

    public enum Mode {Normal, Packet, Instant};

    private final SettingGroup sgGeneral = settings.createGroup("general");
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Whether to rotate or not.").defaultValue(false).build());
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").description("Which mode to use for breaking the obsidian.").defaultValue(Mode.Packet).build());
    private final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder().name("smart-delay").description("Waits until the target can get damaged again with breaking the block.").visible(() -> mode.get() == Mode.Instant).defaultValue(true).build());
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder().name("swing").description("Renders your swing client-side.").defaultValue(true).build());
    //old private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder().name("switch-delay").description("How many ticks to wait before after switching hotbar slots.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder().name("switch-delay").description("How many ticks to wait before hitting an entity after switching hotbar slots.").defaultValue(1).min(0).sliderMax(10).visible(() -> mode.get() == Mode.Packet).build());


    public CevBreaker() { super(AddModule.COMBAT, "cev-breaker", "Automatically places an obsidian block and a crystal on top of the target and breaks the obby and crystal to deal massive damage."); }

    private PlayerEntity closestTarget;
    private boolean startedYet;
    private int switchDelayLeft, timer, breakDelayLeft;
    private List<PlayerEntity> blacklisted = new ArrayList<>();
    private List<EndCrystalEntity> crystals = new ArrayList<>();

    @EventHandler
    public void onActivate() {
        closestTarget = null;
        startedYet = false;
        switchDelayLeft = 0;
        timer = 0;
        blacklisted.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switchDelayLeft--;
        breakDelayLeft--;
        timer--;
        int crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).slot();
        int obsidianSlot = InvUtils.findInHotbar(Items.OBSIDIAN).slot();
        int pickSlot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).slot();
        pickSlot = pickSlot == -1 ? InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).slot() : pickSlot;

        if((crystalSlot == -1 && !(mc.player.getOffHandStack().getItem() instanceof EndCrystalItem)) || obsidianSlot == -1 || pickSlot == -1) {
            warning("No " + (crystalSlot == -1 && !(mc.player.getOffHandStack().getItem() instanceof EndCrystalItem) ? "crystals" : (obsidianSlot == -1 ? "obsidian" :  "pickaxe")) + " found, disabling...");
            toggle();
            return;
        }
        getEntities();
        if(closestTarget == null) {
            error( "No target found, disabling...");
            toggle();
            return;
        }

        BlockPos blockPos = closestTarget.getBlockPos().add(0, 2, 0);
        BlockState blockState = mc.world.getBlockState(blockPos);
        float[] rotation = PlayerUtils.calculateAngle(new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
        boolean crystalThere = false;
        for(EndCrystalEntity crystal : crystals) {      //Check for crystal in right pos
            if(crystal.getBlockPos().add(0, -1, 0).equals(blockPos)) {
                crystalThere = true;
                break;
            }
        }
        //Placing obby
        if(!blockState.isOf(Blocks.OBSIDIAN) && !crystalThere && (mc.player.getMainHandStack().getItem().equals(Items.OBSIDIAN) || switchDelayLeft <= 0)) {
            if(!BPlusWorldUtils.place(blockPos, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 50, BPlusWorldUtils.SwitchMode.Client, BPlusWorldUtils.PlaceMode.Client, false, BPlusWorldUtils.AirPlaceDirection.Down, swing.get(), true, true)) {
                blacklisted.add(closestTarget);
                getEntities();
                if(closestTarget == null) {
                    warning("Can't place obsidian above the target! Disabling...");
                    toggle();
                    return;
                }
                else {
                    return;
                }
            }
        }

        //Placing crystal
        boolean offhand = mc.player.getOffHandStack().getItem() instanceof EndCrystalItem;
        boolean mainhand = mc.player.getMainHandStack().getItem() instanceof EndCrystalItem;
        if(!crystalThere && blockState.isOf(Blocks.OBSIDIAN)) {
            if(!(offhand || mainhand || switchDelayLeft <= 0)) return;
            double x = blockPos.up().getX();
            double y = blockPos.up().getY();
            double z = blockPos.up().getZ();

            if(!mc.world.getOtherEntities(null, new Box(x, y, z, x + 1D, y + 2D, z + 1D)).isEmpty()
                    || !mc.world.getBlockState(blockPos.up()).isAir()) {
                blacklisted.add(closestTarget);
                getEntities();
                if(closestTarget == null) {
                    warning("Can't place the crystal! Disabling...");
                    toggle();
                    return;
                }
                else {
                    return;
                }
            }
            else {
                if(!offhand && !mainhand) mc.player.getInventory().selectedSlot = crystalSlot;
                Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                BlockHitResult result = new BlockHitResult(mc.player.getPos(), blockPos.getY() < mc.player.getY() ? Direction.UP : Direction.DOWN, blockPos, false);
                if(swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                if(rotate.get()) {
                    Rotations.rotate(rotation[0], rotation[1], 25, () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result)));
                } else mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, result));
            }
        }
        //Breaking obby
        if(blockState.isAir() && mode.get() == Mode.Packet) startedYet = false;
        if((mc.player.getInventory().selectedSlot == pickSlot || switchDelayLeft <= 0) && crystalThere && blockState.isOf(Blocks.OBSIDIAN)) {
            Direction direction = BPlusEntityUtils.rayTraceCheck(blockPos, true);
            if(mode.get() == Mode.Instant) {
                if(!startedYet) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
                    startedYet = true;
                } else {
                    if(smartDelay.get() && closestTarget.hurtTime > 0) return;
                    mc.player.getInventory().selectedSlot = pickSlot;
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
                }
            }
            else if(mode.get() == Mode.Normal) {
                mc.player.getInventory().selectedSlot = pickSlot;
                mc.interactionManager.updateBlockBreakingProgress(blockPos, direction);
            }
            else if(mode.get() == Mode.Packet) {
                timer = startedYet ? timer : BPlusEntityUtils.getBlockBreakingSpeed(blockState, blockPos, pickSlot);
                if(!startedYet) {
                    mine(blockPos, swing.get(), rotate.get());
                    startedYet = true;
                }
                else if(timer <= 0){
                    mc.player.getInventory().selectedSlot = pickSlot;
                }
            }
        }
        //Breaking the crystal
        BananaBomber BBomber = Modules.get().get(BananaBomber.class);
        if(BBomber.bestTarget == null || BBomber.bestTarget != closestTarget || BBomber.BminDamage.get() >= 6) {
            if (mode.get() == Mode.Packet && breakDelayLeft >= 0) return;
            for(EndCrystalEntity crystal : crystals) {
                if(DamageUtils.crystalDamage(closestTarget, crystal.getPos()) >= 6) {
                    float[] breakRotation = PlayerUtils.calculateAngle(BPlusEntityUtils.crystalEdgePos(crystal));
                    if(swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
                    else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    if(rotate.get()) Rotations.rotate(breakRotation[0], breakRotation[1], 30, () -> mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false)));
                    else mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false));
                    break;
                }
            }
        }
    }

    private void getEntities() {
        closestTarget = null;
        crystals.clear();
        for(Entity entity : mc.world.getEntities()) {
            if(entity.isInRange(mc.player, 6))
                if(entity.isAlive())
                    if(entity instanceof PlayerEntity) {
                        if(entity != mc.player)
                            if(Friends.get().shouldAttack((PlayerEntity) entity))
                                if(closestTarget == null || mc.player.distanceTo(entity) < mc.player.distanceTo(closestTarget))
                                    if(!blacklisted.contains(entity))
                                        closestTarget = (PlayerEntity) entity;
                    } else if(entity instanceof EndCrystalEntity)
                        crystals.add((EndCrystalEntity) entity);
        }
    }

    public void mine(BlockPos blockPos, boolean swing, boolean rotate) {
        if(rotate) {
            Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mine(blockPos, swing, false));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            if(swing) mc.player.swingHand(Hand.MAIN_HAND);
            else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchDelayLeft = 1;
            breakDelayLeft = switchDelay.get();
        }
    }

    @Override
    public String getInfoString() {
        if (closestTarget != null) return closestTarget.getEntityName();
        return null;
    }
}
