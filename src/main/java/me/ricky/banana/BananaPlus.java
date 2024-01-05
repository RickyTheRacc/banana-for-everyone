package me.ricky.banana;

import me.ricky.banana.modules.combat.*;
import me.ricky.banana.modules.combat.Monkhand;
import me.ricky.banana.modules.misc.Sprint;
import me.ricky.banana.modules.misc.TravelLog;
import me.ricky.banana.modules.misc.WebNoSlow;
import me.ricky.banana.oldmodules.*;
import me.ricky.banana.system.BananaTab;
import me.ricky.banana.hud.*;
import com.mojang.logging.LogUtils;
import me.ricky.banana.oldutils.StatsUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.starscript.value.ValueMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class BananaPlus extends MeteorAddon {
	public static final Category COMBAT = new Category("Banana Combat", Items.END_CRYSTAL.getDefaultStack());
	public static final Category FIXED = new Category("Fixed Modules", Items.FEATHER.getDefaultStack());
	public static final HudGroup HUD_GROUP = new HudGroup("Banana+");

	public static final Version VERSION;
	public static final Logger LOG = LogUtils.getLogger();

	static {
		// Thanks for the code minegame :trollswagcat:
		ModMetadata metadata = FabricLoader.getInstance().getModContainer("banana-plus").orElseThrow().getMetadata();

		String versionString = metadata.getVersion().getFriendlyString();
		if (versionString.contains("-")) versionString = versionString.split("-")[0];
		if (versionString.equals("${version}")) versionString = "0.0.0";

		VERSION = new Version(versionString);
	}

	@Override
	public void onInitialize() {
	    LOG.info("Initializing...");

		// Banana+ Tab
		Tabs.get().add(2, new BananaTab());

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

		// Fixed Modules
		Modules.get().add(new SurroundClicker());
		Modules.get().add(new AntiTrap());
		Modules.get().add(new SurroundBuster());
		Modules.get().add(new ArmorAlerts());
		Modules.get().add(new Monkhand());
		Modules.get().add(new PhaseESP());
		Modules.get().add(new TickShift());
		Modules.get().add(new TravelLog());
		Modules.get().add(new XPThrower());
		Modules.get().add(new WebNoSlow());
		Modules.get().add(new Sprint());

		// Combat Modules
		Modules.get().add(new OldAutoTrap());
		Modules.get().add(new AnchorPlus());
		Modules.get().add(new BananaBomber());
		Modules.get().add(new HoleESPPlus());
		Modules.get().add(new MonkeBurrow());
		Modules.get().add(new OldSurround());
		Modules.get().add(new SmartHoleFill());
		Modules.get().add(new StepPlus());
		Modules.get().add(new StrafePlus());
		Modules.get().add(new OldSelfTrap());
		Modules.get().add(new ReverseStepTimer());
	}

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(FIXED);
	    Modules.registerCategory(COMBAT);
	}

	@Override
	public String getPackage() {
		return "me.ricky.banana";
	}

	@Override
	public GithubRepo getRepo() {
		return new GithubRepo("RickyTheRacc", "banana-for-everyone", "rewrite");
	}
}
