package eu.smink.ai;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
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
        System.out.println();
        System.out.println("Total input tokens       : " + getTotalNrOfInputTokens());
        System.out.println("Total output tokens      : " + getTotalNrOfOutputTokens());
        System.out.println("Total tokens             : " + getTotalNrTokens());
        System.out.printf("Total input token costs  : %.04f %s\n", getTotalInputCosts(), currencyInCents);
        System.out.printf("Total output token costs : %.04f %s\n", getTotalOutputCosts(), currencyInCents);
        System.out.printf("Total token costs        : %.04f %s\n", getTotalCosts(), currencyInCents);
        System.out.println("Total calls              : " + getTotalNrOfCalls());
    }
}