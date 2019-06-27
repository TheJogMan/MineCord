package mineCord.world;

import java.awt.Point;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import mineCord.Main;

public final class Chunk implements Iterable<Tile>
{
	public static final int chunkWidth = 16;
	public static final int chunkHeight = 16;
	
	private World world;
	private Tile[][] tiles;
	private int chunkX;
	private int chunkY;
	boolean keepLoaded;
	
	private File file;
	
	Chunk(World world, int chunkX, int chunkY)
	{
		tiles = new Tile[chunkWidth][chunkHeight];
		this.chunkX = chunkX;
		this.chunkY = chunkY;
		this.world = world;
		keepLoaded = true;
		
		Main.ensureDirectory(new File("Worlds/" + world.getID() + "/" + chunkX + "/" + chunkY + "/entities"));
		file = new File("Worlds/" + world.getID() + "/" + chunkX + "/" + chunkY + "/chunkData");
		if (file.exists()) load();
		else
		{
			for (int x = 0; x < chunkWidth; x++) for (int y = 0; y < chunkHeight; y++) tiles[x][y] = new Tile(this, x, y);
			world.chunkGenerator.generate(this);
		}
	}
	
	public World getWorld()
	{
		return world;
	}
	
	public int getX()
	{
		return chunkX;
	}
	
	public int getY()
	{
		return chunkY;
	}
	
	void save(boolean saveEntities)
	{
		try
		{
			PrintStream writer = new PrintStream(file);
			for (Iterator<Tile> iterator = iterator(); iterator.hasNext();)
			{
				Point point = ((ChunkTileIterator)iterator).currentPosition();
				Tile tile = iterator.next();
				if (tile == null) System.out.println(point + " " + chunkX + " " + chunkY);
				writer.print(tile.getType().name());
				if (iterator.hasNext()) writer.println();;
			}
			writer.close();
			if (saveEntities) for (Iterator<Entity> iterator = getEntities().iterator(); iterator.hasNext();) iterator.next().save();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	void load()
	{
		try
		{
			FileReader reader = new FileReader(file);
			String currentLine = "";
			ChunkTileIterator iterator = new ChunkTileIterator();
			while (reader.ready() && iterator.hasNext())
			{
				int in = reader.read();
				if (in == -1 || (char)in == '\n')
				{
					Point point = iterator.currentPosition();
					Tile tile = new Tile(this, (int)point.getX(), (int)point.getY());
					tile.setType(TileType.valueOf(currentLine));
					currentLine = "";
					tiles[tile.getChunkX()][tile.getChunkY()] = tile;
					iterator.step();
				}
				else if ((char)in != '\r') currentLine += (char)in;
			}
			reader.close();
			if (currentLine.length() > 0 && iterator.hasNext())
			{
				Point point = iterator.currentPosition();
				Tile tile = new Tile(this, (int)point.getX(), (int)point.getY());
				tile.setType(TileType.valueOf(currentLine));
				tiles[tile.getChunkX()][tile.getChunkY()] = tile;
				iterator.step();
			}
			while (iterator.hasNext())
			{
				Point point = iterator.currentPosition();
				Tile tile = new Tile(this, (int)point.getX(), (int)point.getY());
				tiles[tile.getChunkX()][tile.getChunkY()] = tile;
				iterator.step();
				//System.out.println("Failed to load tile: Chunk(" + chunkX + "," + chunkY + ") Tile(" + (int)point.getX() + "," + (int)point.getY() + ")");
			}
			
			File[] files = (new File("Worlds/" + world.getID() + "/" + chunkX + "/" + chunkY + "/entities")).listFiles();
			for (int index = 0; index < files.length; index++)
			{
				Entity entity = Entity.loadEntity(world, files[index]);
				world.addEntity(entity);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public ArrayList<Entity> getEntities()
	{
		ArrayList<Entity> entities = new ArrayList<Entity>();
		for (Iterator<Entry<UUID, Entity>> iterator = world.entities.entrySet().iterator(); iterator.hasNext();)
		{
			Entity entity = iterator.next().getValue();
			if (entity.getChunk().equals(this)) entities.add(entity);
		}
		return entities;
	}
	
	void unloadChunk()
	{
		for (Iterator<Entity> iterator = getEntities().iterator(); iterator.hasNext();) iterator.next().unload(true);
		save(false);
	}
	
	public Tile getTile(int x, int y)
	{
		if (x >= 0 && x < chunkWidth && y >= 0 && y < chunkHeight) return tiles[x][y];
		else return world.getTile(Integer.MIN_VALUE, 0);
	}
	
	@Override
	public Iterator<Tile> iterator()
	{
		return new ChunkTileIterator();
	}
	
	private class ChunkTileIterator implements Iterator<Tile>
	{
		int x = 0;
		int y = 0;
		
		@Override
		public boolean hasNext()
		{
			return y < chunkHeight && x < chunkWidth;
		}
		
		Point currentPosition()
		{
			return new Point(x, y);
		}
		
		void step()
		{
			x++;
			if (x >= chunkWidth)
			{
				x = 0;
				y++;
			}
		}
		
		@Override
		public Tile next()
		{
			Tile tile = tiles[x][y];
			step();
			return tile;
		}
	}
}