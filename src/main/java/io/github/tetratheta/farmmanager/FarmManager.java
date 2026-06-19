package io.github.tetratheta.farmmanager;

import io.github.tetratheta.farmmanager.command.FMCommand;
import io.github.tetratheta.mol.plugin.BasePlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/// Bootstraps the FarmManager plugin and owns the active runtime.
public final class FarmManager extends BasePlugin<FarmManagerRuntime> {

  /// Creates the services and Bukkit resources for the current plugin configuration.
  ///
  /// @return new FarmManager runtime
  @Override
  protected FarmManagerRuntime createRuntime() {
    return new FarmManagerRuntime(this);
  }

  /// Registers commands after the initial runtime is available.
  @Override
  protected void onPluginEnabled() {
    getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            cmd -> cmd.registrar().register(new FMCommand(this).getCommand()));
  }
}
