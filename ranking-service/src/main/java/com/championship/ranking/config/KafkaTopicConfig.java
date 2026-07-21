package com.championship.ranking.config;

import com.championship.ranking.infrastructure.messaging.events.MatchResultCorrectedPayload;
import com.championship.ranking.infrastructure.messaging.events.RankingUpdatedPayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic rankingUpdatedTopic() {
        return TopicBuilder.name(RankingUpdatedPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic matchResultCorrectedTopic() {
        return TopicBuilder.name(MatchResultCorrectedPayload.TYPE).partitions(3).replicas(1).build();
    }
}
