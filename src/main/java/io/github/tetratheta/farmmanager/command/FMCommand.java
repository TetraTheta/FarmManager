package io.github.tetratheta.farmmanager.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.tetratheta.farmmanager.FarmManager;
import io.github.tetratheta.farmmanager.FarmManagerRuntime;
import io.github.tetratheta.farmmanager.region.RegionKey;
import io.github.tetratheta.farmmanager.region.RegionResolution;
import io.github.tetratheta.mol.message.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/// Builds and executes the `/fm` command tree.
public final class FMCommand {
  private static final String ARG_REGION = "region";
  private static final String PERMISSION_ADMIN = "farmmanager.admin";
  private final FarmManager plugin;

  /// Creates the root command builder.
  ///
  /// @param plugin plugin entry point used to access active services
  public FMCommand(FarmManager plugin) {
    this.plugin = plugin;
  }

  /// Builds the root `/fm` command.
  ///
  /// @return Brigadier command node registered during the command lifecycle event
  public LiteralCommandNode<CommandSourceStack> getCommand() {
    return Commands.literal("fm").executes(this::listRegions).then(getReloadCommand()).then(getRegionCommand()).build();
  }

  /// Builds the reload subcommand.
  ///
  /// @return reload command builder
  private LiteralArgumentBuilder<CommandSourceStack> getReloadCommand() {
    return Commands.literal("reload").executes(ctx -> {
      if (!ensureAdmin(ctx)) return Command.SINGLE_SUCCESS;
      CommandSender sender = ctx.getSource().getSender();
      plugin.reloadRuntime();
      plugin.getRuntime().getMessageService().send(sender, "command.reload.success");
      return Command.SINGLE_SUCCESS;
    });
  }

  /// Builds the region subcommand tree.
  ///
  /// @return region command builder
  private LiteralArgumentBuilder<CommandSourceStack> getRegionCommand() {
    return Commands.literal("region").executes(this::listRegions).then(Commands.literal("add").then(
                     Commands.argument(ARG_REGION, StringArgumentType.greedyString()).suggests((ctx, builder) -> suggestRegions(ctx, builder, false))
                             .executes(this::addRegion))).then(getRemoveCommand("del")).then(getRemoveCommand("delete")).then(getRemoveCommand(
                     "remove"))
                   .then(Commands.literal("list").executes(this::listRegions));
  }

  /// Builds one remove alias.
  ///
  /// @param alias remove alias
  /// @return remove command builder
  private LiteralArgumentBuilder<CommandSourceStack> getRemoveCommand(String alias) {
    return Commands.literal(alias).then(
      Commands.argument(ARG_REGION, StringArgumentType.greedyString()).suggests((ctx, builder) -> suggestRegions(ctx, builder, true))
              .executes(this::removeRegion));
  }

  /// Adds a watched WorldGuard region.
  ///
  /// @param ctx command context
  /// @return command result
  @SuppressWarnings("SameReturnValue")
  private int addRegion(CommandContext<CommandSourceStack> ctx) {
    if (!ensureAdmin(ctx)) return Command.SINGLE_SUCCESS;
    CommandSender sender = ctx.getSource().getSender();
    MessageService messages = runtime().getMessageService();
    String input = StringArgumentType.getString(ctx, ARG_REGION);
    RegionResolution resolution = runtime().getRegionService().resolveCommandInput(input, sender);
    if (sendResolutionFailure(sender, input, resolution)) return Command.SINGLE_SUCCESS;
    RegionKey region = resolution.region();
    if (runtime().getRegionService().isWatchedRegion(region)) {
      messages.send(sender, "command.region.already-watched", region.asString());
      return Command.SINGLE_SUCCESS;
    }
    runtime().getRegionService().addWatchedRegion(region);
    runtime().getConfig().addWatchedRegion(region);
    runtime().getConfig().saveConfig();
    messages.send(sender, "command.region.added", region.asString());
    return Command.SINGLE_SUCCESS;
  }

