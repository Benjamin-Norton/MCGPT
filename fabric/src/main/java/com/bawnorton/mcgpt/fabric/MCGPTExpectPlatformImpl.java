package com.bawnorton.mcgpt.fabric;

import com.bawnorton.mcgpt.MCGPTClient;
import com.bawnorton.mcgpt.config.Config;
import com.bawnorton.mcgpt.config.ConfigManager;
import com.bawnorton.mcgpt.store.SecureTokenStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.theokanning.openai.completion.chat.ChatMessage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public class MCGPTExpectPlatformImpl {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            registerAskCommand(dispatcher);
            registerAuthCommand(dispatcher);
            registerListConversationsCommand(dispatcher);
            registerNextConversationCommand(dispatcher);
            registerPreviousConversationCommand(dispatcher);
            registerSetConversationCommand(dispatcher);
        });
    }

    private static void registerAskCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("ask")
                .then(ClientCommandManager.argument("question", StringArgumentType.greedyString()).executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    String question = StringArgumentType.getString(context, "question");
                    source.sendFeedback(Text.literal("§7<" + source.getPlayer().getDisplayName().getString() + "> " + question));
                    MCGPTClient.ask(question);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerAuthCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("mcpgt-auth")
                .then(ClientCommandManager.argument("token", StringArgumentType.string()).executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    String token = StringArgumentType.getString(context, "token");
                    if(token.length() != 51) {
                        MCGPTClient.LOGGER.error("Invalid token length");
                        source.sendFeedback(Text.translatable("mcgpt.auth.invalid.token"));
                        return 0;
                    }
                    if(!token.startsWith("sk-")) {
                        MCGPTClient.LOGGER.error("Invalid token prefix");
                        source.sendFeedback(Text.translatable("mcgpt.auth.invalid.token"));
                        return 0;
                    }
                    Config.getInstance().token = SecureTokenStorage.encrypt(token);
                    ConfigManager.saveConfig();
                    MCGPTClient.startService();
                    source.sendFeedback(Text.translatable("mcgpt.auth.success"));
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerListConversationsCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("listconversations").executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    List<List<ChatMessage>> conversations = MCGPTClient.getConversations();
                    source.sendFeedback(Text.translatable("mcgpt.conversation.list"));
                    for (int i = 0; i < conversations.size(); i++) {
                        List<ChatMessage> conversation = conversations.get(i);
                        if(conversation.size() < 2) continue;
                        String lastQuestion = conversation.get(conversation.size() - 2).getContent();
                        source.sendFeedback(Text.of("§b[MCGPT]: §r" + (i + 1) + ": " + lastQuestion));
                    }
                    return 1;
                });
        dispatcher.register(builder);
    }

    private static void registerNextConversationCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("nextconversation").executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    boolean newConversation = MCGPTClient.nextConversation();
                    int index = MCGPTClient.getConversationIndex();
                    if(newConversation) {
                        source.sendFeedback(Text.translatable("mcgpt.conversation.new", index + 1));
                    } else {
                        source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1));
                    }
                    return 1;
                });
        dispatcher.register(builder);
    }

    private static void registerPreviousConversationCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("previousconversation").executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    MCGPTClient.previousConversation();
                    int index = MCGPTClient.getConversationIndex();
                    source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1));
                    return 1;
                });
        dispatcher.register(builder);
    }

    private static void registerSetConversationCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("setconversation")
                .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1)).executes(context -> {
                    FabricClientCommandSource source = context.getSource();
                    int index = IntegerArgumentType.getInteger(context, "index") - 1;
                    if(index >= MCGPTClient.getConversations().size()) {
                        source.sendFeedback(Text.translatable("mcgpt.conversation.invalid"));
                        return 0;
                    }
                    MCGPTClient.setConversationIndex(index);
                    source.sendFeedback(Text.translatable("mcgpt.conversation.continue", index + 1));
                    return 1;
                }));
        dispatcher.register(builder);
    }
}
