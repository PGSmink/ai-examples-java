
# Getting started with AI using Ollama and langchain4j on any laptop, part3 costs

This branch I will add more logging and reporting to the application given in branch **external-tinydolphin-llm**.
In order to get more insight, the following is added:
* Logging of duration of each chat call,
* Logging of input messages,
* Reporting of costs per chat call and in total.

# Chat duration
For this we could add logging around the actual chat calls, but there is a better way to do this.
It is possible to register a listener on a **ChatModel** instance, that has methods called at the begin (request) and the end (response) of a chat call.
The interface is as follows
```
interface ChatModelListener {
   void onRequest(ChatModelRequestContext requestContext);
   void onResponse(ChatModelResponseContext responseContext);
}
```
the implementation to log the duration is as follows:
```
package eu.smink.ai;

import dev.langchain4j.model.chat.listener.*;
import java.time.Instant;

public class ChatStopWatch implements ChatModelListener
{
    private Instant instantStart;

    @Override
    public void onRequest(ChatModelRequestContext requestContext)
    {
        instantStart = Instant.now();
    }


    @Override
    public void onResponse(ChatModelResponseContext responseContext)
    {
        Instant instantEnd = Instant.now();
        long seconds = instantEnd.getEpochSecond() - instantStart.getEpochSecond();
        System.out.println("Chat took " + seconds + " seconds\n");
    }
}
```
This will log the duration of each chat call after it is registered on the ChatModel instance.

## Logging of input messages
This  logging gives insight in all system (system prompt) and user messages (the actual question) used in a single request.
Again, these messages can be logged using the **ChatModelListener** interface, in the **onRequest** method.
The implementation is as follows
```
package eu.smink.ai;

import dev.langchain4j.model.chat.listener.*;

public class RequestMessageLogger implements ChatModelListener
{
    @Override
    public void onRequest(ChatModelRequestContext requestContext)
    {
        ChatModelListener.super.onRequest(requestContext);
        System.out.println("Chat input messages:");
        requestContext.chatRequest()
              .messages()
              .forEach(
                 chatMessage -> System.out.println("     " + chatMessage)
              );
    }
}
```

## Reporting of costs per call and in total

Ofcouse there are no direct costs involved when running local. 
But if you intend to, or are already using, an AI provider in the cloud it good to have insights in the costs involved.
Disclaimer: costs charged by an AI provider can slightly deviate from costs calculated. 

There is a little complication, that in the current application the following two chat methods are used:
```
String ChatModel#chat(String message);
ChatResponse ChatModel#chat(ChatMessage ... messages);
```
The costs can be found in the ChatResponse object returned by the second method. 
But for the first method it is not possible to derive the costs.
To calculate the costs for both methods, again the  **ChatModelListener** interface can be used.
The implementation will also keep track of the overall totals, so they can be printed in a report at the end.

The implementation is as follows:
```
package eu.smink.ai;

import dev.langchain4j.model.chat.listener.*;
import dev.langchain4j.model.output.TokenUsage;

public class CostCalculator implements ChatModelListener
{
    private final double costPerInputTokenInCents;
    private final double costPerOutputTokenInCents;
    private final String currencyInCents;
    private long totalNrOfInputTokens = 0L;
    private long totalNrOfOutputTokens = 0L;
    private long totalNrOfCalls = 0L;
    private double totalInputCosts = 0d;
    private double totalOutputCosts = 0d;

    public CostCalculator(double costPerInputTokenInCents, double costPerOutputTokenInCents, String currency)
    {
        this.costPerInputTokenInCents = costPerInputTokenInCents;
        this.costPerOutputTokenInCents = costPerOutputTokenInCents;
        currencyInCents = "ct (" + currency + ")";
    }

    public double getTotalCosts()
    {
        return totalInputCosts + totalOutputCosts;
    }

    public double getTotalInputCosts()
    {
        return totalInputCosts;
    }

    public long getTotalNrOfCalls()
    {
        return totalNrOfCalls;
    }

    public long getTotalNrOfInputTokens()
    {
        return totalNrOfInputTokens;
    }

    public long getTotalNrOfOutputTokens()
    {
        return totalNrOfOutputTokens;
    }

    public long getTotalNrTokens()
    {
        return totalNrOfInputTokens + totalNrOfOutputTokens;
    }

    public double getTotalOutputCosts()
    {
        return totalOutputCosts;
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext)
    {
        TokenUsage tokenUsage = responseContext.chatResponse().tokenUsage();
        Integer inputTokenCount = tokenUsage.inputTokenCount();
        Integer outputTokenCount = tokenUsage.outputTokenCount();
        Integer total = tokenUsage.totalTokenCount();
        double estimateCostsInput = inputTokenCount * costPerInputTokenInCents;
        double estimateCostsOutput = outputTokenCount * costPerOutputTokenInCents;
        totalNrOfInputTokens += inputTokenCount;
        totalNrOfOutputTokens += outputTokenCount;
        totalInputCosts += estimateCostsInput;
        totalOutputCosts += estimateCostsOutput;
        totalNrOfCalls++;
        double estimatedCost = estimateCostsInput + estimateCostsOutput;
        System.out.println("Call input tokens       : " + inputTokenCount);
        System.out.println("Call output tokens      : " + outputTokenCount);
        System.out.println("Call tokens             : " + total);
        System.out.printf("Call input token costs  : %.04f %s\n", estimateCostsInput, currencyInCents);
        System.out.printf("Call output token costs : %.04f %s\n", estimateCostsOutput, currencyInCents);
        System.out.printf("Call token costs        : %.04f %s\n", estimatedCost, currencyInCents);
    }

    public void printReport()
    {
        System.out.println("Total input tokens       : " + getTotalNrOfInputTokens());
        System.out.println("Total output tokens      : " + getTotalNrOfOutputTokens());
        System.out.println("Total tokens             : " + getTotalNrTokens());
        System.out.printf("Total input token costs  : %.04f %s\n", getTotalInputCosts(), currencyInCents);
        System.out.printf("Total output token costs : %.04f %s\n", getTotalOutputCosts(), currencyInCents);
        System.out.printf("Total token costs        : %.04f %s\n", getTotalCosts(), currencyInCents);
        System.out.println("Total calls              : " + getTotalNrOfCalls());
    }
}
```
In the constructor the cost per input and output token and the currency is specified. 
So the user can specify the costs of the current or intended AI provider to be used, including the corresponding currency. 

