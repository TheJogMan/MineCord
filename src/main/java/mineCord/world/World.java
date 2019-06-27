package mineCord.world;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;

import mineCord.Main;
import mineCord.entity.Player;

public class World implements Iterable<Chunk>
{
	public static final int columnSize = 16;
	public static final int loadDistance = 2;
	
	private HashMap<Integer, Chunk[]> chunks;
	private HashMap<Integer, Boolean> keepLoaded;
	private Stack<Entity> entityAdditionQueue;
	HashMap<UUID, Entity> entities;
	ChunkGenerator chunkGenerator;
	long id;
	long messageID = 0;
	public File file;
	
	Player player;
	public boolean deleteWorld;
	
	public World(long id, ChunkGenerator chunkGenerator)
	{
		this.chunkGenerator = chunkGenerator;
		chunks = new HashMap<Integer, Chunk[]>();
		keepLoaded = new HashMap<Integer, Boolean>();
		entities = new HashMap<UUID, Entity>();
		entityAdditionQueue = new Stack<Entity>();
		this.id = id;
		Main.ensureDirectory(new File("Worlds/" + id));
		file = new File("Worlds/" + id + "/worldData");
		deleteWorld = false;
		if (file.exists())
		{
			load();
			cleanChannel(false);
		}
		else
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			int y = 0;
			for (y = columnSize * Chunk.chunkHeight - 1; y > 0; y--)
			{
				if (!getTile(0, y).getType().traversable())
				{
					if (y < (columnSize * Chunk.chunkHeight - 1)) y++;
					break;
				}
			}
			
			player = (Player)Entity.createEntity(this, 0, y, EntityType.PLAYER);
			addEntity(player);
			processEntityQueue();
			cleanChannel(true);
			save();
		}
	}
	
	public Player getPlayer()
	{
		return player;
	}
	
	public void update(boolean render)
	{
		if (processEntityQueue()) render = true;
		
		Chunk chunk = player.getChunk();
		int minX = chunk.getX() - loadDistance;
		int maxX = chunk.getX() + loadDistance;
		for (int x = minX; x <= maxX; x++) getChunk(x, 0);
		
		Stack<UUID> entitiesToRemove = new Stack<UUID>();
		for (Iterator<Entry<UUID, Entity>> iterator = entities.entrySet().iterator(); iterator.hasNext();)
		{
			Entity entity = iterator.next().getValue();
			int x = entity.getX();
			int y = entity.getY();
			entity.entityUpdate();
			if (entity.getX() != x || entity.getY() != y) render = true;
			if (entity.remove)
			{
				entitiesToRemove.push(entity.getUUID());
				render = true;
			}
		}
		while (!entitiesToRemove.isEmpty()) entities.remove(entitiesToRemove.pop());
		
		save();
		if (render) render(false);
	}
	
	void purgeChunks()
	{
		Chunk chunk = player.getChunk();
		int minX = chunk.getX() - loadDistance;
		int maxX = chunk.getX() + loadDistance;
		ArrayList<Integer> columnsToUnload = new ArrayList<Integer>();
		for (Iterator<Integer> iterator = chunks.keySet().iterator(); iterator.hasNext();)
		{
			int x = iterator.next();
			if (x < minX || x > maxX || !keepLoaded.get(x)) columnsToUnload.add(x);
		}
		for (Iterator<Integer> iterator = columnsToUnload.iterator(); iterator.hasNext();) unloadColumn(iterator.next());
	}
	
	boolean processEntityQueue()
	{
		boolean entitiesAdded = false;
		while (!entityAdditionQueue.isEmpty())
		{
			Entity entity = entityAdditionQueue.pop();
			entities.put(entity.getUUID(), entity);
			entitiesAdded = true;
		}
		return entitiesAdded;
	}
	
	void save()
	{
		processEntityQueue();
		try
		{
			PrintStream writer = new PrintStream(file);
			Chunk chunk = player.getChunk();
			writer.println(player.getUUID().toString());
			writer.println(chunk.getX());
			writer.println(chunk.getY());
			writer.println(messageID);
			writer.close();
			for (Iterator<Chunk> iterator = iterator(); iterator.hasNext();) iterator.next().save(true);
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
			int chunkX = Integer.parseInt(lines.get(1));
			int chunkY = Integer.parseInt(lines.get(2));
			messageID = Long.parseLong(lines.get(3));
			getChunk(chunkX, chunkY);
			processEntityQueue();
			player = (Player)getEntity(UUID.fromString(lines.get(0)));
			render(false);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void unload()
	{
		int playerX = player.getChunk().getX();
		for (Iterator<Integer> iterator = chunks.keySet().iterator(); iterator.hasNext();)
		{
			int x = iterator.next();
			if (x == playerX)
			{
				Chunk[] column = chunks.get(x);
				for (int y = 0; y < columnSize; y++) column[y].save(true);
			}
			else keepLoaded.put(x, false);
		}
		purgeChunks();
		Main.worlds.remove(id);
	}
	
	public void unloadColumn(int x)
	{
		if (player.getChunk().getX() == x) return; //can't unload the chunk the player is in, doing so would break everything and would not be pretty
		
		if (chunks.containsKey(x))
		{
			Chunk[] column = chunks.get(x);
			for (int y = 0; y < columnSize; y++) column[y].unloadChunk();
			chunks.remove(x);
			keepLoaded.remove(x);
		}
	}
	
	public Entity getEntity(UUID id)
	{
		return entities.get(id);
	}
	
	public Entity getEntity(int x, int y)
	{
		for (Iterator<Entry<UUID, Entity>> iterator = entities.entrySet().iterator(); iterator.hasNext();)
		{
			Entity entity = iterator.next().getValue();
			if (entity.getX() == x && entity.getY() == y) return entity;
		}
		return null;
	}
	
	void addEntity(Entity entity)
	{
		entityAdditionQueue.push(entity);
	}
	
	public long getID()
	{
		return id;
	}
	
	public Tile getTile(int x, int y)
	{
		Chunk chunk = getChunkFromTilePosition(x, y);
		int tileX = x - chunk.getX() * Chunk.chunkWidth;
		int tileY = y - chunk.getY() * Chunk.chunkHeight;
		if (tileX < 0) tileX += Chunk.chunkWidth;
		if (tileY < 0) tileY += Chunk.chunkHeight;
		return chunk.getTile(tileX, tileY);
	}
	
	public Chunk getChunkFromTilePosition(int x, int y)
	{
		int chunkX = x / Chunk.chunkWidth;
		int chunkY = y / Chunk.chunkHeight;
		if (x < 0) chunkX--;
		if (y < 0) chunkY--;
		return getChunk(chunkX, chunkY);
	}
	
	public Chunk getChunk(int chunkX, int chunkY)
	{
		if (chunkY >= 0 && chunkY < columnSize)
		{
			if (!chunks.containsKey(chunkX))
			{
				Chunk[] column = new Chunk[columnSize];
				for (int y = 0; y < columnSize; y++) column[y] = new Chunk(this, chunkX, y);
				chunks.put(chunkX, column);
				keepLoaded.put(chunkX, true);
			}
			return chunks.get(chunkX)[chunkY];
		}
		return getChunk(Integer.MIN_VALUE, 0);
	}
	
	@Override
	public Iterator<Chunk> iterator()
	{
		return new ChunkIterator();
	}
	
	private class ChunkIterator implements Iterator<Chunk>
	{
		Iterator<Integer> columnIterator = chunks.keySet().iterator();
		int x = 0;
		int y = 0;
		boolean hasAny = false;
		boolean endReached = false;
		
		ChunkIterator()
		{
			if (columnIterator.hasNext())
			{
				x = columnIterator.next();
				hasAny = true;
			}
		}
		
		@Override
		public boolean hasNext()
		{
			return !endReached && hasAny && y < columnSize;
		}
		
		@Override
		public Chunk next()
		{
			Chunk chunk = chunks.get(x)[y];
			y++;
			if (y >= columnSize)
			{
				if (columnIterator.hasNext())
				{
					y = 0;
					x = columnIterator.next();
				}
				else endReached = true;
			}
			return chunk;
		}
	}
	
	public void render(boolean newMessage)
	{
		String message = "Player Position (X, Y): " + player.getX() + ", " + player.getY() + "\n";
		
		int horizontalRange = 4;
		int verticalRange = 4;
		
		for (int y = player.getY() + verticalRange; y >= player.getY() - verticalRange; y--)
		{
			for (int x = player.getX() - horizontalRange; x <= player.getX() + horizontalRange; x++)
			{
				Tile tile = getTile(x, y);
				Entity entity = getEntity(x, y);
				if (entity != null) message += entity.draw();
				else message += tile.getType().draw();
			}
			message += "\n";
		}
		
		TextChannel channel = getChannel();
		if (newMessage || messageID == 0)
		{
			try
			{
				if (messageID != 0) channel.getMessageById(messageID).get().delete();
				messageID = channel.sendMessage(message).get().getId();
			}
			catch (InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				CompletableFuture<Message> mes = channel.getMessageById(messageID);
				Message me = null;
				try
				{
					me = mes.get();
				}
				catch (Exception e)
				{
					messageID = channel.sendMessage(message).get().getId();
				}
				if (me != null) me.edit(message);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public TextChannel getChannel()
	{
		return Main.api.getTextChannelById(id).get();
	}
	
	void cleanChannel(boolean total)
	{
		TextChannel channel = getChannel();
		Stream<Message> messages = channel.getMessagesAsStream();
		for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext();)
		{
			Message message = iterator.next();
			if (messageID != message.getId() || total) message.delete();
		}
		render(true);
	}
}