package me.omnicrash.praecantatio;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

public class PraecantatioBlockListener extends BlockListener
{
	private Praecantatio plugin;
	
	public PraecantatioBlockListener(Praecantatio instance)
	{
		plugin = instance;
	}
	
	public void onBlockBreak(BlockBreakEvent event)
  	{
		Block block = event.getBlock();
		if (plugin.watcher.removeBlock(block))
			event.setCancelled(true);
  	}
	
}