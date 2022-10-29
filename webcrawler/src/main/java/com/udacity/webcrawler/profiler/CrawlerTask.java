package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class CrawlerTask extends RecursiveAction {

    private String url;
    private Clock clock;
    private Instant deadLine;
    private int maxDepth;
    private PageParserFactory pageParserFactory;
    private List<Pattern> ignoredUrls;
    private Map<String, Integer> counts;
    private Set<String> visitedUrls;
    public CrawlerTask(String url, Clock clock , Instant deadLine, int maxDepth, PageParserFactory pageParserFactory,
                       List<Pattern> ignoredUrls, Map<String, Integer> counts,Set<String> visitedUrls){
        this.url=url;
        this.clock=clock;
        this.deadLine=deadLine;
        this.maxDepth=maxDepth;
        this.pageParserFactory=pageParserFactory;
        this.ignoredUrls=ignoredUrls;
        this.counts=counts;
        this.visitedUrls=visitedUrls;

    }

    @Override
    protected void compute() {
        if(maxDepth>0&&clock.instant().isBefore(deadLine)){
            List<String> subLinks= getSublink(url);
            List<CrawlerTask> taskList = createTask(subLinks);
            ForkJoinTask.invokeAll(taskList);
        }

    }

    private List<String> getSublink(String suburl) {
        List<String> sbuLinkss = new ArrayList<>();
        if(maxDepth==0||clock.instant().isAfter(deadLine)){
            return sbuLinkss;
        }
        for(Pattern pattern: ignoredUrls){
            if(pattern.matcher(suburl).matches()){
                return sbuLinkss;

            }
        }
        if(visitedUrls.contains(suburl)){
            return sbuLinkss;

        }visitedUrls.add(suburl);
        PageParser.Result result = pageParserFactory.get(suburl).parse();
        for(Map.Entry<String,Integer> e: result.getWordCounts().entrySet()){
            if(counts.containsKey(e.getKey())){
                counts.put(e.getKey(),e.getValue()+counts.get(e.getKey()));
            }else {
                counts.put(e.getKey(), e.getValue());
            }
        }
        sbuLinkss.addAll(result.getLinks());
        return sbuLinkss;
    }

    private List<CrawlerTask> createTask(List<String> subLinks) {
        List<CrawlerTask> taskList= new ArrayList<>();
        for (String suburl: subLinks){
            CrawlerTask crawlerTask= new CrawlerTask(suburl,clock,deadLine,maxDepth-1,pageParserFactory,ignoredUrls,counts,visitedUrls);
            taskList.add(crawlerTask);

        }
        return taskList;
    }
}