  /// Removes a watched WorldGuard region.
  ///
  /// @param ctx command context
  /// @return command result
  @SuppressWarnings("SameReturnValue")
  private int removeRegion(CommandContext<CommandSourceStack> ctx) {
    if (!ensureAdmin(ctx)) return Command.SINGLE_SUCCESS;
    CommandSender sender = ctx.getSource().getSender();
    MessageService messages = runtime().getMessageService();
    String input = StringArgumentType.getString(ctx, ARG_REGION);
    Optional<RegionKey> resolvedRegion = resolveWatchedInput(input, sender);
    if (resolvedRegion.isEmpty()) {
      messages.send(sender, "command.region.not-watched", input);
      return Command.SINGLE_SUCCESS;
    }
    RegionKey region = resolvedRegion.get();
    runtime().getRegionService().removeWatchedRegion(region);
    runtime().getConfig().removeWatchedRegion(region);
    runtime().getConfig().saveConfig();
    messages.send(sender, "command.region.removed", region.asString());
    return Command.SINGLE_SUCCESS;
  }

  /// Lists watched regions.
  ///
  /// @param ctx command context
  /// @return command result
  @SuppressWarnings("SameReturnValue")
  private int listRegions(CommandContext<CommandSourceStack> ctx) {
    if (!ensureAdmin(ctx)) return Command.SINGLE_SUCCESS;
    CommandSender sender = ctx.getSource().getSender();
    MessageService messages = runtime().getMessageService();
    Set<RegionKey> watchedRegions = runtime().getRegionService().getWatchedRegions();
    if (watchedRegions.isEmpty()) {
      messages.send(sender, "command.region.empty-list");
      return Command.SINGLE_SUCCESS;
    }
    messages.send(sender, "command.region.list-header");
    for (RegionKey region : watchedRegions) messages.send(sender, "command.region.list-entry", region.asString());
    return Command.SINGLE_SUCCESS;
  }

  /// Sends suggestions for region arguments.
  ///
  /// @param ctx         command context
  /// @param builder     suggestions builder
  /// @param watchedOnly true when only watched regions should be suggested
  /// @return suggestion future
  private CompletableFuture<Suggestions> suggestRegions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder, boolean watchedOnly) {
    CommandSender sender = ctx.getSource().getSender();
    for (String suggestion : runtime().getRegionService().suggest(sender, builder.getRemaining(), watchedOnly)) builder.suggest(suggestion);
    return builder.buildFuture();
  }

  /// Returns the active runtime.
  ///
  /// @return active runtime
  private FarmManagerRuntime runtime() {
    return plugin.getRuntime();
  }

  /// Ensures a command sender has the admin permission and sends a direct command reply.
  ///
  /// @param ctx command context
  /// @return true when the sender is allowed
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean ensureAdmin(CommandContext<CommandSourceStack> ctx) {
    CommandSender sender = ctx.getSource().getSender();
    if (sender.hasPermission(PERMISSION_ADMIN)) return true;
    runtime().getMessageService().send(sender, "command.no-permission");
    return false;
  }

  /// Sends a localized message for failed command input resolution.
  ///
  /// @param sender     command sender
  /// @param input      raw user input
  /// @param resolution resolution result
  /// @return true when a failure was sent
  private boolean sendResolutionFailure(CommandSender sender, String input, RegionResolution resolution) {
    if (resolution.status() == RegionResolution.Status.RESOLVED) return false;
    MessageService messages = runtime().getMessageService();
    switch (resolution.status()) {
      case AMBIGUOUS_CONSOLE_REGION -> messages.send(sender, "command.region.ambiguous-console-region", input);
      case INVALID_FORMAT -> messages.send(sender, "command.region.invalid-format", input);
      case NOT_FOUND -> messages.send(sender, "command.region.not-found", input);
      case UNKNOWN_WORLD -> messages.send(sender, "command.region.unknown-world", input);
    }
    return true;
  }

  /// Resolves input against the watched list so stale regions can still be removed.
  ///
  /// @param input  raw command input
  /// @param sender command sender
  /// @return watched region key, or empty when not watched or ambiguous
  private Optional<RegionKey> resolveWatchedInput(String input, CommandSender sender) {
    if (input.contains(":")) {
      Optional<RegionKey> parsedRegion = RegionKey.parse(input);
      return parsedRegion.filter(region -> runtime().getRegionService().isWatchedRegion(region));
    }
    List<RegionKey> candidates = new ArrayList<>();
    if (sender instanceof Player player) {
      RegionKey region = new RegionKey(player.getWorld().getName(), input);
      if (runtime().getRegionService().isWatchedRegion(region)) candidates.add(region);
    } else {
      for (RegionKey region : runtime().getRegionService().getWatchedRegions()) {
        if (region.regionId().equalsIgnoreCase(input)) candidates.add(region);
      }
    }
    return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
  }
}
