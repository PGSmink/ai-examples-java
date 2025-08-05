package eu.smink.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

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
                .timeout(Duration.of(5, ChronoUnit.MINUTES)) // add some time for slow laptops ;-)
                .build();

        chatAiServices(model);

        chatEssentials(model);

        costCalculator.printReport();
    }


    private static void chatAiServices(ChatModel model)
    {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new ConferenceTool())
                .build();

        // Example using AiServices and assistant
        System.out.println(assistant.chat("Get me a list of cool conference talks in 2024"));
    }


    private static void chatEssentials(ChatModel model)
    {
        // step 1: construct the tool specifications
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(ConferenceTool.class);

        // step 2: first chat to analyze tools to use
        UserMessage userMessage = UserMessage.from("Get list of cool conference talks in 2024");
        ChatRequest request = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse chatResponse = model.chat(request);
        AiMessage aiMessage = chatResponse.aiMessage();

        // step 3: prepare executing the tool
        ToolExecutionRequest toolExecutionRequest = aiMessage.toolExecutionRequests().getFirst();
        ToolExecutor defaultToolExecutor = new DefaultToolExecutor(new ConferenceTool(), ConferenceTool.class.getDeclaredMethods()[0]);
        // step 4: call the tool
        String executionResult = defaultToolExecutor.execute(toolExecutionRequest, "default");
        // step 5: merge all results in one chat
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, executionResult);
        ChatRequest requestWithToolOutput = ChatRequest.builder()
                .messages(List.of(userMessage, aiMessage, toolExecutionResultMessage))
                .toolSpecifications(toolSpecifications)
                .build();
        ChatResponse responseWithToolResult = model.chat(requestWithToolOutput);
        System.out.println(responseWithToolResult.aiMessage());
    }
}