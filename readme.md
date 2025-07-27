# Getting started with MCP, AI using Ollama and langchain4j

This branch I shall develop on top of branch **external-tinydolphin-llm** and will add MCP tools.
MCP, https://modelcontextprotocol.io/overview is the next step to add tools to AI. As stated on their site, 
```
MCP provides a secure, standardized, simple way to give AI systems the context they need.
```
MCP allows you to build your own MCP server or one of many already out there.
The use case for this story is that 
  * Use chat client application as found in branch **external-tinydolphin-llm** as starting point
  * Information stored in the chat client application itself shall be made accessible
  * Use MCP for this, as it's a new standard 
  * The new MCP tool can run in the same application as the AI client and will return a list of conference talks, optionally for a given year.

## Pick LLM
Als a result of the last requirement, I'm not going to build a remote MCP tool, but a local MCP tool running in the same application as the AI client.
Not all LLM's support MCP right now. In the previous branch **external-tinydolphin-llm**, the TinyDolphin LLM was used, but that does not support MCP.
An LLM dat does support MCP is qwen2:7b:
* 7.6 billion parameters
* needs 5.1G memory
* Supports English language
* supports native MCP 
* has Apache 2.0 license
* Created by Alibaba, and still can run locally on a laptop.

## Needed dependencies
Two dependencies are added to the build.gradle
```
    implementation 'io.modelcontextprotocol.sdk:mcp:0.10.0'
    implementation 'dev.langchain4j:langchain4j-mcp:1.1.0-beta7'
```

The first is the implementation of the protocol, the second is the implementation to integrate it into langchain4j.

## Outline code
Since last version a AiServices class is added that hides code no longer needs to be written. It requires an Ai service interface (with any name) that it will implement for you:
```java
public interface Assistant
{
    String chat(String userMessage);
}
```
The AI service is created using:
```java
Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new YourTool1(), new YourTool2(), ... new YourToolN())
                .build();
```
Where **model** is the **OllamaChatModel** instance configured to use the qwen2 LLM.
It can be used like:
```java
        System.out.println(assistant.chat("Sentence to call one of the N tools..."));
```
By default the LLM can choose whether or not to use tools. 
It can be forced to use the tools alway (using ToolChoice.REQUIRED), 
but that is not supported by the qwen2 LLM: I did not use this option. 

