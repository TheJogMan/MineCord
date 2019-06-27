package mineCord.world.chunkGenerators;

import java.util.Iterator;

import mineCord.world.Chunk;
import mineCord.world.ChunkGenerator;
import mineCord.world.Tile;
import mineCord.world.TileType;

public class FlatWorld extends ChunkGenerator
{
	@Override
	protected void generate(Chunk chunk)
	{
		//a more interesting generator could use a noise function such as perlin noise, with the world ID as the seed, this is just something quick and easy to provide a testing world
		for (Iterator<Tile> iterator = chunk.iterator(); iterator.hasNext();)
		{
			Tile tile = iterator.next();
			int y = tile.getWorldY();
			if (y == 0) tile.setType(TileType.BEDROCK);
			else if (y == 59) tile.setType(TileType.GRASS);
			else if (y == 58 || y == 57) tile.setType(TileType.DIRT);
			else if (y < 57) tile.setType(TileType.STONE);
		}
	}
}