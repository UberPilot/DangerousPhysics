package me.evillootly.physics;

import me.evillootly.physics.storage.PhysicsConfig;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class DangerousPhysics extends JavaPlugin implements Listener, CommandExecutor{

	public static DangerousPhysics instance;
	Random random = new Random();
	private World wor = null;
	BukkitScheduler scheduler = null;
	private ArrayList<String> worlds = new ArrayList<>();

	//physics

	private BlockDurability dur = null;
	private HashMap<Block, Integer> tempers = new HashMap<>();
	private List<Entity> effectEnts = new ArrayList<>();
	private HashMap<Entity, Long> armors = new HashMap<>();
	private List<Block> fires = new ArrayList<>();
	private List<Block> firesT = new ArrayList<>();
	private List<Entity> fallingsands = new ArrayList<>();
	private BlockSounds bs = null;
	private List<GasManager> gasManagers = new ArrayList<>();

	private PhysicsConfig physicsConfig;

	@Override
	public void onEnable()
	{
		instance = this;
		ConfigurationSerialization.registerClass(PhysicsConfig.class);

		this.loadConfig();
		bs = new BlockSounds();
		dur = new BlockDurability();
		this.getServer().getPluginManager().registerEvents(this, this);
		scheduler = getServer().getScheduler();
		/////////
		for(String world : physicsConfig.getWorlds())
		{
			if(getServer().getWorld(world) != null)
			{
				worlds.add(world);
			}
		}
		this.registerManagers();
		this.scheduleTasks();
	}

	private void registerManagers()
	{
		if(physicsConfig.isFireSmokeEnabled())
		{
			GasManager smoke = new GasManager("smoke", Particle.SMOKE_LARGE, 1, 0.6);
			gasManagers.add(smoke);
		}
		if(physicsConfig.isSteamEnabled())
		{
			GasManager steam = new GasManager("steam", Particle.CLOUD, 1, .7);
			gasManagers.add(steam);
		}
		GasManager water = new GasManager("water", Particle.BLOCK_CRACK, 0, 1);
		gasManagers.add(water);
	}

	private void scheduleTasks()
	{
		scheduler = getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, () ->
		{
			for(GasManager gm : gasManagers) {
				gm.doParticleEffects();
			}
			doGasEffects();
		}, 0L, /* 600 */10L);
		scheduler.scheduleSyncRepeatingTask(this, () ->
		{
			betterEffectLooper();
			doDynamicTemperature();
			for(GasManager gm : gasManagers) {
				gm.doGravity();
			}
		}, 0L, /* 600 */3L);
		scheduler.scheduleSyncRepeatingTask(this, () ->
		{
			runFireTemp();
			for(GasManager gm : gasManagers) {
				gm.checkLives();
			}
		}, 0L, /* 600 */700L);
		scheduler.scheduleSyncRepeatingTask(this, this::removeTemp, 0L, /* 600 */3300L);
		scheduler.scheduleSyncRepeatingTask(this, this::dofallingsands, 0L, 1L);
	}

	@Override
	public void onDisable()
	{
		instance = null;
	}

	public void loadConfig()
	{
		if(!getDataFolder().exists())
		{
			getDataFolder().mkdirs();
		}
		File configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists())
		{
			saveResource("config.yml", false);
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		this.physicsConfig = (PhysicsConfig) config.get("config");
	}

	public int coinFlip()
	{
    	if(random.nextBoolean())
    	{
    		return 1;
    	}
    	else {
    		return -1;
    	}
    }
	
	public double[] randomSpherePoint(double x0, double y0, double z0, double radius) {
		double u = Math.random();
		double v = Math.random();
		double theta = 2 * Math.PI * u;
		double phi = Math.acos(2 * v - 1);
		double x = x0 + (radius * Math.sin(phi) * Math.cos(theta));
		double y = y0 + (radius * Math.sin(phi) * Math.sin(theta));
		double z = z0 + (radius * Math.cos(phi));
		double[] points = { x, y, z };
		return points;
	}

	public void runColor(double r, double g, double b, Location l) {
		for (int zz = 0; zz < 3; zz++) {
			for (int i = 0; i < 420; i++) {
				double x = l.getX() + (random.nextInt(3) - 1);
				double y = l.getY() + (random.nextInt(3) - 1);
				double z = l.getZ() + (random.nextInt(3) - 1);
				double[] points = randomSpherePoint(x, y, z, 2);
				x = points[0];
				y = points[1];
				z = points[2];
				l.getWorld().spawnParticle(Particle.REDSTONE, x, y, z, 0, r, g, b, 1);
			}
		}
	}

	@EventHandler
	public void onSmokeFall(PotionSplashEvent event) {
		if (event.getPotion().getItem().hasItemMeta()) {
			ItemMeta i = event.getPotion().getItem().getItemMeta();
			if (i.hasLore()) {
				if (i.getLore().size() > 2) {
					String color = i.getLore().get(2);
					if (color.equals("red")) {
						runColor(1, 0.001, 0.001, event.getEntity().getLocation());
					}
					if (color.equals("lime")) {
						runColor(.3, 1, .3, event.getEntity().getLocation());
					}
					if (color.equals("black")) {
						runColor(0.001, 0.001, 0.001, event.getEntity().getLocation());
					}
					if (color.equals("green")) {
						runColor(0.388, 0.819, 0.243, event.getEntity().getLocation());
					}
					if (color.equals("brown")) {
						runColor(0.364, 0.223, 0.121, event.getEntity().getLocation());
					}
					if (color.equals("blue")) {
						runColor(0.001, 0.001, .627, event.getEntity().getLocation());
					}
					if (color.equals("purple")) {
						runColor(0.666, 0.001, .666, event.getEntity().getLocation());
					}
					if (color.equals("aqua")) {
						runColor(0.001, .666, .666, event.getEntity().getLocation());
					}
					if (color.equals("smoke")) {
						runColor(.666, .666, .666, event.getEntity().getLocation());
					}
					if (color.equals("gray")) {
						runColor(.333, .333, .333, event.getEntity().getLocation());
					}
					if (color.equals("pink")) {
						runColor(0.925, 0.364, 0.717, event.getEntity().getLocation());
					}
					if (color.equals("yellow")) {
						runColor(0.925, 0.992, 0.070, event.getEntity().getLocation());
					}
					if (color.equals("lblue")) {
						runColor(0.321, 0.858, 1, event.getEntity().getLocation());
					}
					if (color.equals("magenta")) {
						runColor(1, 0.333, 1, event.getEntity().getLocation());
					}
					if (color.equals("orange")) {
						runColor(0.992, 0.415, 0.007, event.getEntity().getLocation());
					}
					if (color.equals("white")) {
						runColor(1, 1, 1, event.getEntity().getLocation());
					}
				}
			}
		}
	}

    //Physics Engine
	public void placeFallingSand(Entity ee, int check) {
		if(!worlds.contains(ee.getWorld().getName())) {
			return;
		}
		Location dothing = ee.getLocation();
		boolean cando = false;
		Material m = ee.getLocation().getBlock().getType();
		if(isAir(m) || m==Material.LAVA || m==Material.WATER || ee.getLocation().getBlock().isPassable()) {
			cando=true;
		}
		if(!cando) {
			Location ltemp = ee.getLocation().clone().add(0, 1, 0);
			for(int i = 0; i < 16; i++) {
				Material newm = ltemp.clone().add(0, i, 0).getBlock().getType();
				if(isAir(newm)) {
					dothing = ltemp.clone().add(0, i, 0);
					break;
				}
			}
		}
		if(check==0) {
			dothing.getBlock().setType(Material.getMaterial(ee.getMetadata("fallsand1").get(0).asString()));
			ee.getWorld().playSound(dothing, bs.getBreakSound(dothing.getBlock().getType()+""), 1, 1);
			fallingsands.remove(ee);
			ee.remove();
		}
		else if(check==1) {
			dothing.getBlock().setType(Material.getMaterial(ee.getPassengers().get(0).getMetadata("fallsand1").get(0).asString()));
			ee.getWorld().playSound(dothing, bs.getBreakSound(dothing.getBlock().getType()+""), 1, 1);
			fallingsands.remove(ee.getPassengers().get(0));
			ee.getPassengers().get(0).remove();
			fallingsands.remove(ee);
			ee.remove();
		}
	}

	public void dofallingsands() {
		if(!fallingsands.isEmpty()) {
			List<Entity> fallingsands2 = new ArrayList<Entity>(fallingsands);
			for(Entity ee : fallingsands2) {
				if(ee!=null) {
					if(ee.isOnGround() ||(ee.getLocation().getBlock().getType()==Material.LAVA||ee.getLocation().getBlock().getType()==Material.WATER)) {
						if(ee instanceof ArmorStand) {
							placeFallingSand(ee, 0);
						}
						else {
							Vector v = null;
							if(ee.getPassengers().size()>0) {
							if(ee.getPassengers().get(0).getMetadata("fallsand1").size()>0) {
								if(dur.softs.contains("" + Material.getMaterial(ee.getPassengers().get(0).getMetadata("fallsand1").get(0).asString()))) {
									v = checkSides(ee.getLocation().getBlock().getLocation());
								}
							}
							if(v==null) {
								ee.getPassengers();
								if(ee.getPassengers().size()>0) {
									placeFallingSand(ee, 1);
								}
								else {
									fallingsands.remove(ee);
									ee.remove();
								}
							}
							else {
								ee.setVelocity(v);
							}
							}
							else {
								fallingsands.remove(ee);
								ee.remove();
							}
						}
					}
				}
				else {
					fallingsands.remove(ee);
				}
			}
		}
	}

	public Vector checkSides(Location l) {
		Vector v = null;
		List<BlockFace> sidetypes = new ArrayList<BlockFace>();
		BlockFace[] sides = {BlockFace.WEST,BlockFace.SOUTH,BlockFace.NORTH,BlockFace.EAST};
		for (BlockFace side : sides)
		{
			Block b1 = l.clone().getBlock().getRelative(side);
			Block b2 = l.clone().subtract(0, 1, 0).getBlock().getRelative(side);
			if (isAir(b1.getType()) && isAir(b2.getType()))
			{
				sidetypes.add(side);
			}
		}
		if(sidetypes.size()>0) {
		BlockFace choosen = sidetypes.get(random.nextInt(sidetypes.size()));
		if(choosen == BlockFace.WEST) {
			v = new Vector(-.3, 0, 0);
		}
		else if(choosen == BlockFace.NORTH) {
			v = new Vector(0, 0, -.3);
		}
		else if(choosen == BlockFace.SOUTH) {
			v = new Vector(0, 0, .3);
		}
		else if(choosen == BlockFace.EAST) {
			v = new Vector(.3, 0, 0);
		}
		return v;
		}
		else {
			return null;
		}
	}

	@EventHandler
	public void setWorld(PlayerJoinEvent event) {
		if(wor==null) {
			wor = event.getPlayer().getWorld();
		}
	}

	@SuppressWarnings("unused")
	public void betterEffectLooper() {
		if(!effectEnts.isEmpty()) {
			List<Entity> tempEntss = new ArrayList<Entity>(effectEnts);
			for(Entity e : tempEntss) {
				LivingEntity e2 = (LivingEntity) e;
				if(e2 != null) {
					if(!e2.isDead()) {
						if(e2 instanceof Player) {
							Location feet = e2.getLocation().subtract(0, 1, 0);
							Location leg = e2.getLocation();
							Location head = e2.getLocation().add(0, 1, 0);
							int temp1 = 0; int temp2 = 0; int temp3 = 0;
							boolean hasTemp = false;
							if(feet.getBlock().hasMetadata("T")) {
								hasTemp = true;
								if(!feet.getBlock().getMetadata("T").isEmpty()) {
									temp1 = feet.getBlock().getMetadata("T").get(0).asInt();
								}
							}
							if(leg.getBlock().hasMetadata("T")) {
								hasTemp = true;
								if(!leg.getBlock().getMetadata("T").isEmpty()) {
									temp2 = leg.getBlock().getMetadata("T").get(0).asInt();
								}
							}
							if(head.getBlock().hasMetadata("T")) {
								hasTemp = true;
								if(!head.getBlock().getMetadata("T").isEmpty()) {
									temp3 = head.getBlock().getMetadata("T").get(0).asInt();
								}
							}
							if(!hasTemp) {
								effectEnts.remove(e);
							}
							else {
								runTemperature(((Player) e2), Math.max(Math.max(temp1, temp2), temp3));
							}
						}
						else {
							effectEnts.remove(e);
						}
						}
					else {
						effectEnts.remove(e);
					}
				}
				else {
					effectEnts.remove(e);
				}
			}
		}
	}


	public void runFireTemp() {
		if(physicsConfig.isTemperatureEnabled()) {
		if(!fires.isEmpty()) {
			List<Block> fires2 = new ArrayList<Block>(fires);
			for(Block b : fires2) {
				//removal
				if(b.getType()!=Material.FIRE) {
					fires.remove(b);
					if(b.hasMetadata("T")) {
						if(b.getMetadata("T").size()>0) {
							tempers.put(b, b.getMetadata("T").get(0).asInt());
						}
						else {
							tempers.put(b, 2000);
						}
					}
					else {
						tempers.put(b, 2000);
					}
					BlockFace[] sides = {BlockFace.DOWN,BlockFace.WEST,BlockFace.UP,BlockFace.SOUTH,BlockFace.NORTH,BlockFace.EAST};
					for (BlockFace side : sides)
					{
						Block b2 = b.getRelative(side);
						if (b2.getType() != Material.FIRE)
						{
							fires.remove(b2);
							firesT.remove(b2);
							if (b2.hasMetadata("T"))
							{
								if (b2.getMetadata("T").size() > 0)
								{
									tempers.put(b2, b2.getMetadata("T").get(0).asInt());
								}
								else
								{
									tempers.put(b2, 2000);
								}
							}
							else
							{
								tempers.put(b2, 2000);
							}
						}
					}
				}
				//
				else {
				BlockFace[] sides = {BlockFace.DOWN,BlockFace.WEST,BlockFace.UP,BlockFace.SOUTH,BlockFace.NORTH,BlockFace.EAST,BlockFace.SELF};
					for (BlockFace side : sides)
					{
						Block b2 = b.getRelative(side);
						if (side != BlockFace.SELF)
						{
							if (!firesT.contains(b2))
							{
								firesT.add(b2);
							}
						}
						if (b2.hasMetadata("T"))
						{
							if (b2.getMetadata("T").size() > 0)
							{
								if (b2.getMetadata("T").get(0).asInt() < 2200)
								{
									b2.setMetadata("T", new FixedMetadataValue(this,
											b2.getMetadata("T").get(0).asInt() + 300));
								}
							}
							else
							{
								b2.setMetadata("T", new FixedMetadataValue(this, 300));
							}
						}
						else
						{
							b2.setMetadata("T", new FixedMetadataValue(this, 300));
						}
					}
			}
			}
		}
		}
	}

	public void runTemperature(Player p, int temp) {
		if(!worlds.contains(p.getWorld().getName())) {
			return;
		}
		if(temp<125) {
			if(random.nextInt(20)==0) {
				p.damage(1);
			}
		}
		else if(temp<256) {
			if(random.nextInt(16)==0) {
				p.damage(1);
			}
		}
		else if(temp<526) {
			if(random.nextInt(15)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(40);
				}
			}
		}
		else if(temp<1027) {
			if(random.nextInt(10)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(80);
				}
			}
		}
		else if(temp<2546) {
			if(random.nextInt(7)==0) {
				p.damage(2);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(160);
				}
			}
		}
		else if(temp<5678) {
			if(random.nextInt(4)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(360);
				}
			}
		}
		else {
			if(random.nextInt(2)==0) {
				p.damage(4);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(1200);
				}
			}
		}
	}

	public void runTemperatureE(LivingEntity p, int temp) {
		if(!worlds.contains(p.getWorld().getName())) {
			return;
		}
		if(temp<125) {
			if(random.nextInt(20)==0) {
				p.damage(1);
			}
		}
		else if(temp<256) {
			if(random.nextInt(16)==0) {
				p.damage(1);
			}
		}
		else if(temp<526) {
			if(random.nextInt(15)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(40);
				}
			}
		}
		else if(temp<1027) {
			if(random.nextInt(10)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(80);
				}
			}
		}
		else if(temp<2546) {
			if(random.nextInt(7)==0) {
				p.damage(2);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(160);
				}
			}
		}
		else if(temp<5678) {
			if(random.nextInt(4)==0) {
				p.damage(1);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(360);
				}
			}
		}
		else {
			if(random.nextInt(2)==0) {
				p.damage(4);
				if(p.getFireTicks()<=0) {
					p.setFireTicks(1200);
				}
			}
		}
	}

	public void removeTemp() {
		if(!tempers.isEmpty()) {
			HashMap<Block, Integer> temps = new HashMap<Block, Integer>(tempers);
			for(Block b : temps.keySet()) {
				int newTemp = temps.get(b)/4;
				if(newTemp<=1) {
					b.removeMetadata("T", this);
					tempers.remove(b);
					fires.remove(b);
					firesT.remove(b);
				}
				else {
					tempers.put(b, newTemp);
					b.setMetadata("T", new FixedMetadataValue(this, newTemp));
				}
			}
		}
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent e) {
		if(!worlds.contains(e.getBlock().getLocation().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isEnhancedFireEnabled()) {
		if(!e.isCancelled()) {
			if(!physicsConfig.isNetherrackEnabled()) {
				if(e.getBlock().getLocation().subtract(0, 1, 0).getBlock().getType()==Material.NETHERRACK) {
					return;
				}
				else if(e.getBlock().getType()==Material.NETHERRACK) {
					return;
				}
			}
			if(!fires.contains(e.getBlock())) {
				tempers.remove(e.getBlock());
				firesT.remove(e.getBlock());
				fires.add(e.getBlock());
			}
		}
		}
	}

	public void doGasEffects() {
		for(GasManager gm : gasManagers) {
			List<Location> gsLocations = new ArrayList<Location>(gm.getLocations());
			if(!gsLocations.isEmpty()) {
				for(Location l : gsLocations) {
					for(Entity e : l.getWorld().getNearbyEntities(l, 1, 1, 1)) {
						if(e instanceof LivingEntity) {
						if(gm.name.equals("smoke")) {
								((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 200));
								((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
						}
						else if(gm.name.equals("steam")) {
							if(random.nextInt(20)==0) {
							((LivingEntity) e).damage(1);
							}
						}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onBurn(BlockBurnEvent e) {
		if(!worlds.contains(e.getBlock().getLocation().getWorld().getName())) {
			return;
		}
		else {
		if(physicsConfig.isAshEnabled()) {
			Material m = e.getBlock().getType();
		if(m.name().toLowerCase().contains("wood")||m.name().toLowerCase().contains("log")||m.name().toLowerCase().contains("plank")) {
			e.setCancelled(true);
			e.getBlock().setType(Material.LIGHT_GRAY_CONCRETE_POWDER);
		}
		else if(m.name().toLowerCase().contains("leave")) {
			if(random.nextInt(7)==0) {
				e.setCancelled(true);
				e.getBlock().setType(Material.LIGHT_GRAY_CONCRETE_POWDER);
			}
		}
		}
		if(physicsConfig.isFireSmokeEnabled()) {
		gasManagers.get(0).addGasLocation(e.getBlock().getLocation().add(0, 1, 0), 1);
		}
		}
	}

	public void tempBlockChanges(Block b, int temp) {
		if(!worlds.contains(b.getLocation().getWorld().getName())) {
			return;
		}
		if(b.getWorld().getBlockAt(b.getLocation()).getType()==Material.FIRE&&temp>600) {
			if(random.nextBoolean()) {
			b.getWorld().spawnParticle(Particle.FLAME, b.getLocation().add(0.5, 0.5, 0.5), 1, .5, .5, .5, 0.03);
			}
		}
		if(temp>1000&&temp<1600) {
			if(random.nextInt(20)==0) {
			tempAbove1000(b);
			}
		}
		else if(temp>1600&&temp<2000) {
			if(random.nextInt(20)==0) {
			tempAbove1600(b);
			}
		}
		else if(temp>2000) {
			if(random.nextInt(20)==0) {
			tempAbove2000(b);
			}
		}
	}

	public void tempAbove1000(Block b) {
		Location loc = b.getLocation();
		int radius = 2;
		int cx = loc.getBlockX();
		int cy = loc.getBlockY();
		int cz = loc.getBlockZ();
		for(int x = cx - radius; x <= cx + radius; x++){
		for (int z = cz - radius; z <= cz + radius; z++){
		for(int y = (cy - radius); y < (cy + radius); y++){
		double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + ((cy - y) * (cy - y));

			if(dist < radius * radius){
				Location l = new Location(loc.getWorld(), x, y, z);
				Material m = l.getBlock().getType();
				if(m==Material.GRASS_BLOCK||m.name().toLowerCase().contains("dirt")) {
				if(random.nextInt(10)==0) {
					int decided = random.nextInt(3);
					if(decided==0) {
					l.getBlock().setType(Material.SAND);
					}
					else if(decided==1) {
						l.getBlock().setType(Material.DIRT);
					}
					else if(decided==2) {
						l.getBlock().setType(Material.COARSE_DIRT);
					}
				}
				}
				else if(m == Material.WATER) {
					if(random.nextInt(10)==0) {
					l.getBlock().setType(Material.AIR);
					gasManagers.get(1).addGasLocation(l.add(0, 1, 0), 1);
					}
				}
			}
		}
		}
		}
	}

	public void tempAbove1600(Block b) {
		Location loc = b.getLocation();
		int radius = 3;
		int cx = loc.getBlockX();
		int cy = loc.getBlockY();
		int cz = loc.getBlockZ();
		for(int x = cx - radius; x <= cx + radius; x++){
		for (int z = cz - radius; z <= cz + radius; z++){
		for(int y = (cy - radius); y < (cy + radius); y++){
		double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + ((cy - y) * (cy - y));

			if(dist < radius * radius){
				Location l = new Location(loc.getWorld(), x, y, z);
				Material m = l.getBlock().getType();
				if(m==Material.GRASS_BLOCK||m.name().toLowerCase().contains("dirt")) {
				if(random.nextInt(10)==0) {
					int decided = random.nextInt(3);
					if(decided==0) {
					l.getBlock().setType(Material.SAND);
					}
					else if(decided==1) {
						l.getBlock().setType(Material.DIRT);
					}
					else if(decided==2) {
						l.getBlock().setType(Material.COARSE_DIRT);
					}
				}
				}
				else if(isStony(m)&&(!(m.name().toLowerCase().contains("redstone")))) {
					if(random.nextInt(10)==0) {
						if(l.getBlock().getType()==Material.COBBLESTONE) {
							if(random.nextBoolean()) {
								l.getBlock().setType(Material.STONE);
							}
						}
						else {
							l.getBlock().setType(Material.MAGMA_BLOCK);
						}
					}
				}
				else if(m.name().toLowerCase().contains("leave")||m.name().toLowerCase().contains("wood")||m.name().toLowerCase().contains("log")||m.name().toLowerCase().contains("plank")) {
					boolean doRealisticSpreading = false;
					if(doRealisticSpreading) {
					if(random.nextInt(20)==0) {
						l.getBlock().setType(Material.FIRE);
					}
					}
				}
				else if(m == Material.WATER) {
					if(random.nextInt(10)==0) {
					l.getBlock().setType(Material.AIR);
					gasManagers.get(1).addGasLocation(l.add(0, 1, 0), 1);
					}
				}
			}
		}
		}
		}
	}

	public void tempAbove2000(Block b) {
		Location loc = b.getLocation();
		int radius = 3;
		int cx = loc.getBlockX();
		int cy = loc.getBlockY();
		int cz = loc.getBlockZ();
		for(int x = cx - radius; x <= cx + radius; x++){
		for (int z = cz - radius; z <= cz + radius; z++){
		for(int y = (cy - radius); y < (cy + radius); y++){
		double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + ((cy - y) * (cy - y));

			if(dist < radius * radius){
				Location l = new Location(loc.getWorld(), x, y, z);
				Material m = l.getBlock().getType();
				if((isStony(m)||m == Material.MAGMA_BLOCK)&&(!(m.name().toLowerCase().contains("red")))) {
					if(random.nextInt(10)==0) {
						if(random.nextInt(5)==0) {
							l.getBlock().setType(Material.LAVA);
						}
						else {
							l.getBlock().setType(Material.MAGMA_BLOCK);
						}
					}
				}
				else if(m == Material.WATER) {
					if(random.nextInt(6)==0) {
					l.getBlock().setType(Material.AIR);
					gasManagers.get(1).addGasLocation(l.add(0, 1, 0), 1);
					}
				}
			}
		}
		}
		}
	}

	@EventHandler
	public void onWaterChange(BlockFormEvent e) {
		if(!worlds.contains(e.getBlock().getLocation().getWorld().getName())) {
			return;
		}
		if (!e.isCancelled())
		{
			if(physicsConfig.isSteamEnabled()) {
			if(e.getBlock().getType()==Material.LAVA||e.getBlock().getType()==Material.WATER) {
					gasManagers.get(1).addGasLocation(e.getBlock().getLocation().add(0, 2, 0), 1);
			}
			}
		}
	}

	public void doDynamicTemperature() {
		if(!tempers.isEmpty()) {
			HashMap<Block, Integer> temps = new HashMap<Block, Integer>(tempers);
			for(Block b : temps.keySet()) {
				for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 2, 2, 2)) {
					if(e instanceof LivingEntity && (!(e instanceof Player))) {
						runTemperatureE((LivingEntity) e, temps.get(b));
					}
				}
				if(b.getWorld().getBlockAt(b.getLocation()).getType()==Material.FIRE&&temps.get(b)>300) {
					if(random.nextBoolean()) {
						doRandomFireParticle(b.getLocation().add(0.5, 0.5, 0.5));
					}
				}
				if(temps.get(b)>1000) {
					if(random.nextInt(10)==0) {
						Location oldL = b.getLocation().add((random.nextInt(100)/100.0), (random.nextInt(100)/100.0), (random.nextInt(100)/100.0));
						oldL.getWorld().spawnParticle(Particle.SMOKE_NORMAL, oldL, 1, 0, 0, 0, 0.01);
					}
					tempBlockChanges(b, temps.get(b));
				}
			}
		}
		if(!fires.isEmpty()) {
			List<Block> temps = new ArrayList<Block>(fires);
			for(Block b : temps) {
				int temp = 0;
				if(b.hasMetadata("T")) {
					if(b.getMetadata("T").size()>0) {
						temp = b.getMetadata("T").get(0).asInt();
					}
				}
				if(temp>0) {
				for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 2, 2, 2)) {
					if(e instanceof LivingEntity && (!(e instanceof Player))) {
						runTemperatureE((LivingEntity) e, temp);
					}
				}
				if(b.getWorld().getBlockAt(b.getLocation()).getType()==Material.FIRE&&temp>300) {
					if(random.nextBoolean()) {
						doRandomFireParticle(b.getLocation().add(0.5, 0.5, 0.5));
					}
				}
				if(temp>1000) {
					if(random.nextInt(10)==0) {
						Location oldL = b.getLocation().add((random.nextInt(100)/100.0), (random.nextInt(100)/100.0), (random.nextInt(100)/100.0));
						oldL.getWorld().spawnParticle(Particle.SMOKE_NORMAL, oldL, 1, 0, 0, 0, 0.01);
					}
					tempBlockChanges(b, temp);
				}
				}
			}
		}
		if(!firesT.isEmpty()) {
			List<Block> temps = new ArrayList<Block>(firesT);
			for(Block b : temps) {
				int temp = 0;
				if(b.hasMetadata("T")) {
					if(b.getMetadata("T").size()>0) {
						temp = b.getMetadata("T").get(0).asInt();
					}
				}
				if(temp>0) {
				for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 2, 2, 2)) {
					if(e instanceof LivingEntity && (!(e instanceof Player))) {
						runTemperatureE((LivingEntity) e, temp);
					}
				}
				if(b.getWorld().getBlockAt(b.getLocation()).getType()==Material.FIRE&&temp>300) {
					if(random.nextBoolean()) {
						doRandomFireParticle(b.getLocation().add(0.5, 0.5, 0.5));
					}
				}
				if(temp>1000) {
					if(random.nextInt(10)==0) {
						Location oldL = b.getLocation().add((random.nextInt(100)/100.0), (random.nextInt(100)/100.0), (random.nextInt(100)/100.0));
						oldL.getWorld().spawnParticle(Particle.SMOKE_NORMAL, oldL, 1, 0, 0, 0, 0.01);
					}
					tempBlockChanges(b, temp);
				}
				}
			}
		}
	}

	public void doBlockStuff(List<Block> bl, int key, int eRange, Location begin) {
		for(Block bls : bl) {
			if(random.nextInt(8)==0) {
				if(random.nextBoolean() && random.nextInt(40)!=0) {
				bls.getWorld().playSound(bls.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
				}
				else {
					bls.getWorld().playSound(bls.getLocation(), bs.getBreakSound(bls.getLocation().getBlock().getType()+""), 1, 1);
				}
			}
			if(random.nextInt(20)==0) {
				if(key>eRange-eRange/5) {
					//if(config.getBoolean("Enable Block Physics ")) {
					if(random.nextInt(2)==0 && (!(isAir(bls.getType())))) {
						Entity fb;
						fb = spawnRealFSand(bls.getLocation(), bls.getType());
						if(fb!=null) {
						fb.setVelocity(begin.toVector().subtract(fb.getLocation().toVector()).normalize().multiply(1.2));
						}
						bls.setType(Material.AIR);
						if(bls.hasMetadata("D")) {
							bls.removeMetadata("D", this);
						}
					}
					//}
					bls.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, bls.getLocation(), 1);
				}
				else if(key>eRange-eRange/2) {
					bls.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, bls.getLocation(), 1);
				}
				else {
					bls.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, bls.getLocation(), 1);
				}
			}
			if(random.nextInt(20)==0) {

			}
			if(bls.getType()==Material.TNT) {
				bls.setType(Material.AIR);
				HashMap<Integer, List<Block>> empty = new HashMap<Integer, List<Block>>();
				iterate(empty, bls.getLocation(), (int) (.25*16)*2, (int) ((.25*16)*(.25*16)*(.25*16))*2, 350);
			}
			if(bls.hasMetadata("D")) {
				bls.removeMetadata("D", this);
			}
			if(random.nextInt(5)!=0) {
				bls.setType(Material.AIR);
			}
			else {
					bls.breakNaturally();
			}
		}
	}
	@EventHandler
	public void onPMove(PlayerMoveEvent event) {
		if(!worlds.contains(event.getPlayer().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isTemperatureEnabled()) {
		if(!tempers.isEmpty()) {
			if(!effectEnts.contains(event.getPlayer())) {
		Player e2 = event.getPlayer();
		Location feet = e2.getLocation().subtract(0, 1, 0);
		Location leg = e2.getLocation();
		Location head = e2.getLocation().add(0, 1, 0);
		int temp1 = 0; int temp2 = 0; int temp3 = 0;
		if(feet.getBlock().hasMetadata("T")) {
			if(!feet.getBlock().getMetadata("T").isEmpty()) {
				temp1 = feet.getBlock().getMetadata("T").get(0).asInt();
			}
		}
		if(leg.getBlock().hasMetadata("T")) {
			if(!leg.getBlock().getMetadata("T").isEmpty()) {
				temp2 = leg.getBlock().getMetadata("T").get(0).asInt();
			}
		}
		if(head.getBlock().hasMetadata("T")) {
			if(!head.getBlock().getMetadata("T").isEmpty()) {
				temp3 = head.getBlock().getMetadata("T").get(0).asInt();
			}
		}
		if(Math.max(Math.max(temp1, temp2), temp3)>32) {
			effectEnts.add(e2);
		}
			}
		}
		}
	}

    @EventHandler
    public void onBlockBreak2(BlockBreakEvent event) {
        if(!instance.getWorlds().contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if(event.getBlock().hasMetadata("D")) {
            event.getBlock().removeMetadata("D", instance);
        }
    }

    @EventHandler
    public void onBlockPlace2(BlockPlaceEvent event) {
        if(!instance.getWorlds().contains(event.getBlock().getWorld().getName())) {
            return;
        }
        if(event.getBlock().hasMetadata("D")) {
            event.getBlock().removeMetadata("D", instance);
        }
    }

	public void iterate(HashMap<Integer, List<Block>> blocksF, Location locer, int eRange, int amount, int temperature){
		if(eRange>20 || eRange<0) {
			return;
		}
		int times = 1000;
		if(amount <= 1000) {
			times = amount;
		}
		amount = amount - 1000;
		for(int i = 0; i < times; i++) {
			final BlockIterator bit = new BlockIterator(wor, locer.toVector(), new Vector(0.0D + Math.random() - Math.random(),0.0D + Math.random() - Math.random(),0.0D + Math.random() - Math.random()), 0, eRange);
			int blockNum = 1;
			double power = eRange*eRange*eRange/3;
			int temptemp = temperature;
			while (bit.hasNext()) {
				final Block next = bit.next();
				String name = (next.getType()+"").toLowerCase();
				if((name.contains("redstone")&&(!(name.contains("block")||name.contains("ore"))))||name.contains("bedrock")||name.contains("barrier")||name.contains("command")) {
					break;
				}
				HashMap<Double, Integer> newV = runBlockDecision(next, power, locer, temptemp);
				for(Double d : newV.keySet()) {
					power = d;
					temptemp = newV.get(d);
				}
				if(power>0) {
					if(blocksF.containsKey(blockNum)) {
						if(!blocksF.get(blockNum).contains(next)) {
						blocksF.get(blockNum).add(next);
						}
					}
					else {
						List<Block> blnew = new ArrayList<Block>();
						blnew.add(next);
						blocksF.put(blockNum, blnew);
					}
					blockNum++;
				}
				else {
					break;
				}
			}
		}
		if(amount > 1000) {
			final int am = amount;
			Bukkit.getScheduler().runTaskLater(this, () -> iterate(blocksF, locer, eRange, am, temperature), 40);
		}
		else {
			throwExplosion(locer, eRange, blocksF);
		}
	}

	public HashMap<Double, Integer> runBlockDecision(Block b, double power, Location begin, int temp) {
		HashMap<Double, Integer> values = new HashMap<>();
		Material m = b.getType();
		double pow2 = power;
		int temp2 = temp;
		if(b.hasMetadata("D")) {
			if(!b.getMetadata("D").isEmpty()) {
			pow2 = pow2 - b.getMetadata("D").get(0).asDouble();
			}
		}
		else {
			pow2 = pow2 - dur.duras.get(m + "");
		}
		if(physicsConfig.isTemperatureEnabled()) {
		if(b.hasMetadata("T")) {
			if(!b.getMetadata("T").isEmpty()) {
				int calc = (int) (temp2 / (dur.temp.get(b.getType() + "")/4.0));
				int tempB = b.getMetadata("T").get(0).asInt();
					temp2 = temp2 - dur.temp.get(b.getType() + "");
				if(tempB>=temp2) {
					temp2 = calc;
				}
				else {
					b.setMetadata("T", new FixedMetadataValue(this, temp2));
					tempers.put(b, temp2);
					temp2 = calc;
				}
			}
		}
		else {
			int calc = (int) (temp2 / (dur.temp.get(b.getType() + "")/4.0));
			b.setMetadata("T", new FixedMetadataValue(this, temp2));
			tempers.put(b, temp2);
			temp2 = calc;
		}
		}
		for(Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1)) {
			if(e instanceof LivingEntity) {
				if(pow2>0) {
					if(e instanceof Player) {
						DamageReducer.ReducedDamage(((LivingEntity) e), pow2/7.8);
					}
					else {
						((LivingEntity) e).damage(pow2/2);
					}
				}
			}
			if(pow2>0) {
					e.setVelocity(begin.toVector().subtract(e.getLocation().toVector()).normalize().multiply(-1-(pow2/100)));
			}
		}
		if(pow2<=0) {
			if(b.hasMetadata("D")) {
				if(!b.getMetadata("D").isEmpty()) {
				b.setMetadata("D", new FixedMetadataValue(this, b.getMetadata("D").get(0).asDouble() - power));
				if(b.getMetadata("D").get(0).asDouble()<=dur.duras.get(m + "")/2.0) {
					if(dur.converts.containsKey(b.getType() + "")) {
					b.setType(dur.converts.get(b.getType() + ""));
					}
				}
				}
				values.put(pow2, temp2);
				return values;
			}
			else {
				b.setMetadata("D", new FixedMetadataValue(this, dur.duras.get(m + "")-power));
				if(!b.getMetadata("D").isEmpty()) {
				if(b.getMetadata("D").get(0).asDouble()<=dur.duras.get(m + "")/2.0) {
					if(dur.converts.containsKey(b.getType() + "")) {
					b.setType(dur.converts.get(b.getType() + ""));
					}
				}
				}
				values.put(pow2, temp2);
				return values;
			}
		}
		else {
			values.put(pow2, temp2);
			return values;
		}
	}

	@EventHandler
	public void onDamaged(EntityDamageEvent event) {
		if(!worlds.contains(event.getEntity().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isEnhancedExplosionsEnabled()) {
		if(event.getCause()==DamageCause.BLOCK_EXPLOSION || event.getCause()==DamageCause.ENTITY_EXPLOSION) {
			event.setCancelled(true);
		}
		}
	}

	public HashMap<Integer, List<Block>> getBlocks(Location locer, int eRange){
		HashMap<Integer, List<Block>> blocks = new HashMap<>();
		//block range getter
		int fullRadi = eRange * eRange * eRange;
		if(fullRadi>6000) {
		for(int i = 0; i < eRange * eRange * eRange; i++) {
			//final BlockIterator bit = new BlockIterator(wor, loc.toVector(), new Vector(random.nextInt(360)+1,random.nextInt(360)+1,random.nextInt(360)+1), 0, radius);
			final BlockIterator bit = new BlockIterator(wor, locer.toVector(), new Vector(0.0D + Math.random() - Math.random(),0.0D + Math.random() - Math.random(),0.0D + Math.random() - Math.random()), 0, eRange);
			int blockNum = 1;
			while (bit.hasNext()) {
				final Block next = bit.next();
				if(blocks.containsKey(blockNum)) {
					if(!blocks.get(blockNum).contains(next)) {
					blocks.get(blockNum).add(next);
					}
				}
				else {
					List<Block> blnew = new ArrayList<Block>();
					blnew.add(next);
					blocks.put(blockNum, blnew);
				}
				blockNum++;
			}
		}
		}
		return blocks;
	}

	public void throwExplosion(Location locer, int eRange, HashMap<Integer, List<Block>> blocks) {
		locer.getWorld().playSound(locer, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
		for(int key : blocks.keySet()) {
			int counterr = 0;
			if(eRange>20) {
				Bukkit.getScheduler().runTaskLater(this,
						() -> doBlockStuff(blocks.get(key), key, eRange, locer), 40+counterr);
				counterr+=41;
			}
			else {
				doBlockStuff(blocks.get(key), key, eRange, locer);
			}
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if(!worlds.contains(event.getEntity().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isEnhancedExplosionsEnabled()) {
		if(!event.isCancelled()) {
			if (!(event.getYield() < 0) && !(event.getYield() > 50))
			{
		if(event.getEntityType()==EntityType.PRIMED_TNT||event.getEntityType()==EntityType.CREEPER||event.getEntityType()==EntityType.FIREBALL||event.getEntityType()==EntityType.WITHER_SKULL) {
			event.setCancelled(true);
			event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
			HashMap<Integer, List<Block>> empty = new HashMap<Integer, List<Block>>();
			if(event.getEntityType()==EntityType.FIREBALL||event.getEntityType()==EntityType.WITHER_SKULL) {
				iterate(empty, event.getLocation(), (int) (3), (int) (30/*(1.31245*16)*(1.3125*16)*(1.31245*16)*/), (int) (/*16*((1.31245*16)*(1.31245*16)*(1.31245*16))*/30));
			    doFireExplosionEffect(event.getLocation());
			}
			else {
			iterate(empty, event.getLocation(), (int) (event.getYield()*16), (int) ((event.getYield()*16)*(event.getYield()*16)*(event.getYield()*16)), (int) (16*((event.getYield()*16)*(event.getYield()*16)*(event.getYield()*16))));
			}
			}
		}
		}
		}
	}

	public void doFireExplosionEffect(Location l) {
		Location loc = l.add(0, 0.5, 0);
		for(int count = 0; count < 10; count++) {
			Entity sand;
			if(random.nextBoolean()) {
				sand = spawnRealFSand(loc, Material.FIRE);
			}
			else {
				sand = spawnRealFSand(loc, Material.MAGMA_BLOCK);
			}
			if(sand != null) {
				sand.setVelocity(new Vector((coinFlip()*(random.nextInt(30)+30))/100.0,(random.nextInt(3)+30)/100.0,(coinFlip()*(random.nextInt(30)+30))/100.0));
			}
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if(!worlds.contains(event.getBlock().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isEnhancedExplosionsEnabled()) {
		if(!event.isCancelled()) {
			if (!(event.getYield() < 0) && !(event.getYield() > 50))
			{
		event.setCancelled(true);
		event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
		HashMap<Integer, List<Block>> empty = new HashMap<Integer, List<Block>>();
		iterate(empty, event.getBlock().getLocation(), (int) (event.getYield()*16), (int) ((event.getYield()*16)*(event.getYield()*16)*(event.getYield()*16)), (int) (16*((event.getYield()*16)*(event.getYield()*16)*(event.getYield()*16))));
			}
		}
		}
	}

	public Entity spawnRealFSand(Location l, Material m) {
		if(m == Material.SAND || m == Material.GRAVEL || m.name().toLowerCase().contains("powder")) {
			return null;
		}
		return l.getWorld().spawnFallingBlock(l, m.createBlockData());
	}

	@EventHandler
	public void onArmorHitBlock(EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof ArmorStand && e.getDamager() instanceof Player) {
			if(!((ArmorStand) e.getEntity()).isVisible()) {
				if(e.getEntity().hasMetadata("fallsand1")) {
					e.getEntity().remove();
				}
			}
		}
	}

	@EventHandler
	public void onRedstone(BlockRedstoneEvent e) {
		if(!worlds.contains(e.getBlock().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isEnhancedExplosionsEnabled()) {
		Block b = e.getBlock().getRelative(BlockFace.DOWN);
		if(b.hasMetadata("D")) {
			if(b.getMetadata("D").size()>0) {
				int amount = b.getMetadata("D").get(0).asInt() + 30;
				b.setMetadata("D", new FixedMetadataValue(this, amount));
			}
			else {
				b.setMetadata("D", new FixedMetadataValue(this, 30));
			}
		}
		else {
			b.setMetadata("D", new FixedMetadataValue(this, 30));
		}
		}
	}

	//block gravity

	@EventHandler
	public void onBlockBreakedG(BlockBreakEvent event) {
		if(!worlds.contains(event.getBlock().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isBlockPhysicsEnabled()) {
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			if(!testGravity(event.getBlock().getLocation(), 0, 0)) {
				Location loc = event.getBlock().getLocation();
				int radius = 4;
				int cx = loc.getBlockX();
				int cy = loc.getBlockY();
				int cz = loc.getBlockZ();
				for(int x = cx - radius; x <= cx + radius; x++){
				for (int z = cz - radius; z <= cz + radius; z++){
				for(int y = (cy - radius); y < (cy + radius); y++){
				double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + ((cy - y) * (cy - y));

					if(dist < radius * radius){
						Location l = new Location(loc.getWorld(), x, y, z);
						if((!(isAir(l.getBlock().getType())))&&(!l.equals(loc))) {
							if(!testGravity(l, 0, 1)) {
								spawnRealFSand(l.add(0.5, 0, 0.5), l.getBlock().getType());
								l.getBlock().getLocation().getBlock().setType(Material.AIR);
							}
						}
					}
				}
				}
				}
			}
			}
		}
	}

	@EventHandler
	public void onBlockPlacedG(BlockPlaceEvent event) {
		if(!worlds.contains(event.getBlock().getWorld().getName())) {
			return;
		}
		if(physicsConfig.isBlockPhysicsEnabled()) {
		if(!event.isCancelled()) {
			if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			if(!testGravity(event.getBlock().getLocation(), 0, 1)) {
				spawnRealFSand(event.getBlock().getLocation().add(0.5, 0, 0.5), event.getBlock().getType());
				event.setCancelled(true);
				//event.getBlock().getLocation().getBlock().setType(Material.AIR);
			}
			}
		}
		}
	}

	private boolean checkDowns(Location l, int safeAm) {
		boolean isTrue = true;
		for(int i = 0; i < safeAm; i++) {
			if(isAir(l.getBlock().getType())) {
				isTrue = false;
				break;
			}
			l.subtract(0, 1, 0);
		}
		return isTrue;
	}

	private boolean testGravity(Location l, int itercount, int type) {
		boolean isSafe = false;
		int lengthSafety = dur.lengths.get("" + l.getBlock().getType());
		if(itercount >= 4 || lengthSafety==25) {
			return true;
		}
		Location checkUp = l.clone();
		boolean hasSupport = true;
		boolean hasSafety = false;
		boolean hasAir = false;
		for(int i = 0; i < lengthSafety+1; i++) {
			if(isAir(checkUp.add(0, 1, 0).getBlock().getType())) {
				hasAir = true;
				break;
			}
		}
		Location checkDown = l.clone();
		for(int i = 0; i < lengthSafety; i++) {
			if(isAir(checkDown.subtract(0, 1, 0).getBlock().getType())&&(!(testGravity(checkDown.clone().add(0, 1, 0), itercount+1, 0)))) {
				hasSupport = false;
				break;
			}
		}
		if(hasSupport) {
			isSafe = true;
		}
		if(isSafe) {
			if(isAir(l.clone().subtract(0, 1, 0).getBlock().getType())) {
				isSafe = false;
			}
		}
		for(int i = 0; i < 4; i++) {
			Location checkSide = l.clone();
			int checkLength = 0;
			for(int i2 = 0; i2 < lengthSafety; i2++) {
				if(i==0) {
					Location l2 = checkSide.subtract(0, 0, 1);
					if(isAir(l2.getBlock().getType())) {
						break;
					}
					if((!(isAir(l2.clone().subtract(0, 1, 0).getBlock().getType())))) {
						if(checkDowns(l2.subtract(0, 1, 0), checkLength)) {
							isSafe = true;
							hasSafety = true;
							break;
						}
					}
					else {
						checkLength++;
					}
				}
				if(i==1) {
					Location l2 = checkSide.add(0, 0, 1);
					if(isAir(l2.getBlock().getType())) {
						break;
					}
					if(!isAir(l2.clone().subtract(0, 1, 0).getBlock().getType())) {
						if(checkDowns(l2.subtract(0, 1, 0), checkLength)) {
							isSafe = true;
							hasSafety = true;
							break;
						}
					}
					else {
						checkLength++;
					}
				}
				if(i==2) {
					Location l2 = checkSide.subtract(1, 0, 0);
					if(isAir(l2.getBlock().getType())) {
						break;
					}
					if(!isAir(l2.clone().subtract(0, 1, 0).getBlock().getType())) {
						if(checkDowns(l2.subtract(0, 1, 0), checkLength)) {
							isSafe = true;
							hasSafety = true;
							break;
						}
					}
					else {
						checkLength++;
					}
				}
				if(i==3) {
					Location l2 = checkSide.add(1, 0, 0);
					if(isAir(l2.getBlock().getType())) {
						break;
					}
					if(!isAir(l2.clone().subtract(0, 1, 0).getBlock().getType())) {
						if(checkDowns(l2.subtract(0, 1, 0), checkLength)) {
							isSafe = true;
							hasSafety = true;
							break;
						}
					}
					else {
						checkLength++;
					}
				}
			}
		}
		if(itercount==0&&(hasAir && !hasSafety && !hasSupport && !isSafe)) {
			makeFallUp(l, 4, type);
			return true;
		}
		else {
			return isSafe;
		}
	}

	private void makeFallUp(Location l, int fallsize, int type) {
		if(type == 1) {
			spawnRealFSand(l.getBlock().getLocation().clone().add(0.5, 0, 0.5), l.getBlock().getType());
			l.getBlock().getLocation().getBlock().setType(Material.AIR);
		}
		Location l1 = l.clone();
		for(int lcount = 0; lcount < fallsize; lcount++) {
			Location l1temp = l1.add(0, 1, 0);
			if(!isAir(l1temp.getBlock().getType())) {
					spawnRealFSand(l1temp.getBlock().getLocation().add(0.5, 0, 0.5), l1temp.getBlock().getType());
				l1temp.getBlock().getLocation().getBlock().setType(Material.AIR);
			}
			else {
				break;
			}
		}
		Location l2 = l.clone();
		for(int lcount = 0; lcount < fallsize; lcount++) {
			Location l2temp = l2.subtract(0, 1, 0);
			if(!isAir(l2temp.getBlock().getType())) {
				spawnRealFSand(l2temp.getBlock().getLocation().add(0.5, 0, 0.5), l2temp.getBlock().getType());
				l2temp.getBlock().getLocation().getBlock().setType(Material.AIR);
			}
			else {
				break;
			}
		}
	}

    //

	public boolean isAir(Material m) {
		return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
	}

	public boolean isStony(Material m) {
		return m == Material.STONE || m == Material.MOSSY_COBBLESTONE || m == Material.ANDESITE || m == Material.DIORITE || m == Material.COBBLESTONE || m == Material.GRANITE || m == Material.GRAVEL;
	}

    private void doRandomFireParticle(Location l) {
    	if(random.nextInt(4)!=0) {
    		l.getWorld().spawnParticle(Particle.FLAME, l, 1, .5, .5, .5, 0.02);
    	}
    	else if(random.nextInt(3)!=0) {
    		l.getWorld().spawnParticle(Particle.LAVA, l, 1, .5, .5, .5, 0.02);
    	}
    	else if(random.nextInt(10)==0) {
    		l.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, l, 1, .5, .5, .5, 0.05);
    	}
    	else {
    		l.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l, 1, .5, .5, .5, 0.002);
    	}
    }

	private List<String> getWorlds()
	{
		return Collections.unmodifiableList(worlds);
	}
}
