package bananaplus;

import bananaplus.modules.combat.*;
import bananaplus.modules.combat.BananaBomber;
import bananaplus.modules.hud.*;
import bananaplus.modules.misc.*;
import bananaplus.utils.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.value.ValueMap;
import net.minecraft.item.Items;

public class BananaPlus extends MeteorAddon {
	public static final Category COMBAT = new Category("Banana Combat", Items.END_CRYSTAL.getDefaultStack());
   	public static final Category MISC = new Category("Banana Misc.", Items.GOLDEN_APPLE.getDefaultStack());
	public static final HudGroup HUD_GROUP = new HudGroup("Banana+");

	@Override
	public void onInitialize() {
	    Log("Beginning initialization.");


		// Starscript Placeholders
		Log("Adding Starscript placeholders...");

		MeteorStarscript.ss.set("banana", new ValueMap()
				.set("kills", StatsUtils::getKills)
				.set("deaths", StatsUtils::getDeaths)
				.set("kdr", StatsUtils::getKDR)
				.set("killstreak", StatsUtils::getKillstreak)
				.set("highscore", StatsUtils::getHighscore)
				.set("crystalsps", StatsUtils::getCrystalsPs)
				.set("discord", "https://discord.gg/tByq7JXakQ")
		);


		// HUD
		Log("Adding HUD modules...");

		Hud.get().register(ItemCounter.INFO);
		Hud.get().register(BindsHud.INFO);
		Hud.get().register(LogoHud.INFO);
		Hud.get().register(WelcomeHud.INFO);
		Hud.get().register(TextPresets.INFO);


		// Combat
		Log("Adding Combat modules...");

		Modules.get().add(new ArmorMessages());
		Modules.get().add(new AutoCityPlus());
		Modules.get().add(new AutoTrapPlus());
		Modules.get().add(new XPThrower());
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
		Modules.get().add(new Monkhand());
		Modules.get().add(new MonkeTotem());
		Modules.get().add(new PostTickKA());
		Modules.get().add(new QuiverPlus());
		Modules.get().add(new QuiverRewrite());
		Modules.get().add(new SelfTrapPlus());
		Modules.get().add(new SmartHoleFill());
		Modules.get().add(new StepPlus());
		Modules.get().add(new StrafePlus());
		Modules.get().add(new SurroundPlus());
		Modules.get().add(new ReverseStepTimer());
		Modules.get().add(new TickShift());


		// Misc
		Log("Adding Other modules...");

		Modules.get().add(new AutoBuild());
		Modules.get().add(new AfkLog());
		Modules.get().add(new AntiGlitchBlock());
		Modules.get().add(new AutoFollow());
		Modules.get().add(new AutoSex());
		Modules.get().add(new BindClickExtra());
		Modules.get().add(new InstaMinePlus());
		Modules.get().add(new KillEffects());
		Modules.get().add(new MonkeFlight());
		Modules.get().add(new OneClickEat());
		Modules.get().add(new Platform());
		Modules.get().add(new PrefixManager());
		Modules.get().add(new Presence());
		Modules.get().add(new SkinBlinker());
		Modules.get().add(new TimeAnimator());
		Modules.get().add(new TPSSync());
		Modules.get().add(new Twerk());
		Modules.get().add(new WebNoSlow());


		Log("Initialized successfully!");
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

	public static void Log(String text) {
		System.out.println("[Banana+] " + text);
	}
}
