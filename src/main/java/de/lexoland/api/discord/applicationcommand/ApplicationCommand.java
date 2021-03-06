package de.lexoland.api.discord.applicationcommand;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class ApplicationCommand {
	
	private static final int SUB_COMMAND = 1;
	private static final int SUB_COMMAND_GROUP = 2;
	
	protected long id;
	protected long applicationId;
	protected ApplicationCommandNode node;
	
	public abstract void build(ApplicationRootCommandNode root, Guild guild);
	public abstract String getName();
	
	protected ApplicationCommandNode option(String name, int type) {
		return new ApplicationCommandNode(name, type);
	}
	
	public long getId() {
		return id;
	}
	
	public long getApplicationId() {
		return applicationId;
	}
	
	public ApplicationCommandNode getNode() {
		return node;
	}

	public RestAction<Void> updatePermissions(ApplicationCommandAPI api, Guild guild) {
		List<ApplicationCommandPermission> permissions = getPermissions(guild);
		return api.editCommandPermissions(guild, id, permissions.toArray(new ApplicationCommandPermission[0]));
	}

	public List<ApplicationCommandPermission> getPermissions(Guild guild) {
		if(isRetrieved())
			throw new IllegalStateException("Can not update permissions of a retrieved command");
		ApplicationRootCommandNode root = (ApplicationRootCommandNode) node;
		List<ApplicationCommandPermission> permissions = new ArrayList<>();
		root.permissionProvider.get(permissions, guild);
		return permissions;
	}

	public boolean isRetrieved() {
		return !(node instanceof ApplicationRootCommandNode);
	}
	
	public static ApplicationCommandChoice choice(String name, String value) {
		return new ApplicationCommandChoice(name, value);
	}
	
	public static ApplicationCommandChoice choice(String name, int value) {
		return new ApplicationCommandChoice(name, value);
	}

	public static ApplicationSubCommandNode subCommand(String name) {
		return new ApplicationSubCommandNode(name);
	}

	public static ApplicationSubCommandGroupNode subCommandGroup(String name) {
		return new ApplicationSubCommandGroupNode(name);
	}

	public static class ApplicationCommandNode {
		
		protected String name, description = "No Description";
		protected int type;
		protected boolean required;
		protected ChoiceProvider choiceProvider = (choices1, g) -> {};
		protected Consumer<SlashCommandEvent> execute;
		protected List<ApplicationCommandNode> options = new ArrayList<>();
		protected PermissionProvider permissionProvider = (permissions, g) -> {};
		protected boolean defaultPermission = true;

		protected ApplicationCommandNode(String name, int type) {
			this.name = name.toLowerCase();
			this.type = type;
		}
		
		protected ApplicationCommandNode(String name) {
			this.name = name.toLowerCase();
			this.type = 0;
		}


		public ChoiceProvider getChoiceProvider() {
			return choiceProvider;
		}

		public String getDescription() {
			return description;
		}
		
		public Consumer<SlashCommandEvent> getExecute() {
			return execute;
		}
		
		public String getName() {
			return name;
		}
		
		public List<ApplicationCommandNode> getOptions() {
			return options;
		}
		
		public int getType() {
			return type;
		}

		public boolean getDefaultPermission() {
			return defaultPermission;
		}

		public PermissionProvider getPermissionProvider() {
			return permissionProvider;
		}
	}

	public static class ApplicationRootCommandNode extends ApplicationCommandNode {

		public ApplicationRootCommandNode(String name) {
			super(name, 0);
		}

		public ApplicationRootCommandNode then(ApplicationSubCommandNode node) {
			options.add(node);
			return this;
		}

		public ApplicationRootCommandNode then(ApplicationSubCommandGroupNode node) {
			options.add(node);
			return this;
		}

		public ApplicationRootCommandNode argument(String name, ArgumentType type, boolean required, String description, ApplicationCommandChoice... choices) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, description, choices));
			return this;
		}

		public ApplicationRootCommandNode argument(String name, ArgumentType type, boolean required, ApplicationCommandChoice... choices) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, "No description", choices));
			return this;
		}

		public ApplicationRootCommandNode argument(String name, ArgumentType type, boolean required, String description, ChoiceProvider choiceProvider) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, description, choiceProvider));
			return this;
		}

		public ApplicationRootCommandNode argument(String name, ArgumentType type, boolean required,  ChoiceProvider choiceProvider) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, "No description", choiceProvider));
			return this;
		}

		public ApplicationRootCommandNode executes(Consumer<SlashCommandEvent> execute) {
			this.execute = execute;
			return this;
		}

		public ApplicationRootCommandNode permissions(PermissionProvider permissionProvider) {
			this.permissionProvider = permissionProvider;
			return this;
		}

		public ApplicationRootCommandNode defaultPermission(boolean value) {
			this.defaultPermission = value;
			return this;
		}

		public ApplicationRootCommandNode description(String description) {
			this.description = description;
			return this;
		}
	}

	public static class ApplicationSubCommandNode extends ApplicationCommandNode {

		public ApplicationSubCommandNode(String name) {
			super(name, SUB_COMMAND);
		}

		public ApplicationSubCommandNode argument(String name, ArgumentType type, boolean required, String description, ApplicationCommandChoice... choices) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, description, choices));
			return this;
		}

		public ApplicationSubCommandNode argument(String name, ArgumentType type, boolean required, ApplicationCommandChoice... choices) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, "No description", choices));
			return this;
		}

		public ApplicationSubCommandNode argument(String name, ArgumentType type, boolean required, String description, ChoiceProvider choiceProvider) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, description, choiceProvider));
			return this;
		}

		public ApplicationSubCommandNode argument(String name, ArgumentType type, boolean required,  ChoiceProvider choiceProvider) {
			options.add(new ApplicationArgumentCommandNode(name, type, required, "No description", choiceProvider));
			return this;
		}

		public ApplicationSubCommandNode executes(Consumer<SlashCommandEvent> execute) {
			this.execute = execute;
			return this;
		}

		public ApplicationSubCommandNode description(String description) {
			this.description = description;
			return this;
		}
	}
	public static class ApplicationSubCommandGroupNode extends ApplicationCommandNode {

		public ApplicationSubCommandGroupNode(String name) {
			super(name, SUB_COMMAND_GROUP);
		}

		public ApplicationSubCommandGroupNode then(ApplicationSubCommandNode option) {
			options.add(option);
			return this;
		}
	}

	public static class ApplicationArgumentCommandNode extends ApplicationCommandNode {

		public ApplicationArgumentCommandNode(String name, ArgumentType type, boolean required, String description, ApplicationCommandChoice... choices) {
			this(name, type, required, description, choices.length == 0 ? null : (choices1, g) -> Collections.addAll(choices1, choices));
		}

		public ApplicationArgumentCommandNode(String name, ArgumentType type, boolean required, String description, ChoiceProvider choiceProvider) {
			super(name, type.getValue());
			this.required = required;
			if(type != ArgumentType.STRING && type != ArgumentType.INTEGER && choiceProvider != null)
				throw new IllegalArgumentException("Choices are only available for strings and integers");
			this.choiceProvider = choiceProvider;
			this.description = description;
		}
	}

	public static class ApplicationCommandChoice {
		
		private final String name;
		private final Object value;
		
		public ApplicationCommandChoice(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public Object getValue() {
			return value;
		}
	}

}
