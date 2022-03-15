package bplusdevelopment.modules;

import bplusdevelopment.modules.combat.*;
import bplusdevelopment.modules.combat.bananabomber.BananaBomber;
import bplusdevelopment.modules.combat.monkesleeper.MonkeSleeper;
import bplusdevelopment.modules.hud.*;
import bplusdevelopment.modules.hud.stats.*;
import bplusdevelopment.modules.misc.*;
import bplusdevelopment.utils.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoCity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AddModule extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger("Banana+");
	public static final Category BANANAPLUS = new Category("Banana+ Combat");
   	public static final Category BANANAMINUS = new Category("Banana+ Misc");

	@Override
	public void onInitialize() {
	    LOG.info("Initializing Banana+ Addon");

		// Required when using @EventHandler
		MeteorClient.EVENT_BUS.registerLambdaFactory("bplusdevelopment.modules", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
		MeteorClient.EVENT_BUS.registerLambdaFactory("bplusdevelopment.utils", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

		// HUD
		HUD hud = Systems.get(HUD.class);
		hud.elements.add(new CrystalsPs(hud));
		hud.elements.add(new BananaHud(hud));
		hud.elements.add(new XpHud(hud));
		hud.elements.add(new MonkeBombsHud(hud));
		hud.elements.add(new CoordinatesHud(hud));
		hud.elements.add(new ObbyHud(hud));
		hud.elements.add(new EchestHud(hud));
		hud.elements.add(new HudLogo(hud));
		hud.elements.add(new Deaths(hud));
		hud.elements.add(new Kills(hud));
		hud.elements.add(new KillStreak(hud));
		hud.elements.add(new HighScore(hud));
		hud.elements.add(new KD(hud));
		hud.elements.add(new BindsHud(hud));


		Modules.get().add(new AutoAuto());
		Modules.get().add(new AutoCityPlus());
		Modules.get().add(new AutoEz());
		Modules.get().add(new AutoTrapPlus());
		Modules.get().add(new AutoXP());
		Modules.get().add(new AnchorPlus());
		Modules.get().add(new AntiTrap());
		Modules.get().add(new AfkLog());
		Modules.get().add(new AntiClick());
		Modules.get().add(new AntiGhostBlock());
		Modules.get().add(new AntiInvisBlock());
		Modules.get().add(new AntiNarrator());
		Modules.get().add(new AutoFollow());
		Modules.get().add(new AutoSex());
		Modules.get().add(new AutoSnowball());
		Modules.get().add(new BDiscordPresence());
		Modules.get().add(new BindClickExtra());
		Modules.get().add(new BPrefix());
		Modules.get().add(new BindClickFriend());
		Modules.get().add(new BurrowESP());
		Modules.get().add(new BurrowMiner());
		Modules.get().add(new ButtonTrap());
		Modules.get().add(new BananaBomber());
		Modules.get().add(new CevBreaker());
		Modules.get().add(new Criticals());
		Modules.get().add(new CityESPPlus());
		Modules.get().add(new CrystalClear());
		Modules.get().add(new Glide());
		Modules.get().add(new HoleESPPlus());
		Modules.get().add(new InstaMineBypass());
		Modules.get().add(new JumpIndicator());
		Modules.get().add(new LightningDeaths());
		Modules.get().add(new MonkeBurrow());
		Modules.get().add(new MonkeDetector());
		Modules.get().add(new Monkhand());
		Modules.get().add(new Mystery());
		Modules.get().add(new MonkeSleeper());
		Modules.get().add(new MonkeTotem());
		Modules.get().add(new NecroSimulator());
		Modules.get().add(new OneClickEat());
		Modules.get().add(new Panic());
		Modules.get().add(new Platform());
		Modules.get().add(new ReloadSoundSystem());
		Modules.get().add(new SkinBlinker());
		Modules.get().add(new TimeAnimator());
		Modules.get().add(new TPSSync());
		Modules.get().add(new Twerk());
		Modules.get().add(new VanillaAutoJump());
		Modules.get().add(new VolumeControl());
		Modules.get().add(new PostTickKA());
		Modules.get().add(new QuiverPlus());
		Modules.get().add(new SelfAnvilPlus());
		Modules.get().add(new SelfTrapPlus());
		Modules.get().add(new Sniper());
		Modules.get().add(new SkinBlinker());
		Modules.get().add(new SmartHoleFill());
		Modules.get().add(new StrafePlus());
		Modules.get().add(new SurroundPlus());
		Modules.get().add(new ReverseStepTimer());
		Modules.get().add(new TickShift());
		Modules.get().add(new WebNoSlow());

	    BPlusDamageUtils.init();
	    StatsUtils.init();

	}

	@Override
	public void onRegisterCategories() {
	    Modules.registerCategory(BANANAPLUS);
        Modules.registerCategory(BANANAMINUS);
	}
}
