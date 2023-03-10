package com.bawnorton.mcgpt;

import com.bawnorton.mcgpt.config.Config;
import com.bawnorton.mcgpt.config.ConfigManager;
import com.bawnorton.mcgpt.store.SecureTokenStorage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MCGPTClient {
    public static final String MOD_ID = "mcgpt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ExecutorService executor;

    private static OpenAiService service;
    private static List<List<ChatMessage>> conversations;
    private static int conversationIndex = 0;

    static {
        executor = Executors.newFixedThreadPool(1);
    }

    public static void init() {
        conversations = new ArrayList<>();

        MCGPTExpectPlatform.registerCommands();
        ConfigManager.loadConfig();

        if(!Config.getInstance().token.isEmpty()) {
            startService();
        }

        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            if(!notAuthed(false)) {
                player.sendMessage(Text.translatable("mcgpt.auth.success"));
            }
        });
    }

    public static void startService() {
        service = new OpenAiService(SecureTokenStorage.decrypt(Config.getInstance().secret, Config.getInstance().token));
    }

    public static boolean notAuthed() {
        return notAuthed(true);
    }

    public static boolean notAuthed(boolean prompt) {
        if(service == null) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if(player != null && prompt) {
                player.sendMessage(Text.translatable("mcgpt.auth.message1"));
                player.sendMessage(Text.translatable("mcgpt.auth.message2"));
                player.sendMessage(Text.literal("§chttps://platform.openai.com/account/api-keys").styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://platform.openai.com/account/api-keys"))));
            }
            return true;
        }
        return false;
    }

    public static List<List<ChatMessage>> getConversations() {
        return conversations;
    }

    public static int getConversationIndex() {
        return conversationIndex;
    }

    public static void setConversationIndex(int index) {
        if(index >= 0 && index < conversations.size()) {
            conversationIndex = index;
        }
    }

    public static boolean nextConversation() {
        if(notAuthed()) return false;
        if(conversationIndex < conversations.size() - 1) {
            conversationIndex++;
            return false;
        }
        conversations.add(new ArrayList<>());
        conversationIndex = conversations.size() - 1;
        conversations.get(conversationIndex).add(new ChatMessage("system", "Context: You are an AI assistant in the game Minecraft. Limit your responses to 256 characters. Assume the player cannot access commands unless explicitly asked for them. Do not simulate conversations"));
        return true;
    }

    public static void previousConversation() {
        if(notAuthed()) return;
        if(conversationIndex > 0) {
            conversationIndex--;
        }
    }

    private static void askSync(String question) {
        if(conversations.size() == 0) {
            nextConversation();
        }
        List<ChatMessage> conversation = conversations.get(conversationIndex);
        conversation.add(new ChatMessage("user", question));
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .messages(conversation)
                .model("gpt-3.5-turbo")
                .build();
        ChatMessage reply;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player == null) return;
        try {
            reply = service.createChatCompletion(req).getChoices().get(0).getMessage();
            conversation.add(reply);
            if(conversation.size() > 10) {
                conversation.remove(1); // don't remove the first message, as it's the minecraft context
            }
            player.sendMessage(Text.of("<ChatGPT> " + reply.getContent().replaceAll("^\\s+|\\s+$", "")), false);
        } catch (RuntimeException e) {
            player.sendMessage(Text.translatable("mcgpt.ask.error").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(e.getMessage())))));
        }
    }

    public static void ask(String question) {
        if(notAuthed()) return;
        executor.execute(() -> {
            try {
                askSync(question);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