The code of interest in the implementation given above, is:
```
    public void onResponse(ChatModelResponseContext responseContext)
    {
        TokenUsage tokenUsage = responseContext.chatResponse().tokenUsage();
        Integer inputTokenCount = tokenUsage.inputTokenCount();
        Integer outputTokenCount = tokenUsage.outputTokenCount();
        Integer total = tokenUsage.totalTokenCount();
        ...
```
The tokenUsage class has getters for the needed token counts.

The **printreport()** method is called at the end of the application to print a report with all totals.


## Changes on application
All new ChatModelListener implementations have to be registered. 

The new application becomes as follows:

```
package eu.smink.ai;

import dev.langchain4j.data.message.*;
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
```
The three new listeners are created and registered on the **ChatModel**. 
The constructor of **CostCalculator** needs the costs of input and output token in cents and the currency used.

See for example https://openai.com/api/pricing for pricing when OpenAI is used.

## Source code
The repository that contains this application and all files needed to build and run, can be found at https://github.com/PGSmink/ai-examples-java.git, 
in branch **external-tinydolphin-llm-with-costs**.

# Build and run
After checking out this repository and switching branch, you can go to the root directory and build and run
the application using
```
./gradlew run
```
(do not forget to start the LLM before running the application)

the output is for example
```
Chat input messages:
     UserMessage { name = null contents = [TextContent { text = "Hi what's your model and role" }] }
Call input tokens       : 37
Call output tokens      : 46
Call tokens             : 83
Call input token costs  : 0.0074 ct (EUR)
Call output token costs : 0.0368 ct (EUR)
Call token costs        : 0.0442 ct (EUR)
Chat took 3 seconds

 I am Dolphin, an AI model that assists in various tasks such as text analysis, natural language processing, and machine learning. My primary role is to assist you with any questions or concerns you may have.


Chat input messages:
     SystemMessage { text = "You are a history student" }
     UserMessage { name = null contents = [TextContent { text = "Give three German speaking countries in Europe" }] }
Call input tokens       : 29
Call output tokens      : 221
Call tokens             : 250
Call input token costs  : 0.0058 ct (EUR)
Call output token costs : 0.1768 ct (EUR)
Call token costs        : 0.1826 ct (EUR)
Chat took 11 seconds

 Sure, here are three German speaking countries in Europe:

1. Germany: This is the most popular and well-known country for German language speakers. It's located in Central Europe and has a rich history of culture, music, and cuisine. The official language is German, but there are also many other languages spoken, such as Alsatian, Bavarian, and Sorbian.

2. Austria: Another well-known country for German speakers, Austria is located in Central Europe and has a rich history of culture, music, and cuisine. The official language is German, but there are also many other languages spoken, such as Czech, Hungarian, and Slovene.

3. Switzerland: Although not a country itself, Switzerland is an important hub for German speakers due to its close proximity to Germany. It's located in Western Europe and has a rich history of culture, music, and cuisine. The official language is German, but there are also many other languages spoken, such as French and Italian.

Total input tokens       : 66
Total output tokens      : 267
Total tokens             : 333
Total input token costs  : 0.0132 ct (EUR)
Total output token costs : 0.2136 ct (EUR)
Total token costs        : 0.2268 ct (EUR)
Total calls              : 2
```

The output contains logging of time spend and costs per call and a total summary for all calls. 

Also for the second call The SystemMessage and UserMessage are logged that are used for that call.

## Summary
The added request message logger, stop watch and cost calculator make it easier to track input messages used,
the duration of a chat call and the costs of chat calls executed. 
They can easily be reused your own langchain4j implementation.

# See also

* https://docs.langchain4j.dev
* https://docs.langchain4j.dev/integrations/language-models/ollama
* https://ollama.com/library/tinydolphin
* https://github.com/ollama/ollama
* https://openai.com/api/pricing for example of input/output token pricing