package net.omnicrash.praecantatio;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.entity.Player;

public class Praecantatio extends JavaPlugin
{
    private final static int QUARTER_SECOND = 5;
	private final static int HALF_SECOND = QUARTER_SECOND*2;
	private final static int SECOND = HALF_SECOND*2;

	private final String FILE_SPELLS = "spells.yml";
	private final String FILE_USERS = "users.yml";

    public final Logger log = Logger.getLogger("Minecraft");
    public final Watcher watcher = new Watcher(this);
    
    public FileConfiguration config;
    public FileConfiguration spells;
    public FileConfiguration users;
    
    public final Hashtable<Player, Spellbook> spellbookCollection = new Hashtable<Player, Spellbook>();
    public final Hashtable<String, String> spellLookup = new Hashtable<String, String>();
    
    private PraecantatioPlayerListener _playerListener;
    private PraecantatioBlockListener _blockListener;
    private PraecantatioEntityListener _entityListener;
    
//    public PermissionAttachment permissions;
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

	}

    @Override
    public void onEnable()
    {
    	// Read or create config file
    	parseConfigs();

    	// Create event listeners
        _playerListener = new PraecantatioPlayerListener(this);
        _blockListener = new PraecantatioBlockListener(this);
        _entityListener = new PraecantatioEntityListener(this);

        // Start watcher thread
        getServer().getScheduler().scheduleSyncRepeatingTask(this, watcher, QUARTER_SECOND, QUARTER_SECOND);

        //setupPermissions();
		usePermissions = config.getBoolean("General.PermissionsEnabled");

        log.info(this.getDescription().getFullName() + " enabled");
    }
    
    @Override
    public void onDisable()
    {
    	//watcher.stop();
    	watcher.cleanUp();
    	//users.save();

        log.info(this.getDescription().getFullName() + " disabled");
    }
    
//    private void setupPermissions()
//    {
////        if (permissions != null)
////            return;
//
//        //Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
//
////        if (permissionsPlugin == null)
////        {
////            log.info("[Praecantatio] Permission system not detected, defaulting to OP");
////            return;
////        }
//
////        permissions = ((Permissions) permissionsPlugin).getHandler();
//        usePermissions = true;
////        log.info("[Praecantatio] Using " + ((Permissions)permissionsPlugin).getDescription().getFullName());
//		log.info("[Praecantatio] Permissions enabled");
//    }
    
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

		config = this.getConfig();

		config.options().copyDefaults(true);
		this.saveConfig();

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

		spells.options().copyDefaults(true);
		try {
			spells.save(spellFile);
		} catch (IOException e) {
			e.printStackTrace();
			log.info("[Praecantatio] Error saving spell file defaults: " + e.getMessage());
		}

		// Parse spell nodes for easy lookup
		Set<String> spellNodes = spells.getConfigurationSection("Spells").getKeys(false);
		for (String key : spellNodes)
		{
			String spellPath = spells.getString("Spells." + key + ".Name");
			spellLookup.put(spellPath, key);
			log.info("[Praecantatio] Registered spell: " + spellPath + " as " + key);
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
				usersFile.createNewFile();
				//this.saveResource(FILE_USERS, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//TODO: Fetch log name from plugin.yml?
			log.info("[Praecantatio] Error creating users file: " + e.getMessage());
		}

		users = YamlConfiguration.loadConfiguration(usersFile);
		try {
			users.save(usersFile);
		} catch (IOException e) {
			e.printStackTrace();
			log.info("[Praecantatio] Error saving users file defaults: " + e.getMessage());
		}

	}

	public void saveUsers()
	{
		//TODO: Move to custom config class
		try {
			users.save(new File(getDataFolder().getPath(), FILE_USERS));
		} catch (IOException e) {
			e.printStackTrace();
			log.info("[Praecantatio] Error saving users file: " + e.getMessage());
		}

	}
    
}