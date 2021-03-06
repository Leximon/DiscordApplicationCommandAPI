package de.lexoland.api.discord.applicationcommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lexoland.api.discord.applicationcommand.ApplicationCommand.ApplicationCommandChoice;
import de.lexoland.api.discord.applicationcommand.ApplicationCommand.ApplicationCommandNode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.handle.SocketHandler;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.Route.CompiledRoute;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ApplicationCommandAPI {
	
	private final JDAImpl jda;
	private final List<ApplicationCommand> allCommands = new ArrayList<>();
	private final List<ApplicationCommand> globalCommands = new ArrayList<>();
	private final HashMap<Long, List<ApplicationCommand>> guildCommands = new HashMap<>();
	
	public ApplicationCommandAPI(JDA jda) {
		this.jda = (JDAImpl) jda;
		jda.addEventListener(new CommandListener(this));
	}
	
	public <T extends ApplicationCommand> RestAction<T> registerGuildCommand(Guild guild, T command) {
		return registerGuildCommand(guild.getIdLong(), command);
	}
	
	public <T extends ApplicationCommand> RestAction<T> registerGuildCommand(long guildId, T command) {
		ApplicationCommand.ApplicationRootCommandNode node = new ApplicationCommand.ApplicationRootCommandNode(command.getName());
		command.build(node, jda.getGuildById(guildId));
		return new RestActionImpl<>(
				jda,
				Route.post("applications/{}/guilds/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId)),
				RequestBody.create(MediaType.get("application/json"), build(jda.getGuildById(guildId), node).toJson()),
				(response, request) -> {
					DataObject obj = response.optObject().get();
					command.id = obj.getLong("id");
					command.applicationId = obj.getLong("application_id");
					command.node = node;

					List<ApplicationCommand> commands = guildCommands.getOrDefault(guildId, new ArrayList<>());
					commands.add(command);
					guildCommands.put(guildId, commands);
					allCommands.add(command);

					command.updatePermissions(this, jda.getGuildById(guildId)).queue();
					return command;
				}
		);
	}

	public RestAction<Collection<ApplicationCommand>> overwriteGuildCommands(long guildId, ApplicationCommand... commands) {
		Guild guild = jda.getGuildById(guildId);
		ApplicationCommand.ApplicationRootCommandNode[] nodes = new ApplicationCommand.ApplicationRootCommandNode[commands.length];
		for(int i = 0; i < commands.length; i++) {
			var node = new ApplicationCommand.ApplicationRootCommandNode(commands[i].getName());
			commands[i].build(node, guild);
			nodes[i] = node;
		}
		DataArray array = DataArray.empty();
		for(ApplicationCommand.ApplicationRootCommandNode node : nodes) {
			array.add(build(guild, node));
		}
		return new RestActionImpl<>(
				jda,
				Route.put("/applications/{}/guilds/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId)),
				RequestBody.create(MediaType.get("application/json"), array.toJson()),
				(response, request) -> {
					DataArray array1 = response.optArray().get();
					for(int i = 0; i < array1.length(); i++) {
						DataObject obj = array1.getObject(i);
						commands[i].id = obj.getLong("id");
						commands[i].applicationId = obj.getLong("application_id");
						commands[i].node = nodes[i];

						List<ApplicationCommand> commands2 = guildCommands.getOrDefault(guildId, new ArrayList<>());
						commands2.add(commands[i]);
						guildCommands.put(guildId, commands2);

						allCommands.add(commands[i]);
					}
					return Arrays.asList(commands);
				}
		);
	}
	
	public <T extends ApplicationCommand> RestAction<T> registerGlobalCommand(T command) {
		ApplicationCommand.ApplicationRootCommandNode node = new ApplicationCommand.ApplicationRootCommandNode(command.getName());
		command.build(node, null);
		return new RestActionImpl<>(
				jda,
				Route.post("applications/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong())),
				RequestBody.create(MediaType.get("application/json"), build(null, node).toJson()),
				(response, request) -> {
					DataObject obj = response.optObject().get();
					command.id = obj.getLong("id");
					command.applicationId = obj.getLong("application_id");
					command.node = node;
					globalCommands.add(command);
					allCommands.add(command);
					return command;
				}
		);
	}

	@Deprecated
	public RestAction<ApplicationCommand> retrieveGlobalCommand(String commandName) {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong())),
			(response, request) -> {
				DataArray array = response.getArray();
				for(int i = 0; i < array.length(); i++) {
					DataObject command = array.getObject(i);
					if(command.getString("name").equalsIgnoreCase(commandName))
						return commandFromDataObject(command);
				}
				return null;
			}
		);
	}

	@Deprecated
	public RestAction<ApplicationCommand> retrieveGlobalCommand(long commandId) {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/commands/{}").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(commandId)),
			(response, request) -> commandFromDataObject(response.getObject())
		);
	}

	public ApplicationCommand getGlobalCommand(long commandId) {
		for(ApplicationCommand command : getGlobalCommands())
			if(command.id == commandId)
				return command;
		return null;
	}

	public ApplicationCommand getGlobalCommand(String commandName) {
		for(ApplicationCommand command : getGlobalCommands())
			if(command.getName().equalsIgnoreCase(commandName))
				return command;
		return null;
	}
	
	public RestAction<Void> deleteGlobalCommand(long commandId) {
		return new RestActionImpl<>(
			jda,
			Route.delete("applications/{}/commands/{}").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(commandId))
		);
	}

	@Deprecated
	public RestAction<List<ApplicationCommand>> retrieveGlobalCommands() {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong())),
			(response, request) -> {
				List<ApplicationCommand> commands1 = new ArrayList<>();
				DataArray array = response.getArray();
				for(int i = 0; i < array.length(); i++) {
					DataObject command = array.getObject(i);
					commands1.add(commandFromDataObject(command));
				}
				return commands1;
			}
		);
	}

	public List<ApplicationCommand> getGlobalCommands() {
		return globalCommands;
	}

	@Deprecated
	public RestAction<ApplicationCommand> retrieveGuildCommand(long guildId, String commandName) {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/guilds/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId)),
			(response, request) -> {
				DataArray array = response.getArray();
				for(int i = 0; i < array.length(); i++) {
					DataObject command = array.getObject(i);
					if(command.getString("name").equalsIgnoreCase(commandName))
						return commandFromDataObject(command);
				}
				return null;
			}
		);
	}

	@Deprecated
	public RestAction<ApplicationCommand> retrieveGuildCommand(long guildId, long commandId) {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/guilds/{}/commands/{}").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId), String.valueOf(commandId)),
			(response, request) -> commandFromDataObject(response.getObject())
		);
	}

	public ApplicationCommand getGuildCommand(long guildId, long commandId) {
		for(ApplicationCommand command : getGuildCommands(guildId)) {
			if (command.id == commandId)
				return command;
		}
		return null;
	}

	public ApplicationCommand getGuildCommand(long guildId, String commandName) {
		for(ApplicationCommand command : getGuildCommands(guildId))
			if(command.getName().equalsIgnoreCase(commandName))
				return command;
		return null;
	}
	
	public RestAction<Void> deleteGuildCommand(long guildId, long commandId) {
		return new RestActionImpl<>(
			jda,
			Route.delete("applications/{}/guilds/{}/commands/{}").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId), String.valueOf(commandId))
		);
	}

	@Deprecated
	public RestAction<List<ApplicationCommand>> retrieveGuildCommands(long guildId) {
		return new RestActionImpl<>(
			jda,
			Route.get("applications/{}/guilds/{}/commands").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId)),
			(response, request) -> {
				List<ApplicationCommand> commands1 = new ArrayList<>();
				DataArray array = response.getArray();
				for(int i = 0; i < array.length(); i++) {
					DataObject command = array.getObject(i);
					commands1.add(commandFromDataObject(command));
				}
				return commands1;
			}
		);
	}

	public List<ApplicationCommand> getGuildCommands(long guildId) {
		return guildCommands.getOrDefault(guildId, new ArrayList<>());
	}

	public RestAction<Void> editCommandPermissions(Guild guild, long commandId, ApplicationCommandPermission... permissions) {
		return editCommandPermissions(guild.getIdLong(), commandId, permissions);
	}

	public RestAction<Void> editCommandPermissions(long guildId, ApplicationCommand command, ApplicationCommandPermission... permissions) {
		return editCommandPermissions(guildId, command.getId(), permissions);
	}

	public RestAction<Void> editCommandPermissions(Guild guild, ApplicationCommand command, ApplicationCommandPermission... permissions) {
		return editCommandPermissions(guild.getIdLong(), command.getId(), permissions);
	}

	public RestAction<Void> editCommandPermissions(long guildId, long commandId, ApplicationCommandPermission... permissions) {
		DataArray dataArray = DataArray.empty();
		for(ApplicationCommandPermission permission : permissions)
			dataArray.add(permission.getJSON());
		DataObject dataObject = DataObject.empty();
		dataObject.put("permissions", dataArray);
		return new RestActionImpl<>(
				jda,
				Route.put("applications/{}/guilds/{}/commands/{}/permissions").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId), String.valueOf(commandId)),
				RequestBody.create(MediaType.get("application/json"), dataObject.toJson()),
				(response, request) -> null
		);
	}

	public RestAction<Void> batchEditCommandPermissions(long guildId, HashMap<ApplicationCommand, List<ApplicationCommandPermission>> permissions) {
		DataArray array = DataArray.empty();
		for(Map.Entry<ApplicationCommand, List<ApplicationCommandPermission>> entry : permissions.entrySet()) {
			DataArray dataArray = DataArray.empty();
			for(ApplicationCommandPermission permission : entry.getValue())
				dataArray.add(permission.getJSON());
			DataObject dataObject = DataObject.empty();
			dataObject.put("id", entry.getKey().getId());
			dataObject.put("permissions", dataArray);
			array.add(dataObject);
		}
		return new RestActionImpl<>(
				jda,
				Route.put("applications/{}/guilds/{}/commands/permissions").compile(String.valueOf(jda.getSelfUser().getIdLong()), String.valueOf(guildId)),
				RequestBody.create(MediaType.get("application/json"), array.toJson()),
				(response, request) -> null
		);
	}

	public List<ApplicationCommand> getAllCommands() {
		return allCommands;
	}

	private ApplicationCommand commandFromDataObject(DataObject commandData) {
		String name = commandData.getString("name");
		ApplicationCommand command = new ApplicationCommand() {
			@Override
			public void build(ApplicationRootCommandNode root, Guild guild) { }

			@Override
			public String getName() {
				return name;
			}
		};
		command.id = commandData.getLong("id");
		command.applicationId = commandData.getLong("application_id");
		command.node = commandNodeFromDataObject(commandData);
		return command;
	}
	
	private ApplicationCommandNode commandNodeFromDataObject(DataObject nodeData) {
		ApplicationCommandNode node = new ApplicationCommandNode(
			nodeData.getString("name"),
			nodeData.getInt("type", 0)
		);
		node.description = nodeData.getString("description");
		node.required = nodeData.getBoolean("required", false);
		if(nodeData.hasKey("choices")) {
			DataArray choicesData = nodeData.getArray("choices");
			ApplicationCommandChoice[] choices = new ApplicationCommandChoice[choicesData.length()];
			for(int i = 0; i < choicesData.length(); i++) {
				DataObject choiceData = choicesData.getObject(i);
				choices[i] = new ApplicationCommandChoice(choiceData.getString("name"), choiceData.get("value"));
			}
			node.choiceProvider = (choices1, g) -> Collections.addAll(choices1, choices);
		}
		if(nodeData.hasKey("options")) {
			DataArray optionsData = nodeData.getArray("options");
			for(int i = 0; i < optionsData.length(); i++) {
				node.options.add(commandNodeFromDataObject(optionsData.getObject(i)));
			}
		}
		return node;
	}
	
	private DataObject build(Guild g, ApplicationCommandNode node) {
		DataObject obj = DataObject.empty()
			.put("name", node.name)
			.put("description", node.description);
		if(node instanceof ApplicationCommand.ApplicationRootCommandNode)
			obj.put("default_permission", node.defaultPermission);
		if(node.options.size() > 0) {
			
			DataArray optionArray = DataArray.empty();
			for(ApplicationCommandNode option : node.options)
				optionArray.add(buildArgument(g, option));
			obj.put("options", optionArray);
		}
			
		return obj;
	}
	
	private DataObject buildArgument(Guild g, ApplicationCommandNode node) {
		DataObject obj = DataObject.empty();
		obj.put("name", node.name);
		obj.put("description", node.description);
		obj.put("type", node.type);
		obj.put("required", node.required);
		List<ApplicationCommandChoice> choices = new ArrayList<>();
		if(node.choiceProvider != null)
			node.choiceProvider.get(choices, g);
		if(choices.size() > 0) {
			DataArray choicesArray = DataArray.empty();
			for(ApplicationCommandChoice choice : choices) {
				DataObject choiceObj = DataObject.empty();
				choiceObj.put("name", choice.getName());
				choiceObj.put("value", choice.getValue());
				choicesArray.add(choiceObj);
			}
			obj.put("choices", choicesArray);
		}
		if(node.options.size() > 0) {
			DataArray optionArray = DataArray.empty();
			for(ApplicationCommandNode option : node.options)
				optionArray.add(buildArgument(g, option));
			obj.put("options", optionArray);
		}
		return obj;
	}
	
	protected RestActionImpl<Response> requestGet(CompiledRoute route) {
		return new RestActionImpl<>(jda, route, (response, request) -> {
			return response;
		});
	}
	
	public JDA getJDA() {
		return jda;
	}

}
