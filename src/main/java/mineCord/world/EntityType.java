package mineCord.world;

import mineCord.entity.Player;

public enum EntityType
{
	PLAYER(Player.class);
	
	Class<? extends Entity> entityClass;
	
	EntityType(Class<? extends Entity> entityClass)
	{
		this.entityClass = entityClass;
	}
}