package mineCord.world;

public enum TileType
{
	AIR			(true,	false,	false,	":large_blue_circle:"),
	STONE		(false,	false,	true,	":white_large_square:"),
	DIRT		(false,	false,	true,	":cookie:"),
	GRASS		(false,	false,	true,	":melon:"),
	WOOD		(false,	false,	true,	":palm_tree:"),
	LEAVES		(false,	false,	true,	":four_leaf_clover:"),
	BEDROCK		(false,	false,	false,	":black_large_square:"),
	LADDER		(true,	true,	true,	":arrow_up:");
	
	private boolean traversable;
	private boolean climbable;
	private boolean breakable;
	private String display;
	
	TileType(boolean traversable, boolean climbable, boolean breakable, String display)
	{
		this.traversable = traversable;
		this.climbable = climbable;
		this.breakable = breakable;
		this.display = display;
	}
	
	public boolean traversable()
	{
		return traversable;
	}
	
	public boolean climbable()
	{
		return climbable;
	}
	
	public boolean breakable()
	{
		return breakable;
	}
	
	public String draw()
	{
		return display;
	}
}