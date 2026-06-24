package io.github.tetratheta.farmmanager.service;

import io.github.tetratheta.mol.message.MessageChannel;
import io.github.tetratheta.mol.message.MessageService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/// Sends gameplay notifications while throttling repeated chat messages.
public final class NotificationService {
  private final long chatCooldownMillis;
  private final MessageChannel channel;
  private final Map<String, Long> lastChatTimes;
  private final MessageService messageService;

  /// Creates a notification service.
  ///
  /// @param messageService    localized message service
  /// @param channel           player-facing notification channel
  /// @param chatCooldownTicks listener chat cooldown in ticks
  public NotificationService(MessageService messageService, MessageChannel channel, int chatCooldownTicks) {
    this.messageService = messageService;
    this.channel = channel;
    chatCooldownMillis = Math.max(0L, chatCooldownTicks) * 50L;
    lastChatTimes = new ConcurrentHashMap<>();
  }

  /// Sends a gameplay notification to a player.
  ///
  /// Chat notifications are throttled because they can flood history. Action bar notifications are transient, so they are sent every time.
  ///
  /// @param player      recipient
  /// @param messagePath localized message path
  /// @param arguments   positional values consumed by `MessageFormat`
  public void send(Player player, String messagePath, Object... arguments) {
    if (channel == MessageChannel.ACTION_BAR) {
      messageService.send(player, channel, messagePath, arguments);
      return;
    }
    if (!shouldSendChat(player.getUniqueId(), messagePath)) return;
    messageService.send(player, channel, messagePath, arguments);
  }

  /// Returns whether a chat message should be sent now.
  ///
  /// @param playerId    recipient UUID
  /// @param messagePath localized message path
  /// @return true when the message should be sent
  private boolean shouldSendChat(UUID playerId, String messagePath) {
    if (chatCooldownMillis <= 0L) return true;
    long now = System.currentTimeMillis();
    String key = playerId + "\u0000" + messagePath;
    Long lastSent = lastChatTimes.get(key);
    if (lastSent != null && now - lastSent < chatCooldownMillis) return false;
    lastChatTimes.put(key, now);
    return true;
  }
}
