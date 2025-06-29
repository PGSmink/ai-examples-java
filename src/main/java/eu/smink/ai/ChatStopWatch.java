package eu.smink.ai;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

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