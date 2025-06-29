package eu.smink.ai;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;

public class RequestMessageLogger implements ChatModelListener
{
    @Override
    public void onRequest(ChatModelRequestContext requestContext)
    {
        System.out.println("\n\nChat input messages:");
        requestContext.chatRequest().messages().forEach(chatMessage -> System.out.println("     " + chatMessage));
    }
}