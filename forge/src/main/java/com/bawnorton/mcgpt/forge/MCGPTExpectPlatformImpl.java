package com.bawnorton.mcgpt.forge;

import com.bawnorton.mcgpt.MCGPTClient;
import com.bawnorton.mcgpt.config.Config;
import com.bawnorton.mcgpt.config.ConfigManager;
import com.bawnorton.mcgpt.store.SecureTokenStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.theokanning.openai.completion.chat.ChatMessage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class MCGPTExpectPlatformImpl {
    public static void registerCommands() {
        // forced, subscribe to event
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        MCGPTClient.LOGGER.info("Registering commands");
        CommandDispatcher<ServerCommandSource> dispatcher = event.getDispatcher();
        registerAskCommand(dispatcher);
        registerAuthCommand(dispatcher);
        registerListConversationsCommand(dispatcher);
        registerNextConversationCommand(dispatcher);
        registerPreviousConversationCommand(dispatcher);
        registerSetConversationCommand(dispatcher);
    }

    private static void registerAskCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("ask")
                .then(CommandManager.argument("question", StringArgumentType.greedyString()).executes(context -> {
                    ServerCommandSource source = context.getSource();
                    String question = StringArgumentType.getString(context, "question");
                    source.sendFeedback(Text.literal("§7<" + source.getPlayer().getDisplayName().getString() + "> " + question), false);
                    MCGPTClient.ask(question);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerAuthCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("mcpgt-auth")
                .then(CommandManager.argument("token", StringArgumentType.string()).executes(context -> {
                    ServerCommandSource source = context.getSource();
                    String token = StringArgumentType.getString(context, "token");
                    if(token.length() != 51) {
                        MCGPTClient.LOGGER.error("Invalid token length");
                        source.sendFeedback(Text.translatable("mcgpt.auth.invalid.token"), false);
                        return 0;
                    }
                    if(!token.startsWith("sk-")) {
                        MCGPTClient.LOGGER.error("Invalid token prefix");
                        source.sendFeedback(Text.translatable("mcgpt.auth.invalid.token"), false);
                        return 0;
                    }
                    Config.getInstance().token = SecureTokenStorage.encrypt(token);
                    ConfigManager.saveConfig();
                    MCGPTClient.startService();
                    source.sendFeedback(Text.translatable("mcgpt.auth.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerListConversationsCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("listconversations").executes(context -> {
            ServerCommandSource source = context.getSource();
            List<List<ChatMessage>> conversations = MCGPTClient.getConversations();
            source.sendFeedback(Text.translatable("mcgpt.conversation.list"), false);
            for (int i = 0; i < conversations.size(); i++) {
                List<ChatMessage> conversation = conversations.get(i);
                if(conversation.size() < 2) continue;
                String lastQuestion = conversation.get(conversation.size() - 2).getContent();
                source.sendFeedback(Text.of("§b[MCGPT]: §r" + (i + 1) + ": " + lastQuestion), false);
            }
            return 1;
        });
        dispatcher.register(builder);
    }

    private static void registerNextConversationCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("nextconversation").executes(context -> {
            ServerCommandSource source = context.getSource();
            boolean newConversation = MCGPTClient.nextConversation();
            int index = MCGPTClient.getConversationIndex();
            if(newConversation) {
                source.sendFeedback(Text.translatable("mcgpt.conversation.new", index + 1), false);
            } else {
                source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1), false);
            }
            return 1;
        });
        dispatcher.register(builder);
    }

    private static void registerPreviousConversationCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("previousconversation").executes(context -> {
            ServerCommandSource source = context.getSource();
            MCGPTClient.previousConversation();
            int index = MCGPTClient.getConversationIndex();
            source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1), false);
            return 1;
        });
        dispatcher.register(builder);
    }

    private static void registerSetConversationCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("setconversation")
                .then(CommandManager.argument("index", IntegerArgumentType.integer()).executes(context -> {
                    ServerCommandSource source = context.getSource();
                    int index = IntegerArgumentType.getInteger(context, "index") - 1;
                    if (index >= MCGPTClient.getConversations().size()) {
                        source.sendFeedback(Text.translatable("mcgpt.conversation.invalid"), false);
                        return 0;
                    }
                    MCGPTClient.setConversationIndex(index);
                    source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }
}
