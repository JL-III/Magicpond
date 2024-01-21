package com.playtheatria.jliii.magicpond;

import com.playtheatria.jliii.magicpond.listeners.PlayerFish;
import com.playtheatria.jliii.magicpond.util.ListGenerators;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Magicpond extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new PlayerFish(ListGenerators.loadMagicPondLocations(), this), this);
    }
}
