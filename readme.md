
# Getting started with AI using Ollama and langchain4j on any laptop, part2


In branch simple-container an application is given that uses testcontainer to start a container to run Ollama and load a LLM automatically at the start of the application.
This is great for the 'it works out of the box' experience. 

However, if you start the application multiple times, each run you've to wait for the model to be loaded what can be annoying.

We are going to change this:
The solution is to install Ollama and the model only once.

## Install Ollama

First you need to install Ollama locally. At download Ollama (https://ollama.com/download) you will find how to do this.
Currently, for Linux the installation is:
```
curl -fsSL https://ollama.com/install.sh | sh
```
(Install curl first, if it's not installed)

## Install LLM in Ollama
Our application uses the tinydolphin. this can be installed and run using
```
ollama run tinydolphin
```
This will download the LLM and start a server on end point http://localhost:11434. You can keep this server up and running as long as you want to use it.
The next time you repeat this command, it will be faster because the LLM is already downloaded on your system.

During the installation of Ollama it was detected what GPU is installed and the amount of memory it has. At the start of the tinydolphin LLM Ollama will decide whether the GPU is used automatically.

## Change to application
The application must be updated to remove testcontainer code and use http://localhost:11434 as end point. 

The application becomes as follows:

```
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
```

## Gradle build script
The updated Gradle build file is as follows:

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
    implementation 'dev.langchain4j:langchain4j-core:1.1.0'
    implementation 'dev.langchain4j:langchain4j-ollama:1.1.0-rc1'
    implementation 'org.slf4j:slf4j-simple:2.0.17'}
```
## Source code
The repository that contains this application and all files needed to build and run, can be found at https://github.com/PGSmink/ai-examples-java.git, 
checkout branch **simple-container-external-llm**.

# Build and run
After checking out this repository and switching branch, you can go to the root directory and build and run
the application using
```
./gradlew run
```
(do not forget to start the LLM before running the application)

## Performance
The execution times for the new application will be similar to the previous, except the delay at the start to load the LLM is gone.
 
You might wonder, is your LLM running on the GPU or CPU? After you've run the application once, you can run
```
ollama ps
```
to see how it is running.
For me the output is
```
NAME                  ID              SIZE      PROCESSOR    UNTIL              
tinydolphin:latest    0f9dd11f824c    1.4 GB    100% CPU     4 minutes from now    
```
So it's running on the CPU for 100%.

The download size is not the same at size at runtime.

Ollama does pause an LLM if it's not used for some time. If the ps output is empty, you need to run the application again.

## Summary
Now you have to start Ollama with the used LLM manually, before the first run of the application. This saves considerable time when experimenting.

## Exercises
You can run also different LLMs, see https://ollama.com/search. Small models most likely do work on your laptop. If the LLM is large,
* you need to have free memory to load it
* you need to have a GPU with sufficient graphical memory for most large models in order to get answers in reasonable time.


# See also

* https://docs.langchain4j.dev
* https://docs.langchain4j.dev/integrations/language-models/ollama
* https://ollama.com/library/tinydolphin
* https://github.com/ollama/ollama
* https://ollama.com/search for models