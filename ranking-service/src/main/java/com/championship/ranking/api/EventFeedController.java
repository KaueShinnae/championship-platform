package com.championship.ranking.api;

import com.championship.ranking.application.EventFeedService;
import com.championship.ranking.application.EventFeedService.FeedEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventFeedController {

    private static final int MAX_LIMIT = 100;

    private final EventFeedService eventFeedService;

    public EventFeedController(EventFeedService eventFeedService) {
        this.eventFeedService = eventFeedService;
    }

    @GetMapping("/recent")
    public List<FeedEntry> recentes(@RequestParam(defaultValue = "20") int limit) {
        return eventFeedService.recentes(Math.clamp(limit, 1, MAX_LIMIT));
    }
}
