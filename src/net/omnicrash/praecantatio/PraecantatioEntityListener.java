package net.omnicrash.praecantatio;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.Listener;

public class PraecantatioEntityListener implements Listener
{
	private Praecantatio plugin;

    public PraecantatioEntityListener(Praecantatio instance)
    {
        plugin = instance;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            Player player = (Player)event.getEntity();
            
            if (plugin.watcher.getTicks(player, "Protect") > 0)
                event.setCancelled(true);
            /*
            else if ((LastSpell.trim().split(" ")[0].equalsIgnoreCase("celeritas")) && (event.getCause() == EntityDamageEvent.DamageCause.FALL)) {
              event.setCancelled(true);
            }
            */
        }
    }

}