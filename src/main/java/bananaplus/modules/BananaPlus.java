package bananaplus.modules;

import bananaplus.modules.combat.*;
import bananaplus.modules.hud.*;
import bananaplus.modules.hud.stats.*;
import bananaplus.modules.misc.*;
import bananaplus.utils.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class BananaPlus extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger("Banana+");
	public static final Category COMBAT = new Category("Banana Combat", Items.END_CRYSTAL.getDefaultStack());
   	public static final Category MISC = new Category("Banana Misc.", Items.GOLDEN_APPLE.getDefaultStack());

	@Override
	public void onInitialize() {
	    LOG.info("Initializing Banana+ Addon");


		// Required when using @EventHandler
		MeteorClient.EVENT_BUS.registerLambdaFactory("bananaplus.modules", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
		MeteorClient.EVENT_BUS.registerLambdaFactory("bananaplus.utils", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));


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


		// Combat
		Modules.get().add(new AutoCityPlus());
		Modules.get().add(new AutoTrapPlus());
		Modules.get().add(new AutoXP());
		Modules.get().add(new AnchorPlus());
		Modules.get().add(new AntiTrap());
		Modules.get().add(new BurrowESP());
		Modules.get().add(new BurrowMiner());
		Modules.get().add(new AntiSurround());
		Modules.get().add(new BananaBomber());
		Modules.get().add(new CevBreaker());
		Modules.get().add(new CityESPPlus());
		Modules.get().add(new HoleESPPlus());
		Modules.get().add(new MonkeBurrow());
		Modules.get().add(new MonkeDetector());
		Modules.get().add(new Monkhand());
		Modules.get().add(new MonkeSleeper());
		Modules.get().add(new MonkeTotem());
		Modules.get().add(new PostTickKA());
		Modules.get().add(new QuiverPlus());
		Modules.get().add(new SelfTrapPlus());
		Modules.get().add(new SmartHoleFill());
		Modules.get().add(new StepPlus());
		Modules.get().add(new StrafePlus());
		Modules.get().add(new SurroundPlus());
		Modules.get().add(new ReverseStepTimer());
		Modules.get().add(new TickShift());


		// Misc
		Modules.get().add(new AfkLog());
		Modules.get().add(new AntiGlitchBlock());
		Modules.get().add(new AntiNarrator());
		Modules.get().add(new AutoFollow());
		Modules.get().add(new AutoSex());
		Modules.get().add(new BDiscordPresence());
		Modules.get().add(new BindClickExtra());
		Modules.get().add(new BPrefix());
		Modules.get().add(new BindClickFriend());
		Modules.get().add(new InstaMinePlus());
		Modules.get().add(new KillEffects());
		Modules.get().add(new MonkeFlight());
		Modules.get().add(new OneClickEat());
		Modules.get().add(new Platform());
		Modules.get().add(new SkinBlinker());
		Modules.get().add(new TimeAnimator());
		Modules.get().add(new TPSSync());
		Modules.get().add(new Twerk());
		Modules.get().add(new WebNoSlow());


		// Utils
	    BPlusDamageUtils.init();
	    StatsUtils.init();
	}

	@Override
	public void onRegisterCategories() {
	    Modules.registerCategory(COMBAT);
        Modules.registerCategory(MISC);
	}
}
