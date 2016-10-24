package me.omnicrash.praecantatio;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

public class PraecantatioEntityListener extends EntityListener
{
	private Praecantatio plugin;

    public PraecantatioEntityListener(Praecantatio instance)
    {
        plugin = instance;
    }

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