## Conference Tool code
Lets implement the ConferenceTool in the existing application.
The ConferenceTool will list conference talks (inspired by https://github.com/danvega/javaone-mcp).
Each conference talk is stored in a record:
```java
package eu.smink.ai;

public record ConferenceTalk(String title, String url, String conference, int year) {}
```
A talk has a title, a URL, the name of the conference where it was recorded and the year of recording.

This corresponding Tool class is like:
```java
package eu.smink.ai;

import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.*;

public class ConferenceTool
{
    private static final List<ConferenceTalk> talks = new ArrayList<>();
    static
    {
        ConferenceTalk accessibility = new ConferenceTalk("We need you - Accessibility on web by Ramona Domen", "https://www.youtube.com/watch?v=RmWdw6k3xf0&list=PLpQuPreMkT6B9KypJdLDEruwbc1bWFxoL&index=27", "NLJug - J-Fall", 2024);
        ConferenceTalk hexagonal = new ConferenceTalk("Hexagonal Architecture in Practice, Live Coding That Will Make Your Applications More Sustainable ", "https://www.youtube.com/watch?v=YPmKHm7G19Q", "Devoxx Belgium", 2025);
        ConferenceTalk c4Model = new ConferenceTalk("C4 models as code By Simon Brown", "https://www.youtube.com/watch?v=LYzOc7vI-Uo", "Devoxx Belgium", 2025);

        talks.addAll(List.of(accessibility, hexagonal, c4Model));
    }

    @Tool("Get list of cool conference talks")
    public static McpSchema.CallToolResult getConferenceTalks()
    {
        List<McpSchema.Content> contents = new ArrayList<>();
        talks.stream()
                .map(Record::toString)
                .map(McpSchema.TextContent::new)
                .forEach(contents::add);
        return new McpSchema.CallToolResult(contents, false);
    }
}
```

The conference talks are stored in a static list. A  **getConferenceTalks** method is annotated with a **Tool** annotation containing the description, to be used in the LLM to select this tool.
The method returns a **CallToolResult** instance containing a list of **Context** objects, representing a table. The table could also be returned as a **String**, but by returning a **CallToolResult**, the output will be passed as a json string to the LLM.
That and makes it easier for the LLM to filter on columns.

The Tool method can have arguments. Let assume the tools should allow to filter on the year of the talk:

```java

/**
 * gets list of conferences, for a given year
 *
 * @param year {@code year of conference}
 * @return CallToolResult containing matching talks
 */
@Tool("Get list of cool conference talks in year")
public static McpSchema.CallToolResult getConferenceTalks(Integer year)
{
    List<McpSchema.Content> contents = new ArrayList<>();
    talks.stream().filter(talk -> year == null || talk.year() == year)
            .map(Record::toString)
            .map(McpSchema.TextContent::new)
            .forEach(contents::add);
     new McpSchema.CallToolResult(contents, false);
}
```
If you check the logs, when running the application, there is a json document describing the tool:
```json
{
  "type" : "function",
  "function" : {
    "name" : "getConferenceTalks",
    "description" : "Get list of cool conference talks in year",
    "parameters" : {
      "type" : "object",
      "properties" : {
        "arg0" : {
          "type" : "integer"
        }
      },
      "required" : [ "arg0" ]
    }
  }
}
```
If you compare this with https://platform.openai.com/docs/guides/function-calling?api-mode=responses#defining-functions, you can observe the description of the year argument is missing.
The description can be used my the LLM to decide which tool to call.
The description can be specified using the **P** annotation for each argument:
```java
McpSchema.CallToolResult getConferenceTalks(@P(value = "Year of conference") Integer year)
```
The properties returned now, are:
```json
"properties" : {
      "arg0" : {
        "type" : "integer",
        "description" : "Year of conference"
      }
    }
```
Al so the name of the year argument is "arg0". The ToolSpecifications are automatically generated using reflection. After debugging, I found that this is actually a compile time issue.
If the code is compiled with the **-parameters** setting, reflection behaves differently. When this flag is used, the names of method arguments are stored in the compiled code.
As a result, those method argument names can be read at runtime using reflection.

the following section needs to be added to the **build.gradle** file:
```
tasks.withType(JavaCompile) {
    options.compilerArgs.add("-parameters")
}
```
In new runs the "arg0" parameter name is replaced by "year" and can be used by the LLM.

It is possible to make the year argument optional:
```java
McpSchema.CallToolResult getConferenceTalks(@P(value = "Year of conference", required = false) Integer year)
```
For me this resulted in problems at runtime: When I did not specify a year, the LLM did set the year parameter to 2023, before calling the tool. I've not tested, if this behaviour is specific for the used Qwen2 LLM.

## Interface Assistant
In previous versions of the application I did use both available chat methods on the **ChatModel** interface:
```java
ChatResponse chat(ChatMessage ... messages);
String chat(String message);
```
Because behaviour is different for these methods.
Currently, interface Assistant only implements the second method. However, if the interface is changed to the first method, the tool is not called anymore.

## System prompts
In order to specify a system prompt, you can 
* code it in the interface Assistant
* specify it in the AiServices builder.
An example of the second is as follows:
```java
Assistant assistantWithPrompt = AiServices.builder(Assistant.class)
        .chatModel(model)
        .systemMessageProvider(obj -> "You are a history student")
        .tools(new ConferenceTool())
        .build();
System.out.println(assistantWithPrompt.chat("Give three German-speaking countries in Europe"));
```

Tip: When the application is run, the logging contains the system and user prompts used.

## The application
With all previous described changes, the new application is as follows:

```java
package eu.smink.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

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
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(new ConferenceTool())
                .build();

        // Example 1
        System.out.println(model.chat("Hi what's your model and role"));

        // Example 2
        Assistant assistantWithPrompt = AiServices.builder(Assistant.class)
                .chatModel(model)
                .systemMessageProvider(obj -> "You are a history student")
                .tools(new ConferenceTool())
                .build();
        System.out.println(assistantWithPrompt.chat("Give three German-speaking countries in Europe"));

        // Example 3
        System.out.println(assistant.chat("Get me a list of cool conference talks in 2024"));

        costCalculator.printReport();
    }
}
```

## Source code
The repository that contains this application and all files needed to build and run, can be found at https://github.com/PGSmink/ai-examples-java.git, 
in branch **external-qwen-llm-with-costs-mcp**.

# Build and run
After checking out this repository and switching branch, you can go to the root directory and build and run
the application using
```
./gradlew run
```
(do not forget to start the LLM before running the application)

# AiServices pros/cons

The way the AiServices is implemented now has some drawbacks
- If you forget to build with **-parameters**, you might get less optimal results without knowing. 
- Tools are not called, for the chat method returning a **ChatResponse**
- As soon as you start debugging, code is complex and not easy to read.

There is a lot of work in progress, so this might change quickly.
 
## Summary
Qwen2.7B allows it to build and test MCP tools on your local laptop without the need of an AI provider (or internet).
There are some issues with optional arguments, but that can be worked around using extra tools with a different description.

# See also

* https://docs.langchain4j.dev
* https://docs.langchain4j.dev/integrations/language-models/ollama
* https://github.com/ollama/ollama
* https://huggingface.co/Qwen/Qwen2-7B
* https://github.com/langchain4j/langchain4j