package me.ricky.banana;

import com.mojang.logging.LogUtils;
import me.ricky.banana.hud.BindsHud;
import me.ricky.banana.hud.LogoHud;
import me.ricky.banana.hud.PotionsHud;
import me.ricky.banana.hud.TextPresets;
import me.ricky.banana.modules.movement.Blink;
import me.ricky.banana.modules.movement.Sprint;
import me.ricky.banana.modules.render.LogoutSpots;
import me.ricky.banana.systems.BananaTab;
import me.ricky.banana.utils.StatsUtils;
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
import org.slf4j.Logger;

public class BananaPlus extends MeteorAddon {
	public static final Category CATEGORY = new Category("Banana Plus");
	public static final HudGroup HUD_GROUP = new HudGroup("Banana Plus");
	public static final Logger LOG = LogUtils.getLogger();
	public static final Version VERSION;

	static {
		ModMetadata metadata = FabricLoader.getInstance().getModContainer("banana-plus").orElseThrow().getMetadata();

		String versionString = metadata.getVersion().getFriendlyString();
		if (versionString.contains("-")) versionString = versionString.split("-")[0];
		if (versionString.equals("${version}")) versionString = "0.0.0";

		VERSION = new Version(versionString);
	}

	@Override
	public void onInitialize() {
		LOG.info("Initializing...");

		// Add system tab

		Tabs.get().add(2, new BananaTab());

		// Starscript

		MeteorStarscript.ss.set("banana", new ValueMap()
			.set("kills", StatsUtils::getKills)
			.set("deaths", StatsUtils::getDeaths)
			.set("kdr", StatsUtils::getKDR)
			.set("killstreak", StatsUtils::getKillstreak)
			.set("highscore", StatsUtils::getHighscore)
			.set("crystalsps", StatsUtils::getCrystalsPs)
		);

		// Add hud elements

		Hud.get().register(BindsHud.INFO);
		Hud.get().register(LogoHud.INFO);
		Hud.get().register(PotionsHud.INFO);
		Hud.get().register(TextPresets.INFO);

		// Add modules

		Modules.get().add(new Blink());
		Modules.get().add(new Sprint());
		Modules.get().add(new LogoutSpots());
	}

	@Override
	public void onRegisterCategories() {
		Modules.registerCategory(CATEGORY);
	}

	@Override
	public String getPackage() {
		return "me.ricky.banana";
	}

	@Override
	public GithubRepo getRepo() {
		return new GithubRepo("RickyTheRacc", "banana-for-everyone", "take-2");
	}
}
