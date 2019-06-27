package mineCord;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import mineCord.world.Tile;
import mineCord.world.TileType;
import mineCord.world.World;

public class MessageListener implements MessageCreateListener
{
	@Override
	public void onMessageCreate(MessageCreateEvent event)
	{
		if (event.getMessageAuthor().isRegularUser() && !event.getMessageAuthor().isWebhook())
		{
			TextChannel channel = event.getChannel();
			if (event.getMessage().getContent().toLowerCase().compareTo("minecord help") == 0)
			{
				User user = event.getMessageAuthor().asUser().get();
				TextChannel response;
				try
				{
					response = user.getPrivateChannel().orElse(user.openPrivateChannel().get());
					response.sendMessage("Commands:"
							+ "\n``Create MineCord World`` - Turns the channel into a MineCord world."
							+ "\n``Delete MineCord World`` - Deletes the MineCord world from the channel."
							+ "\n``MineCord Help`` - Sends this message to you."
							+ "\n``Move <direction>`` - Moves the player. ``Move L``"
							+ "\n``Place <material> <offset>`` - Places a material at an offset from the player location. ``Place Dirt L2U1``"
							+ "\n``Break <offset>`` - Breaks a block (replaces it with air) at an offset from the player location. ``Break L2U1``"
							+ "\n``Tp <x> <y>`` - Teleports the player to the given coordinates."
							+ "\n\nDirections:"
							+ "\n``L`` - Left\n``R`` - Right\n``U`` - Up\n``D`` - Down"
							+ "\n\nMaterials:"
							+ "\nAir, Stone, Dirt, Grass, Wood, Leaves, Bedrock, Ladder");
					if (user.isBotOwner())
					{
						response.sendMessage("As the bot owner, you can also use the command ``MineCord Shutdown`` to instruct the bot to unload all worlds and shutdown."
								+ "\nWorld corruption can occur if the bot is shutdown through other means.");
					}
				}
				catch (InterruptedException | ExecutionException e)
				{
					e.printStackTrace();
				}
			}
			else if (event.getMessage().getContent().toLowerCase().compareTo("minecord shutdown") == 0)
			{
				User user = event.getMessageAuthor().asUser().get();
				if (user.isBotOwner())
				{
					Main.keepRunning = false;
				}
			}
			
			if (channel instanceof PrivateChannel)
			{
				channel.sendMessage("You can only play MineCord in a server channel!");
			}
			else
			{
				File file = new File("Worlds/" + channel.getId());
				if (file.exists())
				{
					File dataFile = new File("Worlds/" + channel.getId() + "/worldData");
					if (dataFile.exists() && event.getMessageContent().compareTo("Delete MineCord World") == 0 && event.getMessageAuthor().isServerAdmin())
					{
						World world = Main.getWorld(channel.getId());
						world.deleteWorld = true;
					}
					else if (dataFile.exists() || (event.getMessageContent().compareTo("Create MineCord World") == 0 && event.getMessageAuthor().isServerAdmin()))
					{
						messageInWorldChannel(event.getMessageContent(), Main.getWorld(channel.getId()));
						event.getMessage().delete();
					}
				}
				else if (event.getMessageContent().compareTo("Create MineCord World") == 0 && event.getMessageAuthor().isServerAdmin())
				{
					file.mkdirs();
					channel.sendMessage("Are you sure you want to do this? Turning this channel into a MineCord world will delete all messages in this channel which can not be undone, run the command again to confirm.");
				}
			}
		}
	}
	
	void messageInWorldChannel(String message, World world)
	{
		ArrayList<String> args = new ArrayList<String>();
		String currentArg = "";
		for (int index = 0; index < message.length(); index++)
		{
			char ch = message.charAt(index);
			ch = Character.toLowerCase(ch);
			if (ch == ' ')
			{
				if (currentArg.length() > 0)
				{
					args.add(currentArg);
					currentArg = "";
				}
			}
			else
			{
				currentArg += ch;
			}
		}
		if (currentArg.length() > 0) args.add(currentArg);
		if (args.size() > 0)
		{
			if (args.get(0).compareTo("move") == 0 && args.size() == 2)
			{
				if (args.get(1).compareTo("u") == 0) world.getPlayer().move(Direction.UP);
				else if (args.get(1).compareTo("d") == 0) world.getPlayer().move(Direction.DOWN);
				else if (args.get(1).compareTo("l") == 0) world.getPlayer().move(Direction.LEFT);
				else if (args.get(1).compareTo("r") == 0) world.getPlayer().move(Direction.RIGHT);
				world.render(false);
			}
			else if (args.get(0).compareTo("place") == 0 && args.size() == 3)
			{
				Tile tile = getTile(args.get(2), world);
				try
				{
					TileType type = TileType.valueOf(args.get(1).toUpperCase());
					tile.setType(type);
					world.render(false);
				}
				catch (Exception e)
				{
					
				}
			}
			else if (args.get(0).compareTo("break") == 0 && args.size() == 2)
			{
				Tile tile = getTile(args.get(1), world);
				tile.setType(TileType.AIR);
				world.render(false);
			}
			else if (args.get(0).compareTo("tp") == 0 && args.size() == 3)
			{
				try
				{
					world.getPlayer().tp(Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2)));
					world.render(false);
				}
				catch (Exception e)
				{
					
				}
			}
		}
	}
	
	Tile getTile(String offset, World world)
	{
		int x = world.getPlayer().getX();
		int y = world.getPlayer().getY();
		
		int index = 0;
		while (index < offset.length())
		{
			char ch = offset.charAt(index);
			Direction direction = null;
			if (ch == 'u') direction = Direction.UP;
			else if (ch == 'd') direction = Direction.DOWN;
			else if (ch == 'l') direction = Direction.LEFT;
			else if (ch == 'r') direction = Direction.RIGHT;
			if (direction != null)
			{
				index++;
				int multiplier = 0;
				if (index < offset.length() && isNum(offset.charAt(index)))
				{
					String multiplierNum = "";
					while (index < offset.length() && isNum(offset.charAt(index)))
					{
						multiplierNum += offset.charAt(index);
						index++;
					}
					multiplier = Integer.parseInt(multiplierNum);
				}
				x += direction.x * multiplier;
				y += direction.y * multiplier;
			}
		}
		
		return world.getTile(x, y);
	}
	
	boolean isNum(char ch)
	{
		 return ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7' || ch == '8' || ch == '9';
	}
}