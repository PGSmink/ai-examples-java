
# Getting started with AI using Ollama and langchain4j on any laptop

If you want to know how to get started with AI in Java, this article will certainly help you to get some experience in
pragmatically building and running it on your own laptop. 
Even if your laptop doesn't have a dedicated graphics card.

Features are
* Chat client written in Java
* Using langchain4j AI client to connect to an AI server
* Using langchain4j library only, to build chat app.
* Using Gradle build automation tool for simple short build file
* Run locally on your laptop, so no AI account needed (yet)
* Assumes you've podman container runtime installed (or Docker)
* Uses Ollama to run the LLM in the container on the CPU.
* Needs only about 1.4G free memory to load the 637M LLM

In order to get quickly started, the application uses TestContainer for setting up and running Ollama in a container.
The AI model loaded in Ollama is tinydolphin, a small model introduced in 2024. This model was selected because of its small size to be able to run it on any laptop.

The application is as follows:

```
package eu.smink.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.testcontainers.containers.Container;
import org.testcontainers.ollama.OllamaContainer;

import java.io.IOException;

public class OllamaChatExample
{
    static final String OLLAMA_IMAGE = "ollama/ollama:latest";
    static final String LLM_MODEL = "tinydolphin";

    public static void main(String[] args)
    {
        try (OllamaContainer ollama = new OllamaContainer(OLLAMA_IMAGE))
        {
            startLLM(ollama);

            // Build the ChatModel
            String endpoint = ollama.getEndpoint();
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


    private static void startLLM(OllamaContainer ollama)
    {
        ollama.start();

        try
        {
            ollama.execInContainer("ollama", "pull", LLM_MODEL);
        }
        catch (IOException | InterruptedException ex)
        {
            throw new RuntimeException("Error pulling model", ex);
        }
    }
}
```
A constant OLLAMA_IMAGE (line 16) is defined that contains the container image name to use to run Ollama. At runtime this image uses the LLM_MODEL defined (line 17).
Line 21/23 start up an Ollama instance running the LLM in the background at the endpoint URL retrieved at line 26.
Now the LLM is up and running an LLM client is setup at line 27-33. The temperature is set to 0 to have as less hallucination as possible.
Logging of requests and response is turned on to be able to follow what is happening. The method ```startLLM``` is used to start the LLM in a container so you do not have to configure it manually yourself. 

## Gradle
The Gradle build file used is as follows:

```
plugins {
    id 'application'
}

application {
    mainClass = "eu.smink.ai.OllamaChatExample"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.testcontainers:ollama:1.21.3'
    implementation 'dev.langchain4j:langchain4j-core:1.1.0'
    implementation 'dev.langchain4j:langchain4j-ollama:1.1.0-rc1'
    implementation 'org.slf4j:slf4j-simple:2.0.17'}
```
First the 'application' plugin is used to compile as a Java application. Secondly, the class is defined that is containing the main application. MavenCentral will be used to retrieve the dependencies are defined at the end.
The testcontainers dependency is defined because the application uses a test container to run Ollama in a container. The next two dependencies contains classes needed for the LLM chat client to communicate with the Ollama back-end. The last dependency is a log implementation that results in the request and results to be logged as configured. 

The repository that contains this application and all files needed to build and run, can be found at https://github.com/PGSmink/ai-examples-java.git.

# Build and run
After checking out this repository you can go to the root directory and build and run
the application using
```
./gradlew run
```
(use 'gradlew run' on windows)

The answers are
## Question 1: Hi what's your model and role
```
I am Dolphin, an AI model that assists in various tasks such as language translation, natural language understanding, and text generation. My primary role is to assist you with any questions or concerns you may have.
```
## Question 2: Give three German speaking countries in Europe
For this question an additional system prompt is specified to get an answer assuming you  are a history student.
```
Sure, here are three German speaking countries in Europe:

1. Germany: This is the most popular and well-known country for German language speakers. It's located in Central Europe and has a rich history of culture and art. The official language is German, but there are also many other languages spoken, such as Alsatian, Bavarian, and Sorbian.
2. Austria: Another popular German speaking country, Austria is located in Central Europe and has a rich history of music, literature, and architecture. The official language is German, but there are also many other languages spoken, such as Czech, Hungarian, and Slovene.
3. Switzerland: Switzerland is another European country known for its strong German influence. It's located in Western Europe and has a rich history of culture and art. The official language is German, but there are also many other languages spoken, such as French, Italian, and Romansh.
```

# Timing
On my Framework laptop (with intel 12th gen) it takes about
* 22 seconds to start the LLM model
* 2 seconds for the first answer
* 4 seconds for the second answer

So it takes some time to bootstrap the LLM, but the answers are given relative fast. As could be expected, adding a system prompt and asking more complex questions results in longer execution times.
The timings are with the LLM running on the CPU. If it runs in a more complex setup on a GPU, it will be much faster.

## Summary
So now you have a simple application to chat with a LLM using Java.
TinyDolphin is a simple experimental LLM model. Probable not fast enough to be used in applications, but fast enough to do these kind of experiments to get understanding how it works.

## Exercises
* Experiment with different prompts 
* Experiment with different system prompts 
* Experiment with a more advanced llama3 model, find its exact model name and check the output. 


# See also

* https://docs.langchain4j.dev
* https://docs.langchain4j.dev/integrations/language-models/ollama
* https://ollama.com/library/tinydolphin
* https://github.com/ollama/ollama
* https://java.testcontainers.org