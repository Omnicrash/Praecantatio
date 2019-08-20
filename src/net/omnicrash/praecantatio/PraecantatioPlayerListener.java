package net.omnicrash.praecantatio;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
//import org.bukkit.entity.CreatureType;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Dye;
import org.bukkit.util.Vector;

public class PraecantatioPlayerListener implements Listener
{
    private Praecantatio plugin;
    
    public PraecantatioPlayerListener(Praecantatio instance)
    {
        plugin = instance;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

	@EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
    	Player player = event.getPlayer();
    	
        if(plugin.watcher.getTicks(player, "SlowFall") > 0)
        {
        	//(BUG: CraftBukkit doesn't report the X and Y values of the velocity, so player can only go straight down when spell is active.)
        	//FIXED/WORKED AROUND
        	Vector velocity = event.getTo().toVector().subtract(event.getFrom().toVector());
        	
            //Vector velocity = player.getVelocity();
            //double vx = velocity.getX();
            double vy = velocity.getY();
            //double vz = velocity.getZ();
            if (vy < -0.1D)
            {
                velocity.setY(-0.1D);
                player.setVelocity(velocity);
                player.setFallDistance(0);
            }
            
        }
        
        /*
        if(playerInfo.Haste)
        {
        	Vector velocity = event.getTo().toVector().subtract(event.getFrom().toVector());
        	
            double vx = velocity.getX() * 2.0d;
            double vz = velocity.getZ() * 2.0d;
            if (vx > 2.0d)
            	vx = 2.0d;
            else if (vx < -2.0d)
            	vx = -2.0d;
            if (vz > 2.0d)
            	vz = 2.0d;
            else if (vz < -2.0d)
            	vz = -2.0d;
            velocity.setX(vx);
            velocity.setZ(vz);
            player.setVelocity(velocity);
        }
        */
        
    }

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
    {
    	Player player = event.getPlayer();
    	ItemStack item = player.getInventory().getItemInMainHand();
    	Action action = event.getAction();
    	
    	if (plugin.config.getBoolean("Spellbook.Enabled", true) && item.getType() == Material.BOOK)
    	{
    		Spellbook spellbook = plugin.spellbookCollection.get(player);
    		if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR)
    		{
    			if (spellbook.StoredSpell.size() == 0)
    				return;
    			
    			// Check cooldown
    			Long cd = plugin.watcher.getCooldown(player, "Spellbook");
    			if (cd > 0)
    			{
    				player.sendMessage("Your spellbook needs to cool down for " + cd / 1000L + " more seconds");
    				player.sendMessage("before it can be used again.");
    			}
    			else
    			{
	    			String spell = spellbook.StoredSpell.get(spellbook.CurrentSpell);
	    			if (spell != "")
	    			{
	    				CastSpell(spell, player);
	    				plugin.watcher.addCooldown(player, "Spellbook", plugin.config.getInt("Spellbook.Cooldown", 5) * 1000L);
	    				player.updateInventory();
	    			}
    			}
    		}
    		else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
    		{
    			if (spellbook.StoredSpell.size() == 0)
    				return;
    			
    			if (++spellbook.CurrentSpell >= spellbook.StoredSpell.size())
    				spellbook.CurrentSpell = 0;
    			
    			player.sendMessage("You turn a page and ready the '" + spellbook.StoredSpell.get(spellbook.CurrentSpell) + "' spell.");
    		}
    		
    	}
    	
    }

	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
    	Player player = event.getPlayer();
    	plugin.watcher.addPlayer(player);
    	Spellbook spellbook = new Spellbook();
    	spellbook.StoredSpell = plugin.users.getStringList(player.getName() + ".Spellbook");
    	plugin.spellbookCollection.put(player, spellbook);
    }

	@EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
    	plugin.watcher.removePlayer(event.getPlayer());
    	plugin.spellbookCollection.remove(event.getPlayer());
    }

	@EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event)
    {
    	//DEBUG
    	plugin.log.info("[Praecantatio] DEBUG: Chat triggered");

    	Player player = event.getPlayer();
    	if (plugin.watcher.getTicks(player, "Silence") > 0)
    	{
    		player.getServer().broadcastMessage(player.getName() + " drools.");
    		event.setCancelled(true);
    	}
    	else
    	{
	    	String message = event.getMessage().trim();
	    	if (CastSpell(message, player))
	    		event.setCancelled(true);
    	}
	}
    
    private String CleanUp(String message)
    {
        String output = "";
        
        for (int i = message.length() - 1; i >= 0; i--)
        {
            if (message.charAt(i) != '!')
            {
                output = message.substring(0, i + 1);
                break;
            }
        }
        
        return output.toLowerCase();
    }
    
    private boolean CastSpell(String message, Player player)
    {
        // Check the strength & filter input
        int strength = 1;
        String superCmd = plugin.config.getString("General.SuperCommand", "magna");
        if (message.length() > superCmd.length() && message.substring(0, superCmd.length() + 1).toLowerCase().equals(superCmd + " "))
        {
        	strength = 3;
        	message = message.substring(6);
        }
        else if (message.toUpperCase() == message || message.charAt(message.length() - 1) == '!')
        {
            strength = 2;
        }
        
        message = CleanUp(message);
        
        String writeCmd = plugin.config.getString("Spellbook.InscriptionCommand", "scripto");
        
        // Process spell
        String nodeName = plugin.spellLookup.get(message);

        //DEBUG
		plugin.log.info("[Praecantatio] Node: " +nodeName);

        if (nodeName != null)
        {
        	//TODO: Permissions
//			if (plugin.usePermissions && !plugin.permissions.has(player, "praecantatio.spells." + nodeName.toLowerCase()))
//			{
//				player.sendMessage("You can't cast that spell.");
//			}
//			else
//			{
			// Announce the spell
			String announce;
			String announceLocal;
			switch (strength) {
				case 1:
					announce = ChatColor.YELLOW + player.getName() + ChatColor.DARK_AQUA + " raises his hands and mutters '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					announceLocal = ChatColor.DARK_AQUA + "You raise your hands and mutter '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					break;

				case 2:
					announce = ChatColor.YELLOW + player.getName() + ChatColor.DARK_AQUA + " raises his hands and yells '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					announceLocal = ChatColor.DARK_AQUA + "You raise your hands and yell '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					break;

				case 3:
				default:
					announce = ChatColor.YELLOW + player.getName() + ChatColor.DARK_AQUA + " raises his hands and forcefully exclaims '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					announceLocal = ChatColor.DARK_AQUA + "You raise your hands and forcefully exclaim '" + ChatColor.DARK_RED + message + ChatColor.DARK_AQUA + "'.";
					break;
			}
			int announceLevel = plugin.config.getInt("General.SpellAnnounceLevel", 1);
			if (announceLevel < 1)
				player.sendMessage(announceLocal);
			else if (announceLevel == 1)
			{
				double size = 10.0d + 15.0f * (strength - 1);
				List<Entity> entities = player.getNearbyEntities(size, size, size);
				for (Entity entity : entities)
				{
					if (entity instanceof Player)
						entity.sendMessage(announce);
				}
				player.sendMessage(announceLocal);

			}
			else
				player.getServer().broadcastMessage(announce);


			String nodePath = "Spells." + nodeName + ".";
			int cooldown = plugin.spells.getInt(nodePath + "Cooldown", 0);
			long currentCooldown = plugin.watcher.getCooldown(player, nodeName);
			if (cooldown > 0 && currentCooldown > 0)
			{
				player.sendMessage(message + " is on cooldown for " + currentCooldown / 1000L + " more seconds.");
			}
			else
			{
				if (plugin.spells.getBoolean(nodePath + "CastItemOnly", false) && !Util.isPlayerHolding(player, Material.getMaterial(plugin.spells.getString("General.CastItem", Material.BOOK.name()))))
				{
					player.sendMessage("You need to hold a book to cast that spell.");
				}
				else
				{
					boolean canCast = false;
					int costMultiplier = plugin.spells.getInt(nodePath + "CostMultiplier", 1);
					if (costMultiplier > 0)
					{
						String reagentType;
						int cost;
						if (plugin.spells.getBoolean(nodePath + "Master", false))
						{
							reagentType = "Master";
							cost = costMultiplier;
						}
						else
						{
							reagentType = "Regular";
							cost = plugin.config.getInt("Reagents.Level" + strength, 1 + 2 * (strength - 1)) * costMultiplier;
						}

						//boolean isDye = plugin.config.getBoolean("Reagents." + reagentType + "IsDye", false);
						Material material = Material.getMaterial(plugin.config.getString("Reagents." + reagentType, Material.LAPIS_LAZULI.name()));
						if (!Util.playerSpendItem(player, material, cost))
						{
							int lowCost = plugin.config.getInt("Reagents.Level1", 1 * costMultiplier);
							if (strength != 1 && Util.playerSpendItem(player, material, lowCost))
							{
								player.sendMessage("You need " + cost + " " + Util.getItemName(material) + " to cast the spell at full strength.");
								strength = 1;
								canCast = true;
							}
							else
								player.sendMessage("You need " + cost + " " + Util.getItemName(material) + " to cast that spell.");
						}
						else
							canCast = true;
					}
					else
						canCast = true;

					if (canCast)
					{
						if (nodeName.equals("Wall"))
							wall(player, strength);
						else if (nodeName.equals("GlassToIce"))
							glassToIce(player, strength);
						else if (nodeName.equals("Entomb"))
							entomb(player, strength);
						/*
						else if (nodeName.equals("Escape"))
							escape(player);
							*/
						else if (nodeName.equals("Replenish"))
							replenish(player, strength);
						else if (nodeName.equals("Bubble"))
							bubble(player, strength);
						else if (nodeName.equals("Lightning"))
							lightning(player, strength);
						else if (nodeName.equals("Light"))
							light(player, strength);
						else if (nodeName.equals("Heal"))
							heal(player, strength);
						else if (nodeName.equals("Blink"))
							blink(player);
						else if (nodeName.equals("Breathe"))
							breathe(player, strength);
						else if (nodeName.equals("Freeze"))
							freeze(player, strength);
						else if (nodeName.equals("Thaw"))
							thaw(player, strength);
						else if (nodeName.equals("Extinguish"))
							extinguish(player, strength);
						else if (nodeName.equals("Rain"))
							rain(player);
						else if (nodeName.equals("Storm"))
							storm(player);
						else if (nodeName.equals("Clear"))
							clear(player);
						else if (nodeName.equals("Mutate"))
							mutate(player, strength);
						else if (nodeName.equals("Activate"))
							activate(player, strength);
						else if (nodeName.equals("Fireball"))
							fireball(player, strength);
						/*
						else if (message.equals("pulsus":
							Pulsus(strength, ev);
						*/
						else if (nodeName.equals("SlowFall"))
							slowFall(player, strength);
						else if (nodeName.equals("Protect"))
							protect(player, strength);
						else if (nodeName.equals("WaterWalking"))
							waterWalking(player,strength);
						else if (nodeName.equals("Transmute"))
							transmute(player);
						else if (nodeName.equals("Launch"))
							launch(player, strength);
						else if (nodeName.equals("Break"))
							_break(player, strength);
						else if (nodeName.equals("Silence"))
							silence(player, strength);

						if (cooldown > 0)
							plugin.watcher.addCooldown(player, nodeName, cooldown * 1000L);
					}
				}
//		        }
		        return true;
			}
        }
        else if (plugin.config.getBoolean("Spellbook.Enabled", true)
        		&& message.length() > writeCmd.length() && message.substring(0, writeCmd.length() + 1).toLowerCase().equals(writeCmd + " "))
        {
        	//TODO: Permissions
//        	if (plugin.usePermissions && !plugin.permissions.has(player, "praecantatio.spellbook"))
//			{
//				player.sendMessage("You can't use a spellbook.");
//			}
//			else
//			{
				writeSpell(player, message.substring(8));
        		return true;
//			}
        }
        return false;
        
    }
    
    private void writeSpell(Player player, String spell)
    {
		//boolean isDye = plugin.config.getBoolean("Spellbook.WriteReagentIsDye", true);
		Material material = Material.getMaterial(plugin.config.getString("Spellbook.WriteReagent", Material.INK_SAC.name()));
		String nodeName = plugin.spellLookup.get(spell);
		if (nodeName == null)
		{
			player.sendMessage("You have no idea how to use the " + spell + " spell.");
		}
		else if (!Util.isPlayerHoldingBook(player))
    	{
    		player.sendMessage("You need to hold a book to inscribe a spell.");
    	}
		else if (plugin.spells.getBoolean("Spells." + nodeName + ".Master", false))
		{
			player.sendMessage("Cannot inscribe master spells.");
		}
		else if (!Util.playerSpendItem(player, material, 1))
		{
			player.sendMessage("You need 1 " + material.name() + " to inscribe a spell.");
		}		
    	else
    	{
    		if (spell.length() >= 6 && spell.substring(0, 6).equals("magna "))
	    		spell = spell.substring(6);
	    	
    		Spellbook spellbook = plugin.spellbookCollection.get(player);
    		if (spellbook.StoredSpell.contains(spell))
    		{
    			player.sendMessage("You tear a page out of your spellbook, removing the '" + spell + "' spell.");
    			player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.PAPER, 1));
    			
    			spellbook.StoredSpell.remove(spell);
    			plugin.users.set(player.getName() + ".Spellbook", spellbook.StoredSpell);
    			
    			if (spellbook.CurrentSpell >= spellbook.StoredSpell.size())
    				spellbook.CurrentSpell = 0;
    		}
    		else
    		{
    			spellbook.StoredSpell.add(spell);
    			plugin.users.set(player.getName() + ".Spellbook", spellbook.StoredSpell);
    			//item.setDurability((short)6);
    			player.sendMessage("You inscribe the spell '" + spell + "' in your spellbook.");
    		}
    	
		}
		
    }
    
    private void silence(Player player, int strength)
    {
    	// lol
    	long time = strength * 5000L;
    	
    	List<LivingEntity> targets = Util.getPlayerTarget(player, plugin.config.getInt("General.SpellRange", 50), true);
    	
    	if (targets.size() != 0)
    	{
    		Player target = (Player)targets.get(0);
    		
    		Long duration = plugin.watcher.getTicks(target, "SilenceImmunity");
        	if (duration > 0)
        	{
        		player.sendMessage(target.getName() + " is immune for " + duration / 1000L + " more seconds.");
        	}
    		else
    		{
	    		plugin.watcher.addTicker(target, "Silence", time, 4);
	    		plugin.watcher.addTicker(target, "SilenceImmunity", 60000L, 4);
	    		target.sendMessage("You have been silenced for " + time / 1000L + " seconds.");
	    		player.sendMessage("You silence " + target.getName() + " for " + time / 1000L + " seconds.");
	    		player.getWorld().playEffect(target.getLocation(), Effect.SMOKE, 1);
    		}
    	}

    	//TODO: setSilent?
    	
    }
    
    private void _break(Player player, int strength)
    {
    	int radius = 2;
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();
        World world = player.getWorld();
        
        for (int i = 0; i < strength; i++)
        {
        	int xorg = location.getBlockX();
            int yorg = location.getBlockY();
            int zorg = location.getBlockZ();
        	
	        for (int x = xorg - radius - 1; x <= xorg + radius + 1; x++)
	        {
	            for (int y = yorg - radius - 1; y <= yorg + radius + 1; y++)
	            {
	                for (int z = zorg - radius - 1; z <= zorg + radius + 1; z++)
	                {
	                	if (y < 1 || y > 128) continue;
	                	
	                    int dx = x - xorg;
	                    int dy = y - yorg;
	                    int dz = z - zorg;
	
	                    if ((int)Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius)
	                    {
	                        Block block = world.getBlockAt(x, y, z);
	                        Material type = block.getType();
	                        if (type != Material.WATER/* && type != Material.STATIONARY_WATER*/
	                    		&& type != Material.LAVA/* && type != Material.STATIONARY_LAVA*/
	                    		&& type != Material.BEDROCK && type != Material.CHEST && type != Material.OBSIDIAN
	                    		&& type != Material.NETHER_PORTAL && type != Material.END_PORTAL && type != Material.ICE && type != Material.FURNACE)
	                		{
	                        	plugin.watcher.addBlock(block, 5000L + 1000L * i);
	                        	block.setType(Material.AIR);
	                		}
	                        
	                    }
	                    
	                }
	            }
	        }
	        
	        if (strength > 1)
	        {
	        	Vector target = location.toVector();
	        	//Vector direction = location.toVector().subtract(target).normalize();
	        	Vector direction = player.getEyeLocation().getDirection().normalize();
	        	Vector newTarget = target.add(direction.multiply(5));
	        	location = newTarget.toLocation(world);
	        }
	        
        }
        
    }
    
    private void launch(Player player, int strength)
    {
    	List<LivingEntity> targets = Util.getPlayerTarget(player, 10.0D);
    	float force = 1.0f + (float)strength / 5.0f;
    	for (LivingEntity target : targets)
    	{
    		target.setVelocity(new Vector(0.0f, force, 0.0f));
    	}
    }
    
    private void glassToIce(Player player, int strength)
    {
    	World world = player.getWorld();
    	
        int radius = 5 * strength;
    	
    	ArrayList<Block> activeBlocks = new ArrayList<Block>();
    	
    	Block targetBlock = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50));
    	int xorg = targetBlock.getX();
        int yorg = targetBlock.getY();
        int zorg = targetBlock.getZ();
        
        if (targetBlock.getType() != Material.GLASS)
        	return;
        else
        	targetBlock.setType(Material.ICE);
        
        activeBlocks.add(targetBlock);
        
        while (activeBlocks.size() > 0)
        {
	    	for (int i = activeBlocks.size() - 1; i >= 0; i--)
	    	{
	    		//Block block = iter.next();
	    		Block block = activeBlocks.get(i);
	    		
	    		//TODO: Permanent magic ice?
	    		//block.setType(Material.ICE);
	    		activeBlocks.remove(block);
	    		
	    		int blockX = block.getX();
	    		int blockY = block.getY();
	    		int blockZ = block.getZ();
	    		
	    		for (int x = blockX - 1; x <= blockX + 1; x++)
	    		{
	    			for (int y = blockY - 1; y <= blockY + 1; y++)
	    			{
	    				for (int z = blockZ - 1; z <= blockZ + 1; z++)
	    				{
	    					Block relative = world.getBlockAt(x, y, z);
	    					if (relative.getType() == Material.GLASS)
	    					{
	    						int dx = blockX - xorg;
	    						int dy = blockY - yorg;
	    						int dz = blockZ - zorg;
	    						
	    						if ((int)Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius)
	    						{
	    							block.setType(Material.ICE);
	    							activeBlocks.add(relative);
	    						}
	    						
	    					}
	    				}
	    			}
	    		}
	    		
	    	}
	    	
        }
        
    }
    
    private void waterWalking(Player player, int strength)
    {
    	long time;
    	if (strength == 1)
        	time = 10000L;
        else if (strength == 2)
        	time = 20000L;
        else// if (strength == 3)
        	time = 30000L;
    	plugin.watcher.addTicker(player, "WaterWalking", time, 1);
    }
    
    private void slowFall(Player player, int strength)
    {
    	long time = strength * 10000L;
    	plugin.watcher.addTicker(player, "SlowFall", time, 4);
    	player.sendMessage("Your falling speed is decreased for " + time / 1000L + " seconds.");
    }
    
    private void heal(Player player, int strength)
    {
    	int frequency = 4;
    	if (strength == 2)
    		frequency = 2;
    	else if (strength == 3)
    		frequency = 1;
    	plugin.watcher.addTicker(player, "Heal", 20000L, frequency);
    }
    
    private void breathe(Player player, int strength)
    {
    	long time;
    	if (strength == 3)
        	time = 120000L;// + 30 * (strength - 1);
        else if (strength == 2)
        	time = 60000L;
        else
        	time = 30000L;
    	plugin.watcher.addTicker(player, "Breathe", time, 4);
    	player.sendMessage("You have infinite air for " + time / 1000L + " seconds.");
    }
    
    private void blink(Player player)
    {
    	 World world = player.getWorld();
    	 Location origin = player.getLocation();
         
         Location target = player.getLastTwoTargetBlocks(null, plugin.config.getInt("General.SpellRange", 50)).get(0).getLocation();
         world.playEffect(origin, Effect.SMOKE, 1);
         // Disable damage for a second
         player.setNoDamageTicks(20);
         target.setPitch(origin.getPitch());
         target.setYaw(origin.getYaw());
         player.teleport(target);
         world.playEffect(target, Effect.SMOKE, 1);
    }
    
    private void protect(Player player, int strength)
    {
         Long duration = 1000L + 2000L * strength;
         plugin.watcher.addTicker(player, "Protect", duration, 4);
         player.sendMessage("You are protected against all damage for " + duration / 1000L + " seconds.");
    }
    
    enum PlayerHoldingType
	{
		Invalid,
		Regular,
		Master
	}
    private void transmute(Player player)
    {
    	//TODO: Rework
		//TODO: Coal stack <-> Diamond transmutation
    	PlayerInventory inventory = player.getInventory();
		ItemStack item = inventory.getItemInMainHand();
		int amount = item.getAmount();
    	
    	// Gather info about reagents
    	//boolean regularIsDye = plugin.config.getBoolean("Reagents.RegularIsDye", false);
    	Material regular = Material.getMaterial(plugin.config.getString("Reagents.Regular", Material.REDSTONE.name()));
    	//boolean masterIsDye = plugin.config.getBoolean("Reagents.MasterIsDye", true);
		Material master = Material.getMaterial(plugin.config.getString("Reagents.Master", Material.LAPIS_LAZULI.name()));
    	
    	// Check if what the player is holding is a reagent
    	PlayerHoldingType holding = PlayerHoldingType.Invalid;
//    	if (item.getType() == Material.INK_SAC)
//    	{
//    		if (regularIsDye && item.getDurability() == regular)
//    			holding = PlayerHoldingType.Regular;
//    		else if (masterIsDye && item.getDurability() == master)
//    			holding = PlayerHoldingType.Master;
//    	}
		/*else */
		if (item.getType() == regular)
			holding = PlayerHoldingType.Regular;
		else if (item.getType() == master)
			holding = PlayerHoldingType.Master;
    	
    	int cost = plugin.config.getInt("Reagents.TransmuteCost", 15);
    	

		// Spend regular reagent
		if (amount < cost)
		{
			player.sendMessage("Not enough " + Util.getItemName(regular) + ", you need " + cost + ".");
			return;
		}
		else if (amount == cost)
		{
			inventory.clear(inventory.getHeldItemSlot());
		}
		else if (amount > cost) //TODO: IDE warning in error?
		{
			item.setAmount(amount - cost);
		}

		// Add master reagent
//    		if (masterIsDye)
//    		{
//    			Dye reagent = new Dye();
//    			reagent.setData((byte)master);
//    			inventory.addItem(reagent.toItemStack(1));
//    		}
//    		else
//    		{


//    		}
		//TODO: Cleanup
		if (holding == PlayerHoldingType.Regular)
		{
			ItemStack reagent = new ItemStack(master, 1);
			inventory.addItem(reagent);
    	}
    	else if (holding == PlayerHoldingType.Master)
    	{
//    		// Spend master reagent
//    		if (amount == 1)
//            {
//                inventory.clear(inventory.getHeldItemSlot());
//            }
//    		else if (amount > 1)
//            {
//            	item.setAmount(amount - 1);
//            }
//
//    		// Add regular reagents
//    		if (regularIsDye)
//    		{
//    			Dye reagent = new Dye();
//    			reagent.setData((byte)regular);
//    			inventory.addItem(reagent.toItemStack(cost));
//    		}
//    		else
//    		{
//    			ItemStack reagent = new ItemStack(regular, cost);
//    			inventory.addItem(reagent);
//    		}

			ItemStack reagent = new ItemStack(regular, 1);
			inventory.addItem(reagent);
    	}
    	else
    	{
    		player.sendMessage("Cannot transform that material.");
    	}
    	
    }
    
    private void entomb(Player player, int strength)
    {
    	long time = strength * 10000L;
    	
    	// Find target
    	Block targetBlock = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50));
    	Location targetBlockLocation = targetBlock.getLocation();
    	World world = player.getWorld();

        List<Player> players = world.getPlayers();
        
        for (Player target : players)
        {
            Location targetLocation = target.getLocation();
            if (target != player
              && targetBlockLocation.distance(targetLocation) <= 5.0D)
            {
            	Long duration = plugin.watcher.getTicks(target, "EntombImmunity");
            	if (duration > 0)
            	{
            		player.sendMessage(target.getName() + " is immune for " + duration / 1000L + " more seconds.");
            	}
            	else
            	{
	            	player.setNoDamageTicks(20);
	            	
	            	// Give target entomb immmunity for 1min
	            	plugin.watcher.addTicker(target, "EntombImmunity", 60000L, 4);
	    		    
	    		    int tx = targetLocation.getBlockX();
	    		    int ty = targetLocation.getBlockY();
	    		    int tz = targetLocation.getBlockZ();
	    		    
	    		    // Obsidian tomb
	    		    Block[] tomb =
			    	{
			    		// Bottom layer
			    		world.getBlockAt(tx - 1, ty - 1, tz - 1), world.getBlockAt(tx, ty - 1, tz - 1), world.getBlockAt(tx + 1, ty - 1, tz - 1),
			    		world.getBlockAt(tx - 1, ty - 1, tz), world.getBlockAt(tx, ty - 1, tz), world.getBlockAt(tx + 1, ty - 1, tz),
			    		world.getBlockAt(tx - 1, ty - 1, tz + 1), world.getBlockAt(tx, ty - 1, tz + 1), world.getBlockAt(tx + 1, ty - 1, tz + 1),
			    		
			    		// Feet layer
			    		world.getBlockAt(tx - 1, ty, tz - 1), world.getBlockAt(tx, ty, tz - 1), world.getBlockAt(tx + 1, ty, tz - 1),
			    		world.getBlockAt(tx - 1, ty, tz), world.getBlockAt(tx + 1, ty, tz),
			    		world.getBlockAt(tx - 1, ty, tz + 1), world.getBlockAt(tx, ty, tz + 1), world.getBlockAt(tx + 1, ty, tz + 1),
			    		
			    		// Eye-level layer
			    		world.getBlockAt(tx - 1, ty + 1, tz - 1), world.getBlockAt(tx, ty + 1, tz - 1), world.getBlockAt(tx + 1, ty + 1, tz - 1),
			    		world.getBlockAt(tx - 1, ty + 1, tz), world.getBlockAt(tx + 1, ty + 1, tz),
			    		world.getBlockAt(tx - 1, ty + 1, tz + 1), world.getBlockAt(tx, ty + 1, tz + 1), world.getBlockAt(tx + 1, ty + 1, tz + 1),
			    		
			    		// Top layer
			    		world.getBlockAt(tx - 1, ty + 2, tz - 1), world.getBlockAt(tx, ty + 2, tz - 1), world.getBlockAt(tx + 1, ty + 2, tz - 1),
			    		world.getBlockAt(tx - 1, ty + 2, tz), world.getBlockAt(tx, ty + 2, tz), world.getBlockAt(tx + 1, ty + 2, tz),
			    		world.getBlockAt(tx - 1, ty + 2, tz + 1), world.getBlockAt(tx, ty + 2, tz + 1), world.getBlockAt(tx + 1, ty + 2, tz + 1)
			    	};
	    		    
	    		    for (int i = 0; i < tomb.length; i++)
	    		    {
	    		    	Material type = tomb[i].getType();
	    		    	if (type != Material.BEDROCK)
			        	{
			        		plugin.watcher.addBlock(tomb[i], time - (long)(Math.random() * 2000.0d));
			        		
			        		tomb[i].setType(Material.DIRT);
			        		tomb[i].setType(Material.OBSIDIAN);
			        	}
	    		    }
	    		    
	    		    // Signs
	    		    Block[] sign =
			    	{
						world.getBlockAt(tx, ty + 1, tz - 2),
						world.getBlockAt(tx, ty + 1, tz + 2),
			    		world.getBlockAt(tx - 2, ty + 1, tz),
			    		world.getBlockAt(tx + 2, ty + 1, tz)
			    	};
	    		    
	    		    for (int i = 0; i < sign.length; i++)
	    		    {
		    		    if (sign[i].getType() == Material.AIR)
		    		    {
		    		    	plugin.watcher.addBlock(sign[i], time - 2000L);
			        		
			        		sign[i].setType(Material.DARK_OAK_WALL_SIGN);

							Directional directional = (Directional)sign[i].getBlockData();
							//TODO: Optimize/move to util class
							switch (i) {
								case 0:
									directional.setFacing(BlockFace.SOUTH);
									break;
								case 1:
									directional.setFacing(BlockFace.WEST);
									break;
								case 2:
									directional.setFacing(BlockFace.NORTH);
									break;
								case 3:
									directional.setFacing(BlockFace.EAST);
									break;
							}
							sign[i].setBlockData(directional);
			        		//sign[i].setData((byte)(2 + i));

			        		Sign signBlock = (Sign)sign[i].getState();
			        		signBlock.setLine(0, "Here lies");
			        		signBlock.setLine(1, target.getName());
			        		signBlock.setLine(2, "R.I.P");
			        		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
			        		signBlock.setLine(3, date.format(new Date()));
		    		    }
	    		    }
	    		    
	    		    target.sendMessage("You have been buried alive.");
	    		    
	    		    //TODO: Center targets position to avoid damage?
	    		    //NOTE: This doesn't work:
	    		    //target.teleport(targetLocation);
	    		    
	    		    break;
            	}
            }
        }
        
    }
    /*
    private void escape(Player player)
    {
    	
    }
    */
    private void fireball(Player player, int strength)
    {
    	Location location = player.getLocation();
        Location target = location.add(location.getDirection().normalize().multiply(2).toLocation(player.getWorld(), location.getYaw(), location.getPitch())).add(0.0D, 1.0D, 0.0D);
        
    	switch (strength)
    	{
    		case 1:
    			player.launchProjectile(Egg.class);
    			player.sendMessage("You fumble and throw an egg.");
    			break;
    		case 2:
				player.launchProjectile(Snowball.class);
    			player.sendMessage("You strain yourself, but you only manage to throw a snowball.");
    			break;
    		case 3:
    			player.getWorld().spawn(target, Fireball.class);
    			break;
    			//TODO: Blaze/dragon fireballs?
    	}
    	
    }
    
    private void mutate(Player player, int strength)
    {
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();
        World world = player.getWorld();
        
        List<Entity> entities = world.getEntities();
        
        // Used to increase chances of bad stuff happening when lots of mobs are nearby
        double badLuck = 0.0d;
        
        for (Entity entity : entities)
        {
            Location oloc = entity.getLocation();
            if ((location.distance(oloc) <= 10.0D)
              && ((entity instanceof LivingEntity) && !(entity instanceof HumanEntity) && !(entity instanceof Giant)))
            {
                Location eloc = entity.getLocation();
                
                // Don't morph owned wolves
                if (entity instanceof Wolf && ((Wolf)entity).isTamed())
                	return;

                //TODO: Guardian? Elder guardian? Dragon??? Shulkers?
				//TODO: Evoker, Witch (people trans only?  Illusioner? Iron golem? Pillager? Ravager? Vex? Vindicator?  EntityType.ZOMBIE_VILLAGER?
				//TODO: Cubs? Puppies?? Kittens??? Babby cows?
				//TODO: Creature variation?

				//TODO: getAllowAnimals & getAllowMonsters

				EntityType[] creatures = {
						EntityType.CHICKEN, EntityType.COW, EntityType.SHEEP, EntityType.SQUID, EntityType.WOLF, EntityType.SLIME, EntityType.BAT,
						EntityType.CAT, EntityType.COD, EntityType.DOLPHIN, EntityType.DONKEY, EntityType.FOX, EntityType.HORSE,  EntityType.PIG,
					 	EntityType.LLAMA, EntityType.MULE, EntityType.OCELOT, EntityType.PARROT, EntityType.PANDA, EntityType.POLAR_BEAR,
						EntityType.PUFFERFISH, EntityType.SALMON, EntityType.SNOWMAN, EntityType.TURTLE, EntityType.TROPICAL_FISH, EntityType.TRADER_LLAMA

				};
				EntityType[] monsters = {
						EntityType.CREEPER, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.PIG_ZOMBIE, EntityType.ZOMBIE, EntityType.WOLF,
						EntityType.ENDERMAN, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.SKELETON_HORSE, EntityType.WITHER_SKELETON,
						EntityType.SILVERFISH, EntityType.STRAY, EntityType.ENDERMITE, EntityType.ZOMBIE_HORSE

				};
                
                EntityType creature;
                double chance = Math.random();
                boolean passive = false;
                if (chance < 0.01d) // 1%
                {
                	// You done goofed up...
	                creature = EntityType.GIANT;
	                player.sendMessage("You completely botch the spell!");
                }
                else if (chance < 0.02d) // 1%
                {
                	// ...but this might be even worse.
                	creature = EntityType.GHAST;
					//TODO: EntityType.BLAZE, guardians? Shulkers?
					player.sendMessage("You completely botch the spell and summon a creature from the nether!");
                }
                else if (chance < (0.22d + badLuck) / strength) // 20%
                {
                	// Backfire
                	int i = (int)(Math.random() * monsters.length);
	                creature = monsters[i];
	                player.sendMessage("The spell backfires!");
                }
                else // 78%
                {
	                int i = (int)(Math.random() * creatures.length);
	                creature = creatures[i];
	                passive = true;
                }
                
                // Replace entity
                double oldHealth = ((LivingEntity)entity).getHealth();
                entity.remove();
                world.playEffect(eloc, Effect.SMOKE, 1);
                LivingEntity spawned = (LivingEntity)world.spawnEntity(eloc, creature);
                spawned.setHealth(oldHealth);
                
                // Bad wolves
                if (spawned instanceof Wolf && !passive)
                	((Wolf)spawned).setAngry(true);
                // And good slimes
                else if (spawned instanceof Slime)
                {
                	if (passive)
                		((Slime)spawned).setSize(1);
                	else
                		((Slime)spawned).setSize(5);
                }
                // And make sure all other monsters are always angry
                else if (spawned instanceof PigZombie)
                {
                	((PigZombie)spawned).setTarget(player);
                }
                else if (spawned instanceof Spider)
                {
                	((Spider)spawned).setTarget(player);
                }
                
                // Increase badLuck in case another entity was nearby enough
                badLuck += 0.20d;
                
            }
        }
    }
  /*
	private void Domus(String[] components, PlayerChatEvent ev)
	{
		if (PlayerSpendReagent(ev.getPlayer(), Material.BOOK, 1))
			ev.getPlayer().teleport(ev.getPlayer().getWorld().getSpawnLocation());
	}
   */
    
    private void lightning(Player player, int strength)
    {
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();
        int xorg = location.getBlockX(); int yorg = location.getBlockY(); int zorg = location.getBlockZ();
        World world = player.getWorld();
        world.strikeLightning(location);
        
        Block block;
        
        if (strength == 1)
        {
        	block = world.getBlockAt(location);
	    	if (block.getType() == Material.SAND)
	    		block.setType(Material.GLASS);
        }
        if (strength == 2)
        {
        	block = world.getBlockAt(xorg, yorg + 1, zorg);
	    	//if (block.getType() == Material.AIR)
    		block.setType(Material.FIRE);
        }
        if (strength > 1)
        {
	        for (int x = xorg - 1; x <= xorg + 1; x++)
	        {
	            for (int z = zorg - 1; z <= zorg + 1; z++)
	            {
	            	// Sand -> glass
	            	block = world.getBlockAt(x, yorg, z);
			    	if (block.getType() == Material.SAND && (Math.random() <= 0.40D))
			    		block.setType(Material.GLASS);
			    	else if (strength > 2)
			    	{
			    		// If the spell is strong enough, turn the block above into fire.
			    		// This won't work for glass blocks, so we use the other ones.
			    		block = world.getBlockAt(x, yorg + 1, z);
				    	if (block.getType() == Material.AIR)
				    		block.setType(Material.FIRE);
			    	}
	            }   
	        }
        }
        
    }

    private void clear(Player player)
    {
        player.getWorld().setStorm(false);
        player.getWorld().setThundering(false);
    }

    private void storm(Player player)
    {
        player.getWorld().setStorm(true);
        player.getWorld().setThundering(true);
    }
    
    private void activate(Player player, int strength)
    {
    	World world = player.getWorld();
    	Block target = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50));
    	int tx = target.getX();
    	int ty = target.getY();
    	int tz = target.getZ();
    	
    	int radius = 5 * strength;
    	
    	// Simple cubic lookup
    	for (int x = tx - radius; x <= tx + radius; x++)
    	{
    		for (int y = ty - radius; y <= ty + radius; y++)
    		{
    			for (int z = tz - radius; z <= tz + radius; z++)
    			{
    				Block current = world.getBlockAt(x, y, z);
    				if (current.getType() == Material.LEGACY_REDSTONE_TORCH_ON)
    				{
    					plugin.watcher.addBlock(current, 5000L);
    					current.setType(Material.AIR);
    				}
    				
    			}
    		}
    	}
    		
    	
    }
    
    private void rain(Player player)
    {
        player.getWorld().setStorm(true);
        player.getWorld().setThundering(false);
    }
    
    private void extinguish(Player player, int strength)
    {
        player.setFireTicks(0);
        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 1);
        
        if (strength > 1)
        {
        	World world = player.getWorld();
        	
        	int radius = 7 * strength;
            Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();

            int xorg = location.getBlockX();
            int yorg = location.getBlockY();
            int zorg = location.getBlockZ();

            for (int x = xorg - radius - 1; x <= xorg + radius + 1; x++)
            {
                for (int y = yorg - radius - 1; y <= yorg + radius + 1; y++)
                {
                	for (int z = zorg - radius - 1; z < zorg + radius + 1; z++)
                	{
	                    if (y < 1)
	                    	y = 1;
	                    else if (y > 120)
	                    {
	                    	y = 120;
	                    }
	                	
	                    int dx = x - xorg;
	                    int dy = y - yorg;
	                    int dz = z - zorg;
	                	
	                    if ((int)Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius)
	                    {
	                    	Block block = world.getBlockAt(x, y, z);
	                        Material type = block.getType();
	                        if (type == Material.FIRE)
	                        {
	                        	block.setType(Material.DIRT);
	                        	block.setType(Material.AIR);
	                        }
	                    }
                	}
                }
            }    
        }
        
    }

    private void replenish(Player player, int strength)
    {
    	if (!plugin.usePermissions && !player.isOp())
    		return;
    	
        int radius = 7 * strength;
        
        //HashSet transparent = null;
        Block block = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50));

        Location origin = block.getLocation();

        int xorg = (int)origin.getX();
        int yorg = (int)origin.getY();
        int zorg = (int)origin.getZ();

        if (Math.abs(yorg - 63) < radius)
        {
            int y = 63;
            
            for (int x = xorg - radius - 1; x <= xorg + radius + 1; x++)
            {
                for (int z = zorg - radius - 1; z <= zorg + radius + 1; z++)
                {
                    int dx = x - xorg;
                    int dz = z - zorg;

                    if ((int)Math.sqrt(dx * dx + dz * dz) <= radius)
                    {
                        Block targetblock = player.getWorld().getBlockAt(x, y, z);
                        Material type = targetblock.getType();
                        if ((type == Material.AIR) || (type == Material.WATER))
                        {
                        	//TODO: Alternative instead of setting to dirt
                            targetblock.setType(Material.DIRT);
                            targetblock.setType(Material.WATER);
                        }
                    }
                }
            }
        }
    }
    
    private void thaw(Player player, int strength)
    {
        int radius = 5 * strength;
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();

        int xorg = location.getBlockX();
        int yorg = location.getBlockY();
        int zorg = location.getBlockZ();

        for (int x = xorg - radius - 1; x <= xorg + radius + 1; x++)
        {
            for (int y = yorg - radius - 1; y <= yorg + radius + 1; y++)
            {
                for (int z = zorg - radius - 1; z <= zorg + radius + 1; z++)
                {
                    if (y < 1)
                        y = 1;
                    else if (y > 120)
                        y = 120;
                    
                    int dx = x - xorg;
                    int dy = y - yorg;
                    int dz = z - zorg;

                    if ((int)Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius)
                    {
                        Block block = player.getWorld().getBlockAt(x, y, z);
                        Material type = block.getType();
                        
                        if (type == Material.ICE)
                        {
                        	// Fix for permanent water
                        	if (!plugin.watcher.removeBlock(block))
                        		block.setType(Material.WATER);
                        }
                        else if (type == Material.SNOW)
                            block.setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    private void freeze(Player player, int strength)
    {
        int radius = 5 * strength;
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();

        int xorg = location.getBlockX();
        int yorg = location.getBlockY();
        int zorg = location.getBlockZ();

        for (int x = xorg - radius - 1; x <= xorg + radius + 1; x++)
        {
            for (int y = yorg - radius - 1; y <= yorg + radius + 1; y++)
            {
                for (int z = zorg - radius - 1; z <= zorg + radius + 1; z++)
                {
                	if (y < 1 || y > 128) continue;
                	/*
                    if (y < 1)
                        y = 1;
                    else if (y > 128)
                        y = 128;
            	 	*/
                    int dx = x - xorg;
                    int dy = y - yorg;
                    int dz = z - zorg;

                    if ((int)Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius)
                    {
                        Block block = player.getWorld().getBlockAt(x, y, z);
                        Block bottomblock = player.getWorld().getBlockAt(x, y - 1, z);
                        Material type = block.getType();
                        byte data = block.getData();
                        Material bottomtype = bottomblock.getType();
                        if ((type == Material.WATER)
                        	&& (data & 0x7) == 0x0 && (data & 0x8) != 0x8)
                          block.setType(Material.ICE);
                        else if ((type == Material.AIR) && (bottomtype != Material.AIR) && (bottomtype != Material.SNOW)) //TODO: Check for plants?
                          block.setType(Material.SNOW);
                        else if (type == Material.LAVA)
                		{
                        	// If it's not falling, stationary
                        	if ((data & 0x7) == 0x0 && (data & 0x8) != 0x8)
                        		block.setType(Material.OBSIDIAN);
                    		else
                    			block.setType(Material.COBBLESTONE);
                		}
                        
                    }
                    
                }
            }
        }
    }
    
    private void light(Player player, int strength)
    {
    	long time;
    	if (strength == 1)
    		time = 60000L;
    	else if (strength == 2)
    		time = 120000L;
    	else //if (strength == 3)
    		time = 300000L;
    	
        Block block = player.getLastTwoTargetBlocks(null, plugin.config.getInt("General.SpellRange", 50)).get(0);
        //TODO: Expand based on strength, or replace with portable light
        // Should always be air?
        Material type = block.getType();
        if (type != Material.BEDROCK)
        {
        	plugin.watcher.addBlock(block, time);
            block.setType(Material.GLOWSTONE);
        }
    }
    
    private void wall(Player player, int strength)
    {
        int length = 5 + 5 * strength;

        Vector vector = player.getEyeLocation().getDirection().normalize();
        Location location = player.getTargetBlock(null, plugin.config.getInt("General.SpellRange", 50)).getLocation();

        int xorg = location.getBlockX();
        int yorg = location.getBlockY();
        int zorg = location.getBlockZ();

        double vx = vector.getX();
        double vz = vector.getZ();

        double ox = -vz;
        double oz = vx;
        
        for (int i = -length / 2; i <= length / 2 + 1; i++)
        {
            location.setX(xorg + i * ox);
            location.setZ(zorg + i * oz);
            for (int y = yorg; y <= yorg + 5; y++)
            {
                if (y < 1 || y > 128) continue;
                location.setY(y);
                Block block = location.getBlock();
                Material type = block.getType();
                if (type == Material.AIR || type == Material.SNOW || type == Material.WATER)
                {
                	plugin.watcher.addBlock(block, 15000L - (y - yorg + 1) * 500L);
                    block.setType(Material.COBWEB);
            	}
            }
        }
    }
    
    private void bubble(Player player, int strength)
    {
    	World world = player.getWorld();
    	
    	Location location = player.getLocation();
    	int playerX = location.getBlockX();
    	int playerY = location.getBlockY();
    	int playerZ = location.getBlockZ();
    	
    	int radius = 4; 
    	int radiusSq = radius * radius;
    	
        float size = (float)(2.0f * Math.ceil(radius)) + 1.0f;
        int halfSize = (int)size / 2;
        double offset = Math.floor(size / 2.0f);
        
        //for (int z = 1; z < size - 1; z++) ??? forgot why I did this, might be XNA specific?
        for (int z = 0; z < size; z++)
        {
        	int actualZ = playerZ + z - halfSize;
        	
        	for (int y = 0; y < size; y++)
            {
        		int actualY = playerY + y - halfSize;
        		if (actualY < 1 || actualY > 128) continue;
        		
        		for (int x = 0; x < size; x++)
                {
        			int actualX = playerX + x - halfSize;
    				/*
        			// Save air blocks, this *should* fix the concalesco bug
        			if (type == Material.AIR)
					{
						IncantatioBlockInfo blockInfo = new IncantatioBlockInfo();
                        blockInfo.time = new Date().getTime() + 10000L * strength;
                        blockInfo.original = type;
                        Incantatio.magicBlocks.put(block, blockInfo);
                        this.cleanupThread.main();
					}
        			*/
        			if ((Math.pow(x - offset, 2.0D) + Math.pow(y - offset, 2.0D) + Math.pow(z - offset, 2.0D) < radiusSq))	
                    {
        				Block block = world.getBlockAt(actualX, actualY, actualZ);
        				Material type = block.getType();
        				
        				if (IsFull(x - 1, y, z, offset, radiusSq) && IsFull(x + 1, y, z, offset, radiusSq) && IsFull(x, y - 1, z, offset, radiusSq)
						  && IsFull(x, y + 1, z, offset, radiusSq) && IsFull(x, y, z - 1, offset, radiusSq) && IsFull(x, y, z + 1, offset, radiusSq))
        				{
        					// When underwater, make a temporary bubble
        					if (type == Material.WATER || type == Material.LAVA)
        					{
        						plugin.watcher.addBlock(block, 10000L * strength - (y + 1) * 500L);
	                            block.setType(Material.AIR);
        					}
        				}
        				else
        				{
	                        if (type == Material.AIR || type == Material.SNOW || type == Material.WATER)
	                        {
	                        	plugin.watcher.addBlock(block, 10000L * strength - (y + 1) * 500L);
	                            block.setType(Material.ICE);
	                    	}
        				}
                    }	
                }
            }
        }
        
    }
    
    private boolean IsFull(int x, int y, int z, double offset, double radiusSq)
    {
		x -= offset;
		y -= offset;
		z -= offset;
		x *= x;
		y *= y;
		z *= z;
		return x + y + z < radiusSq;
	}

	//TODO: Anvil spell

	//TODO: Poison spell

	//TODO: Bouncy floor spell?

}