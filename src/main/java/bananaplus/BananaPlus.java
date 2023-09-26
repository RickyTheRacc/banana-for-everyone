package bananaplus;

import bananaplus.hud.*;
import bananaplus.modules.combat.*;
import bananaplus.modules.combat.BananaBomber;
import bananaplus.modules.misc.*;
import bananaplus.utils.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class BananaPlus extends MeteorAddon {
	public static final Category COMBAT = new Category("Banana Combat", Items.END_CRYSTAL.getDefaultStack());
   	public static final Category MISC = new Category("Banana Misc.", Items.GOLDEN_APPLE.getDefaultStack());
	public static final HudGroup HUD_GROUP = new HudGroup("Banana+");
	public static final Logger LOG = LogUtils.getLogger();

	@Override
	public void onInitialize() {
	    LOG.info("Initializing...");

		// Starscript Values
		MeteorStarscript.ss.set("banana", new ValueMap()
				.set("kills", StatsUtils::getKills)
				.set("deaths", StatsUtils::getDeaths)
				.set("kdr", StatsUtils::getKDR)
				.set("killstreak", StatsUtils::getKillstreak)
				.set("highscore", StatsUtils::getHighscore)
				.set("crystalsps", StatsUtils::getCrystalsPs)
		);

		// Hud Modules
		Hud.get().register(ItemCounter.INFO);
		Hud.get().register(BindsHud.INFO);
		Hud.get().register(LogoHud.INFO);
		Hud.get().register(WelcomeHud.INFO);
		Hud.get().register(TextPresets.INFO);

		// Combat Modules
		Modules.get().add(new ArmorMessages());
		Modules.get().add(new AutoTrapPlus());
		Modules.get().add(new XPThrower());
		Modules.get().add(new AnchorPlus());
		Modules.get().add(new AntiTrap());
		Modules.get().add(new BurrowESP());
		Modules.get().add(new AntiSurround());
		Modules.get().add(new BananaBomber());
		Modules.get().add(new CevBreaker());
		Modules.get().add(new HoleESPPlus());
		Modules.get().add(new MonkeBurrow());
		Modules.get().add(new Monkhand());
		Modules.get().add(new MonkeTotem());
		Modules.get().add(new SelfTrapPlus());
		Modules.get().add(new SmartHoleFill());
		Modules.get().add(new StepPlus());
		Modules.get().add(new StrafePlus());
		Modules.get().add(new SurroundPlus());
		Modules.get().add(new ReverseStepTimer());
		Modules.get().add(new TickShift());

		// Misc Modules
		Modules.get().add(new AutoBuild());
		Modules.get().add(new AfkLog());
		Modules.get().add(new InstaMinePlus());
		Modules.get().add(new KillEffects());
		Modules.get().add(new OneClickEat());
		Modules.get().add(new Platform());
		Modules.get().add(new PrefixManager());
		Modules.get().add(new Presence());
		Modules.get().add(new SkinBlinker());
		Modules.get().add(new TimeAnimator());
		Modules.get().add(new WebNoSlow());
	}

	@Override
	public void onRegisterCategories() {
	    Modules.registerCategory(COMBAT);
        Modules.registerCategory(MISC);
	}

	@Override
	public String getPackage() {
		return "bananaplus";
	}

	@Override
	public GithubRepo getRepo() {
		return new GithubRepo("RickyTheRacc", "banana-for-everyone", "main");
	}
}
