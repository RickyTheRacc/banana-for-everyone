package bananaplus.modules.combat;

import bananaplus.modules.BananaPlus;
import bananaplus.modules.misc.AntiGhostBlock;
import bananaplus.utils.BPlusEntityUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

public class AutoAuto extends Module {
    private final SettingGroup sgAntiGhost = settings.createGroup("Anti Ghost Surround");
    private final SettingGroup sgSurround = settings.createGroup("Surround+");
    private final SettingGroup sgAutoCity = settings.createGroup("Auto City+");
    private final SettingGroup sgBurrowMiner = settings.createGroup("Burrow Miner");


    // Anti Ghost
    private final Setting<Boolean> antiGhost = sgAntiGhost.add(new BoolSetting.Builder()
            .name("anti-ghost-surround")
            .description("Automatically turns on Anti Ghost Block if Surround+ is on and you are surrounded.")
            .defaultValue(false)
            .build()
    );


    // Surround
    private final Setting<Boolean> surroundPlus = sgSurround.add(new BoolSetting.Builder()
            .name("surround+")
            .description("Automatically turns on surround+ once you are in a hole.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> onlyGround = sgSurround.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .defaultValue(true)
            .visible(surroundPlus::get)
            .build()
    );

    private final Setting<Boolean> allowDouble = sgSurround.add(new BoolSetting.Builder()
            .name("allow-doubles")
            .defaultValue(false)
            .visible(surroundPlus::get)
            .build()
    );


    // Auto City
    private final Setting<Boolean> autoCity = sgAutoCity.add(new BoolSetting.Builder()
            .name("auto-city+")
            .description("Automatically turns on Auto City+ if the closest target to you is burrowed / surrounded.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> ACtargetRange = sgAutoCity.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("Maximum target range for Auto City+ automation")
            .defaultValue(5)
            .sliderRange(0,7)
            .visible(autoCity::get)
            .build()
    );

    private final Setting<Boolean> AConlyinHole = sgAutoCity.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .defaultValue(false)
            .visible(autoCity::get)
            .build()
    );

    private final Setting<Boolean> ACallowDoubleHole = sgAutoCity.add(new BoolSetting.Builder()
            .name("allow-doubles")
            .defaultValue(false)
            .visible(() -> autoCity.get() && AConlyinHole.get())
            .build()
    );


    // Burrow Miner
    private final Setting<Boolean> burrowMiner = sgBurrowMiner.add(new BoolSetting.Builder()
            .name("burrow-miner")
            .description("Automatically turns on Burrow Miner if the closest target to you is burrowed.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> BMtargetRange = sgBurrowMiner.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("Maximum target range for Burrow Miner automation")
            .defaultValue(5)
            .sliderRange(0,7)
            .visible(burrowMiner::get)
            .build()
    );

    private final Setting<Boolean> BMonlyinHole = sgBurrowMiner.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .defaultValue(false)
            .visible(burrowMiner::get)
            .build()
    );

    private final Setting<Boolean> BMallowDoubleHole = sgBurrowMiner.add(new BoolSetting.Builder()
            .name("allow-doubles")
            .defaultValue(false)
            .visible(() -> burrowMiner.get() && BMonlyinHole.get())
            .build()
    );


    public AutoAuto() {
        super(BananaPlus.COMBAT, "auto-auto", "Automates automation");
    }


    private boolean shouldAntiGhost;
    private boolean didAntiGhost;


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Fields
        Modules modules = Modules.get();
        SurroundPlus SP = modules.get(SurroundPlus.class);
        AntiGhostBlock AGB = modules.get(AntiGhostBlock.class);
        AutoCityPlus AC = modules.get(AutoCityPlus.class);
        BurrowMiner BM = modules.get(BurrowMiner.class);

        if (surroundPlus.get()) {
            if ((onlyGround.get() && mc.player.isOnGround()) || !onlyGround.get()) {
                if (((BPlusEntityUtils.isInHole(mc.player, true, BPlusEntityUtils.BlastResistantType.Any) && allowDouble.get()) || (BPlusEntityUtils.isSurrounded(mc.player, BPlusEntityUtils.BlastResistantType.Any)))
                        && !SP.isActive()) {
                    SP.toggle();
                }
            }
        }

        if (antiGhost.get()) {
            if (SP.isActive() && BPlusEntityUtils.isSurrounded(mc.player, BPlusEntityUtils.BlastResistantType.Any) && !mc.player.getAbilities().creativeMode) {
                shouldAntiGhost = true;
                if (!AGB.isActive() && AGB.autoToggle.get() && shouldAntiGhost && !didAntiGhost) {
                    AGB.toggle();
                    shouldAntiGhost = false;
                    didAntiGhost = true;
                }

                if (!AGB.autoToggle.get() && AGB.isActive() && shouldAntiGhost && !didAntiGhost) {
                    AGB.toggle();
                    AGB.toggle();
                    shouldAntiGhost = false;
                    didAntiGhost = true;
                }

            } else {
                shouldAntiGhost = false;
                didAntiGhost = false;
            }
        }

        if (autoCity.get()) {
            if ((AConlyinHole.get() && (BPlusEntityUtils.isSurrounded(mc.player, BPlusEntityUtils.BlastResistantType.Any) || (BPlusEntityUtils.isInHole(mc.player, true, BPlusEntityUtils.BlastResistantType.Any) && ACallowDoubleHole.get()))) || !AConlyinHole.get()) {
            if (!AC.isActive()) {
                PlayerEntity ACtarget = TargetUtils.getPlayerTarget(ACtargetRange.get(), SortPriority.LowestDistance);

                if (ACtarget == null) return;
                else {
                    if (BPlusEntityUtils.isBurrowed(ACtarget, BPlusEntityUtils.BlastResistantType.Mineable)
                            || (BPlusEntityUtils.isSurrounded(ACtarget, BPlusEntityUtils.BlastResistantType.Mineable))) {
                        AC.toggle();
                        }
                    }
                }
            }
        }

        if (burrowMiner.get()){
            if ((BMonlyinHole.get() && BPlusEntityUtils.isSurrounded(mc.player, BPlusEntityUtils.BlastResistantType.Any) || (BPlusEntityUtils.isInHole(mc.player, true, BPlusEntityUtils.BlastResistantType.Any) && BMallowDoubleHole.get())) || !BMonlyinHole.get()) {
                if (!BM.isActive()) {
                    PlayerEntity BMtarget = TargetUtils.getPlayerTarget(BMtargetRange.get(), SortPriority.LowestDistance);

                    if (BMtarget == null) return;
                    else {
                        if (BPlusEntityUtils.isBurrowed(BMtarget, BPlusEntityUtils.BlastResistantType.Any)) {
                            BM.toggle();
                        }
                    }
                }
            }
        }
    }
}
