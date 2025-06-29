package eu.smink.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.List;

public class OllamaChatExample
{
    static final String LLM_MODEL = "tinydolphin";
    private static final ChatModelListener requestMessageLogger = new RequestMessageLogger();
    private static final CostCalculator costCalculator = new CostCalculator(
            2 / 10000.0, // hypothetical input token price in EURO cents
            8 / 10000.0, // hypothetical output token price in EURO cents
            "EUR"
    );
    private static final ChatStopWatch stopWatch = new ChatStopWatch();


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
                .listeners(List.of(costCalculator, requestMessageLogger, stopWatch))
                .build();

        // Example 1
        String answer = model.chat("Hi what's your model and role");
        System.out.println(answer);

        // Example 2
        ChatMessage systemMessage = new SystemMessage("You are a history student");
        ChatMessage userMessage = UserMessage.from("Give three German speaking countries in Europe");
        ChatResponse chatResponse = model.chat(systemMessage, userMessage);
        System.out.println(chatResponse.aiMessage().text());

        costCalculator.printReport();
    }
}