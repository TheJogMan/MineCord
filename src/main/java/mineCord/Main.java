package mineCord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.user.UserStatus;

import mineCord.world.World;
import mineCord.world.chunkGenerators.FlatWorld;

public class Main
{
	public static HashMap<Long, World> worlds;
	public static DiscordApi api;
	static World world;
	public static boolean keepRunning = true;
	
	public static void main(String[] args)
	{
		worlds = new HashMap<Long, World>();
		
		String token = "";
		try
		{
			File tokenFile = new File("BotToken.txt");
			if (tokenFile.exists())
			{
				FileReader reader = new FileReader(tokenFile);
				while (reader.ready()) token += (char)reader.read();
				reader.close();
			}
			else
			{
				tokenFile.createNewFile();
				System.out.println("BotToken file was not present, it has been created, please provide a bot token in that file.");
				return;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("Could not retrieve bot token from file!");
			return;
		}
		
		try
		{
			api = new DiscordApiBuilder().setToken(token).login().join();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Could not connect! Make sure that the token provided in the BotToken file is correct.");
			return;
		}
		Permissions permissions = (new PermissionsBuilder()).setAllowed(PermissionType.MANAGE_MESSAGES).setAllowed(PermissionType.READ_MESSAGES).setAllowed(PermissionType.READ_MESSAGE_HISTORY)
				.setAllowed(PermissionType.SEND_MESSAGES).build();
		System.out.println("Connected! Invite to a server with the following link: " + api.createBotInvite(permissions));
		refreshPresence();
		api.updateStatus(UserStatus.ONLINE);
		api.addListener(new MessageListener());
		try
		{
			File worldContainer = new File("Worlds");
			ensureDirectory(worldContainer);
			File[] worldFiles = worldContainer.listFiles();
			for (int index = 0; index < worldFiles.length; index++) getWorld(Long.parseLong(worldFiles[index].getName()));
			while (keepRunning)
			{
				Stack<World> deletedWorlds = new Stack<World>();
				for (Iterator<Entry<Long, World>> iterator = worlds.entrySet().iterator(); iterator.hasNext();)
				{
					World world = iterator.next().getValue();
					world.update(false);
					if (world.deleteWorld)
					{
						deletedWorlds.push(world);
					}
				}
				while (!deletedWorlds.isEmpty())
				{
					World world = deletedWorlds.pop();
					world.unload();
					Main.deleteDirectory(world.file.getParentFile());
					world.getChannel().sendMessage("World deleted!");
				}
				
				try
				{
					Thread.sleep((int)((1.0 / 2.0) * 1000.0));
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (keepRunning) System.out.println("Main loop has died. The bot will now disconnect.");
		else
		{
			System.out.println("Bot has been instructed to disconnect.");
		}
		api.updateStatus(UserStatus.OFFLINE);
		api.disconnect();
	}
	
	public static void refreshPresence()
	{
		api.updateActivity(ActivityType.PLAYING, "MineCord Help");
	}
	
	public static World getWorld(long id)
	{
		if (worlds.containsKey(id)) return worlds.get(id);
		else
		{
			World world = new World(id, new FlatWorld());
			worlds.put(id, world);
			return world;
		}
	}
	
	public static void ensureDirectory(File file)
	{
		if (file != null && !file.exists())
		{
			ensureDirectory(file.getParentFile());
			file.mkdir();
		}
	}
	
	public static void deleteDirectory(File file)
	{
		if (file != null && file.exists())
		{
			if (file.isFile()) file.delete();
			else if (file.isDirectory())
			{
				File[] files = file.listFiles();
				for (int index = 0; index < files.length; index++) deleteDirectory(files[index]);
				file.delete();
			}
		}
	}
}