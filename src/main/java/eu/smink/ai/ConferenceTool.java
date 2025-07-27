package eu.smink.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * gets list of conferences, optionally for a given year
     *
     * @param year {@code year conference}
     * @return CallToolResult
     */
    @Tool("Get list of cool conference talks")
    public static McpSchema.CallToolResult getConferenceTalks(@P(value = "Year of conference", required = false) Integer year)
    {
        List<McpSchema.Content> contents = new ArrayList<>();
        talks.stream().filter(talk -> year == null || talk.year() == year)
                .map(Record::toString)
                .map(McpSchema.TextContent::new)
                .forEach(contents::add);
        return new McpSchema.CallToolResult(contents, false);
    }
}