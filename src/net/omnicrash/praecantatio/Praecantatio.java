package net.omnicrash.praecantatio;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

//import com.nijiko.permissions.PermissionHandler;
//import com.nijikokun.bukkit.Permissions.Permissions;
import org.bukkit.plugin.Plugin;

public class Praecantatio extends JavaPlugin
{
	private final String FILE_SPELLS = "spells.yml";
	private final String FILE_USERS = "users.yml";

    public final Logger log = Logger.getLogger("Minecraft");
    public final Watcher watcher = new Watcher(this);
    
    public FileConfiguration config;
    public FileConfiguration spells;
    public FileConfiguration users;
    
    public final Hashtable<Player, Spellbook> spellbookCollection = new Hashtable<Player, Spellbook>();
    public final Hashtable<String, String> spellLookup = new Hashtable<String, String>();
    
    private final PraecantatioPlayerListener _playerListener;
    private final PraecantatioBlockListener _blockListener;
    private final PraecantatioEntityListener _entityListener;
    
    public PermissionHandler permissions;
    public boolean usePermissions = false;
    
    /*
    public enum spellNames
    {
		Bubble("sphaera"), Lightning("fulmen"), SlowFall("tarduscado"), Heal("remedium"), Blink("transulto"),
		Breathe("respiro"), Freeze("frigidus"), Thaw("concalesco"), Extinguish("extinguo"), Rain("pluvia"),
		Storm("tempestas"), Clear("sereno"), Mutate("mutatio"), Wall("clausus"), Replenish("repleo"),
		Protect("tueri"), Fireball("ignifera"), Write("scripto"), Transmute("transmutare"), Entomb("sepulcrum"),
		GlassToIce("glacia"), Light("lux"), WaterWalking("superaquas");
		private spellNames(String name)
		{
			this.name = name;
		}
		public String name;
    }
    */

    public Praecantatio()
	{
		_playerListener = new PraecantatioPlayerListener(this);
		_blockListener = new PraecantatioBlockListener(this);
		_entityListener = new PraecantatioEntityListener(this);

	}

    @Override
    public void onEnable()
    {
    	// Read or create config file
    	parseConfigs();
    	
    	// Hook events
        //PluginManager pm = getServer().getPluginManager();
//        pm.registerEvent(new AsyncPlayerChatEvent(), this.playerListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.PLAYER_MOVE, this.playerListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.PLAYER_JOIN, this.playerListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.PLAYER_QUIT, this.playerListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.BLOCK_BREAK, this.blockListener, EventPriority.NORMAL, this);
//        pm.registerEvent(Event.Type.ENTITY_DAMAGE, this.entityListener, EventPriority.NORMAL, this);
        
        // Start watcher thread
        //watcher.start();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, watcher, 20, 5);

        setupPermissions();
        
        log.info("[Praecantatio] v" + getDescription().getVersion() + " active.");
    }
    
    @Override
    public void onDisable()
    {
    	//watcher.stop();
    	watcher.cleanUp();
    	//users.save();
    	
        log.info("[Praecantatio] v" + getDescription().getVersion() + " disabled.");
    }
    
    private void setupPermissions()
    {
        if (permissions != null)
            return;
        
        Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
        
        if (permissionsPlugin == null)
        {
            log.info("[Praecantatio] Permission system not detected, defaulting to OP");
            return;
        }
        
        permissions = ((Permissions) permissionsPlugin).getHandler();
        usePermissions = true;
        log.info("[Praecantatio] Using " + ((Permissions)permissionsPlugin).getDescription().getFullName());
    }
    
    private void parseConfigs()
    {
		// Make sure the data folder exists
		getDataFolder().mkdir();

    	parseConfigGeneral();
    	parseConfigSpells();
    	parseConfigUsers();
    	
    }

    private void parseConfigGeneral()
	{
		// Apply defaults (if not found)
		try
		{
			this.saveDefaultConfig();
		} catch (Exception e) {
			e.printStackTrace();
			log.info("[Praecantatio] Error creating config file: " + e.getMessage());
		}

		this.getConfig().options().copyDefaults(true);

		config = this.getConfig();

	}

	private void parseConfigSpells()
	{
		// Read or create spell file
		File spellFile = new File(getDataFolder().getPath(), FILE_SPELLS);
		try
		{
			if (!spellFile.exists()) {
				//TODO: Data folder?
				this.saveResource(FILE_SPELLS, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("[Praecantatio] Error creating spell file: " + e.getMessage());
		}

		spells = YamlConfiguration.loadConfiguration(spellFile);

		// Parse spell nodes for easy lookup
		Map<String, Object> spellNodes = spells.getConfigurationSection("Spells").getValues(false);
		for (Map.Entry<String, Object> entry : spellNodes.entrySet())
		{
			spellLookup.put(entry.toString()/*.getValue().getString("Name")*/, entry.getKey());
		}

	}

	private void parseConfigUsers()
	{
		// Read or create users file
		File usersFile = new File(getDataFolder().getPath(), FILE_USERS);
		try
		{
			//isNew = spellFile.createNewFile();
			if (!usersFile.exists()) {
				//TODO: Data folder?
				this.saveResource(FILE_USERS, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//TODO: Fetch log name from plugin.yml?
			log.info("[Praecantatio] Error creating users file: " + e.getMessage());
		}

		users = YamlConfiguration.loadConfiguration(usersFile);

	}
    
}