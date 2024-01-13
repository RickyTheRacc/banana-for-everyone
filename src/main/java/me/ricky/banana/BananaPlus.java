package me.ricky.banana;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;

public class BananaPlus extends MeteorAddon {
	@Override
	public void onInitialize() {

	}

	@Override
	public void onRegisterCategories() {

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
