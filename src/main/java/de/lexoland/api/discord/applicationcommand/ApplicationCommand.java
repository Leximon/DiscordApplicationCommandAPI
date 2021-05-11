package de.lexoland.api.discord.applicationcommand;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ApplicationCommand {
	
	public static final int SUB_COMMAND = 1;
	public static final int SUB_COMMAND_GROUP = 2;
	public static final int STRING = 3;
	public static final int INTEGER = 4;
	public static final int BOOLEAN = 5;
	public static final int USER = 6;
	public static final int CHANNEL = 7;
	public static final int ROLE = 8;
	public static final int MENTIONABLE = 9;
	
	protected long id;
	protected long applicationId;
	protected ApplicationCommandNode node;
	
	public abstract ApplicationCommandNode build();
	
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
	
	public static ApplicationCommandChoice choice(String name, String value) {
		return new ApplicationCommandChoice(name, value);
	}
	
	public static ApplicationCommandChoice choice(String name, int value) {
		return new ApplicationCommandChoice(name, value);
	}
	
	public static class ApplicationCommandNode {
		
		protected String name, description = "No Description";
		protected int type;
		protected boolean required;
		protected ApplicationCommandChoice[] choices = new ApplicationCommandChoice[0];
		protected Consumer<Interaction> execute;
		protected List<ApplicationCommandNode> options = new ArrayList<>();

		protected ApplicationCommandNode(String name, int type) {
			this.name = name.toLowerCase();
			this.type = type;
		}
		
		protected ApplicationCommandNode(String name) {
			this.name = name.toLowerCase();
			this.type = 0;
		}
		
		public ApplicationCommandNode name(String name) {
			this.name = name;
			return this;
		}
		
		public ApplicationCommandNode description(String description) {
			this.description = description;
			return this;
		}
		
		public ApplicationCommandNode required(boolean required) {
			this.required = required;
			return this;
		}

		/*public ApplicationCommandNode choices(ApplicationCommandChoice... choices) {
			this.choices = choices;
			return this;
		}

		public ApplicationCommandNode executes(Consumer<Interaction> execute) {
			this.execute = execute;
			return this;
		}
		
		public ApplicationCommandNode then(ApplicationCommandNode option) {
			options.add(option);
			return this;
		}*/
		
		public ApplicationCommandChoice[] getChoices() {
			return choices;
		}
		
		public String getDescription() {
			return description;
		}
		
		public Consumer<Interaction> getExecute() {
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

		public ApplicationRootCommandNode argument(ApplicationArgumentCommandNode argument) {
			options.add(argument);
			return this;
		}

		public ApplicationRootCommandNode executes(Consumer<Interaction> execute) {
			this.execute = execute;
			return this;
		}
	}

	public static class ApplicationSubCommandNode extends ApplicationCommandNode {

		public ApplicationSubCommandNode(String name) {
			super(name, SUB_COMMAND);
		}

		public ApplicationSubCommandNode argument(ApplicationArgumentCommandNode argument) {
			options.add(argument);
			return this;
		}

		public ApplicationSubCommandNode executes(Consumer<Interaction> execute) {
			this.execute = execute;
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

		public ApplicationArgumentCommandNode(String name, int type, ApplicationCommandChoice... choices) {
			super(name, type);
			if(type != STRING && type != INTEGER && choices.length >= 1)
				throw new IllegalArgumentException("Choices are only available for strings and integers");
			this.choices = choices;
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