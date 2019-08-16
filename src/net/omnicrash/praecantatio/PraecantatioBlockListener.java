package net.omnicrash.praecantatio;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;

public class PraecantatioBlockListener implements Listener
{
	private Praecantatio plugin;
	
	public PraecantatioBlockListener(Praecantatio instance)
	{
		plugin = instance;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event)
  	{
		Block block = event.getBlock();
		if (plugin.watcher.removeBlock(block))
			event.setCancelled(true);
  	}
	
}