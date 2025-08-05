# Understanding AiServices for building an MCP tool in LangChain4J, back to basic

You have written your first application using LangChain4J its AiServices class to implement an MCP tool. 
If you want to understand how it works, or your tools did not work as expected and an attempt to debug took too much time to understand, keep on reading.

First an example MCP tool application is given that uses the AiServices class.
In the LangChain4J documentation there are some steps documented how to implement MCP tools directly, without using the AiService.
Sadly some in between are missing.

The second example the minimal steps are given to do this. This can be used as a starting point to build MCP tools without the complexity of the AiServices.

Let's start, first examine the code using the AiServices. If this example is not clear, check example in branch **external-qwen-llm-with-costs-mcp** first.
These examples do not need an OpenAI token to run.

## The code using the AiServices to rewrite

```java
package eu.smink.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
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

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new ConferenceTool())
                .build();

        // Example using AiServices and assistant
        System.out.println(assistant.chat("Get me a list of cool conference talks in 2024"));

        costCalculator.printReport();
    }
}
```
The AiServices requires an interface like **Assistant** to create a runtime proxy that implements that interface using the specified **model** and **ConferenceTool**.
An instance of **Assistant** is used to make chat calls finally.
If you debug such a chat call, you can easily get lost because of the code generated at runtime.
The following example is functionally the same, but chat requests are hard coded.  
The code only using the more simple API's of LangChain4J is:
```java
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

        costCalculator.printReport();
    }
}
```
Roughly, it exists of the following steps: 
* First, **ToolSpecifications** is called to get a list of all **ToolSpecification**s for given ConferenceTool.
* Construct a **ChatRequest** with all tool specifications and call **chat** with a user message using the conference tool. The model will analyze which tool and which parameters to call.
* At this point the tool with parameters is known, but not yet called. That will happen at the following chat call.
* Get the tool to call from the returned **ToolExecutionRequest**
* To be able to call the tool, its **ToolExecutor** is required. The tool executor also needs to know what java method must be called to execute the tool. That **Method** is retrieved from the ConferenceTool class. 
* Create a **ToolExecutor** for the tool detected (using the **DefaultToolExecutor**, it is assumed the detected tool matches with the first declared method found in the ConferenceTool).
* The **ToolExecutor** instance has a **execute** method that is called to execute and get the result of the tool.  
* Besides System and User messages, a special **ToolExecutionResultMessage** is required to pass the tool output back to the model.
* Prepare a **ToolExecutionResultMessage** to contain the tool output
* Construct a **ChatRequest** with the userMessage, aiMessage containing previous chat result, and the toolExecutionResultMessage.
* Call **chat** with the previous created **ChatRequest** to let the model process the output of the tool and combine all results in the final answer.

## Source code
The repository that contains this application and all files needed to build and run, can be found at https://github.com/PGSmink/ai-examples-java.git, 
in branch **external-qwen-llm-mcp-essentials**.
This code in this branch is a based on the code in branch **external-qwen-llm-with-costs-mcp**.

# Build and run
After checking out this repository and switching branch, you can go to the root directory and build and run
the application using
```
./gradlew run
```
(do not forget to start the LLM before running the application)
This will run both versions after each other. 

## Summary

The sample chat implementation using the **AiService** requires an extra interface and a few lines of code. Most needed code is hidden behind a proxy and automatically generated at runtime.
This makes it harder to debug or understand how it works. 
It is possible to replace the AiService with code that is easier to read and debug, for you to learn how  MCP is implemented in LangChain4j.

## Note
The rewritten code still contains hard coded methods. In coming blogs a version will be published that can direct be used as an alternative for the  **AiServices**. 


# See also

* https://docs.langchain4j.dev
* https://docs.langchain4j.dev/integrations/language-models/ollama
* https://github.com/ollama/ollama
* https://huggingface.co/Qwen/Qwen2-7B
* https://github.com/langchain4j/langchain4j