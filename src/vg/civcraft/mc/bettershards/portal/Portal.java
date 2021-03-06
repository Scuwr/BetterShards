package vg.civcraft.mc.bettershards.portal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.civmodcore.locations.QTBox;

public class Portal implements QTBox, Comparable<Portal> {

	protected Location corner; // This should be the location of the first block
								// identified
	protected int xrange, yrange, zrange;
	protected Portal connection;
	protected String serverName;
	protected String name;
	protected PortalType type;
	private boolean isOnCurrentServer; // Set to false if not on current server
	protected DatabaseManager db;
	private boolean isDirty = false;

	public Portal(String name, Location corner, int xrange, int yrange,
			int zrange, Portal connection, PortalType type,
			boolean isOnCurrentServer) {
		this.corner = corner;
		this.xrange = xrange;
		this.yrange = yrange;
		this.zrange = zrange;
		this.connection = connection;
		this.name = name;
		this.type = type;
		this.isOnCurrentServer = isOnCurrentServer;
		db = BetterShardsPlugin.getInstance().getDatabaseManager();
	}

	public Portal getPartnerPortal() {
		return connection;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		setDirty(true);
	}

	public void setPartnerPortal(Portal connection) {
		this.connection = connection;
		setDirty(true);
	}

	public PortalType getType() {
		return type;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
		setDirty(true);
	}

	@Override
	public int qtXMax() {
		if (xrange > 0)
			return corner.getBlockX() + xrange;
		return corner.getBlockX();
	}

	@Override
	public int qtXMid() {
		return (qtXMax() + qtXMin()) / 2;
	}

	@Override
	public int qtXMin() {
		if (xrange < 0)
			return corner.getBlockX() + xrange;
		return corner.getBlockX();
	}

	@Override
	public int qtZMax() {
		if (zrange > 0)
			return corner.getBlockZ() + zrange;
		return corner.getBlockZ();
	}

	@Override
	public int qtZMid() {
		return (qtZMax() + qtZMin()) / 2;
	}

	@Override
	public int qtZMin() {
		if (zrange < 0)
			return corner.getBlockZ() + zrange;
		return corner.getBlockZ();
	}

	public boolean isValidY(int y) {
		if (yrange < 0)
			return corner.getBlockY() + yrange <= y && corner.getBlockY() >= y;
		return corner.getBlockY() + yrange >= y && corner.getBlockY() <= y;
	}

	// *------------------------------------------------------------------------------------------------------------*
	// | The following chooseSpawn method contains code made by NuclearW |
	// | based on his SpawnArea plugin: |
	// |
	// http://forums.bukkit.org/threads/tp-spawnarea-v0-1-spawns-targetPlayers-in-a-set-area-randomly-1060.20408/
	// |
	// *------------------------------------------------------------------------------------------------------------*
	public Location findRandomSafeLocation() {
		double xrand = 0;
		double zrand = 0;
		int tries = 0;
		double y = -1;
		do {

			xrand = qtXMin() + Math.random() * Math.abs(qtXMax() - qtXMin());
			zrand = qtZMin() + Math.random() * Math.abs(qtZMax() - qtZMin());
			y = getValidHighestY(corner.getWorld(), xrand, zrand);
			tries++;

		} while (y == -1 && tries < 100);
		
		if (y == -1) {
			//still not found
			for(y = 255; y>0 ; y--) {
				if (validSpawn(corner.getWorld(), xrand, y, zrand)) {
					break;
				}
			}
		} 
		if (y == 0) {
			y = 255;
		}
		Location location = new Location(corner.getWorld(), xrand, y, zrand);

		return location;
	}

	@SuppressWarnings("deprecation")
	private double getValidHighestY(World world, double x, double z) {
		world.getChunkAt(new Location(world, x, 0, z)).load();
		double y = corner.getBlockY()
				+ Math.abs(Math.random() * (yrange));
		y = (double) ((int)y);  //round it
		return validSpawn(world,x,y,z) ? y : -1;
	}
	
	private boolean validSpawn(World world, double x, double y, double z) {
		return world.getBlockAt(new Location(world, x, y-1, z)).getType().isSolid() 
				&& world.getBlockAt(new Location(world, x, y, z)).getType() == Material.AIR
				&& world.getBlockAt(new Location(world, x, y+1, z)).getType() == Material.AIR;
	}

	public int getXRange() {
		return xrange;
	}

	public int getYRange() {
		return yrange;
	}

	public int getZRange() {
		return zrange;
	}

	public Location getCornerBlockLocation() {
		return corner;
	}

	public boolean isOnCurrentServer() {
		return isOnCurrentServer;
	}

	public void teleportPlayer(Player p) {
		if (connection == null)
			return;
		if (connection.getServerName().equals(BetterShardsAPI.getServerName()))
			p.teleport(connection.findRandomSafeLocation());
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int compareTo(Portal o) {
		if (isOnCurrentServer && !o.isOnCurrentServer())
			return -1;
		int x = corner.getBlockX(), y = corner.getBlockY(), z = corner
				.getBlockZ();
		int ox = o.getCornerBlockLocation().getBlockX(), oy = o
				.getCornerBlockLocation().getBlockY(), oz = o
				.getCornerBlockLocation().getBlockZ();
		if (x < ox) {
			return -1;
		}
		if (x > ox) {
			return 1;
		}
		if (z < oz) {
			return -1;
		}
		if (z > oz) {
			return 1;
		}
		if (y < oy) {
			return -1;
		}
		if (y > oy) {
			return 1;
		}
		return 0; // equal
	}
	
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}
}