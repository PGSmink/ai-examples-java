package eu.smink.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;

public class OllamaChatExample
{
    static final String LLM_MODEL = "tinydolphin";

    public static void main(String[] args)
    {
        // Build the ChatModel
        String endpoint = "http://localhost:11434";
        ChatModel model = OllamaChatModel.builder()
                .baseUrl(endpoint)
                .temperature(0.0)
                .logRequests(true)
                .logResponses(true)
                .modelName(LLM_MODEL)
                .build();

        // Example 1
        System.out.println(model.chat("Hi what's your model and role"));

        // Example 2
        ChatMessage systemMessage = new SystemMessage("You are a history student");
        ChatMessage userMessage = UserMessage.from("Give three German speaking countries in Europe");
        ChatResponse chatResponse = model.chat(systemMessage, userMessage);
        System.out.println(chatResponse.aiMessage().text());
    }
}