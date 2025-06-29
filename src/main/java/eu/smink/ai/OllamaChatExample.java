package eu.smink.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class OllamaChatExample
{
    static final String LLM_MODEL = "qwen2:7b"; // 5.1 GiB
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
                .timeout(Duration.of(10, ChronoUnit.MINUTES))
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(
                        new ConferenceTool()
                )
                .build();

        // Example 1
        String answer = model.chat("Hi what's your model and role");
        System.out.println(answer);

        // Example 2
        ChatMessage systemMessage = new SystemMessage("You are a history student");
        ChatMessage userMessage = UserMessage.from("Give three German speaking countries in Europe");
        ChatResponse chatResponse = model.chat(systemMessage, userMessage);
        System.out.println(chatResponse.aiMessage().text());

        System.out.println(assistant.chat("Get me a list of cool conference talks"));

        System.out.println(assistant.chat("Get me a list of cool conference talks in 2025"));


        costCalculator.printReport();
    }
}