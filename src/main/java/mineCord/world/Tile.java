package mineCord.world;

import mineCord.Direction;

public final class Tile
{
	private Chunk chunk;
	private int x;
	private int y;
	private TileType type;
	
	Tile(Chunk chunk, int x, int y)
	{
		this.chunk = chunk;
		this.x = x;
		this.y = y;
		type = TileType.AIR;
	}
	
	public int getWorldX()
	{
		
		return x + chunk.getX() * Chunk.chunkWidth;
	}
	
	public int getWorldY()
	{
		return y + chunk.getY() * Chunk.chunkHeight;
	}
	
	public int getChunkX()
	{
		return x;
	}
	
	public int getChunkY()
	{
		return y;
	}
	
	public Chunk getChunk()
	{
		return chunk;
	}
	
	public World getWorld()
	{
		return chunk.getWorld();
	}
	
	public Tile getTile(Direction direction)
	{
		return chunk.getWorld().getTile(getWorldX() + direction.getX(), getWorldY() + direction.getY());
	}
	
	public TileType getType()
	{
		return type;
	}
	
	public void setType(TileType type)
	{
		this.type = type;
	}
}