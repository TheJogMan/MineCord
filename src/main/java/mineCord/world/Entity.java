package mineCord.world;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.UUID;

import mineCord.Direction;

public abstract class Entity
{
	protected abstract void init(World world, int x, int y);
	protected abstract void update();
	protected abstract String draw();
	protected abstract String getData();
	protected abstract void loadData(String string);
	
	private World world;
	private int x;
	private int y;
	private UUID id;
	private EntityType type;
	boolean remove;
	
	File file;
	
	private final void initEntity(World world, int x, int y, EntityType type, boolean getChunk)
	{
		this.world = world;
		this.x = x;
		this.y = y;
		this.type = type;
		remove = false;
		id = UUID.randomUUID();
		file = null;
		if (getChunk) getChunk();
		init(world, x, y);
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public static Entity createEntity(World world, int x, int y, EntityType type)
	{
		try
		{
			Entity entity = type.entityClass.newInstance();
			entity.initEntity(world, x, y, type, true);
			return entity;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static Entity loadEntity(World world, File file)
	{
		try
		{
			FileReader reader = new FileReader(file);
			ArrayList<String> lines = new ArrayList<String>();
			String currentLine = "";
			while (reader.ready())
			{
				char ch = (char)reader.read();
				if (ch == '\n')
				{
					lines.add(currentLine);
					currentLine = "";
				}
				else if (ch != '\r') currentLine += ch;
			}
			lines.add(currentLine);
			reader.close();
			int x = Integer.parseInt(lines.get(0));
			int y = Integer.parseInt(lines.get(1));
			EntityType type = EntityType.valueOf(lines.get(2));
			String data = exportData(lines.get(3));
			
			Entity entity = type.entityClass.newInstance();
			entity.initEntity(world, x, y, type, false);
			entity.id = UUID.fromString(file.getName());
			entity.file = file;
			entity.loadData(data);
			return entity;
		}
		catch (IOException | InstantiationException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	void save()
	{
		if (file != null) file.delete();
		Chunk chunk = getChunk();
		File file = new File("Worlds/" + world.getID() + "/" + chunk.getX() + "/" + chunk.getY() + "/entities/" + id.toString());
		try
		{
			PrintStream writer = new PrintStream(file);
			writer.println(x);
			writer.println(y);
			writer.println(type.name());
			writer.println(importData(getData()));
			writer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	void unload(boolean save)
	{
		if (save) save();
		else if (file != null) file.delete();
		remove = true;
	}
	
	void entityUpdate()
	{
		if (getTile(Direction.DOWN).getType().traversable() && !getTile(Direction.NONE).getType().climbable()) move(Direction.DOWN);
		update();
	}
	
	public final EntityType getType()
	{
		return type;
	}
	
	public final World getWorld()
	{
		return world;
	}
	
	public final Chunk getChunk()
	{
		return world.getChunkFromTilePosition(x, y);
	}
	
	public final UUID getUUID()
	{
		return id;
	}
	
	public final Tile getTile(Direction direction)
	{
		return world.getTile(x + direction.getX(), y + direction.getY());
	}
	
	public final void tp(int x, int y)
	{
		Tile destination = world.getTile(x, y);
		if (destination.getType().traversable())
		{
			this.x = x;
			this.y = y;
		}
	}
	
	public final void move(Direction direction)
	{
		Tile destination = getTile(direction);
		if (destination.getType().traversable())
		{
			x = destination.getWorldX();
			y = destination.getWorldY();
		}
		else if (destination.getTile(Direction.UP).getType().traversable())
		{
			x = destination.getWorldX() + Direction.UP.getX();
			y = destination.getWorldY() + Direction.UP.getY();
		}
	}
	
	static String importData(String raw)
	{
		String newString = "";
		int index = 0;
		while (index < raw.length())
		{
			char ch = raw.charAt(index);
			if (ch == '\n')
			{
				newString += "#0";
				index++;
			}
			else if (ch == '#')
			{
				newString += "#1";
				index++;
			}
			else
			{
				newString += ch;
				index++;
			}
		}
		return newString;
	}
	
	static String exportData(String sanatized)
	{
		String newString = "";
		int index = 0;
		while (index < sanatized.length())
		{
			char ch = sanatized.charAt(index);
			if (ch == '#')
			{
				char id = sanatized.charAt(index + 1);
				if (id == '0') newString += '\n';
				else if (id == '1') newString += '#';
				index += 2;
			}
			else
			{
				newString += ch;
				index++;
			}
		}
		return newString;
	}
}