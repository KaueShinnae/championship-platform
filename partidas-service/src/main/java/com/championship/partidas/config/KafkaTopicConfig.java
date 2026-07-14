package com.championship.partidas.config;

import com.championship.partidas.infrastructure.messaging.events.ChampionshipCompletedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchFinishedPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchScheduledPayload;
import com.championship.partidas.infrastructure.messaging.events.MatchStartedPayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic matchScheduledTopic() {
        return TopicBuilder.name(MatchScheduledPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic matchStartedTopic() {
        return TopicBuilder.name(MatchStartedPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic matchFinishedTopic() {
        return TopicBuilder.name(MatchFinishedPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic championshipCompletedTopic() {
        return TopicBuilder.name(ChampionshipCompletedPayload.TYPE).partitions(3).replicas(1).build();
    }
}
