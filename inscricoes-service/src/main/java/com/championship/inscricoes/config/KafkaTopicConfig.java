package com.championship.inscricoes.config;

import com.championship.inscricoes.infrastructure.messaging.events.ChampionshipPermissionsChangedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.EnrollmentConfirmedPayload;
import com.championship.inscricoes.infrastructure.messaging.events.TeamRegisteredPayload;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic teamRegisteredTopic() {
        return TopicBuilder.name(TeamRegisteredPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic enrollmentConfirmedTopic() {
        return TopicBuilder.name(EnrollmentConfirmedPayload.TYPE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic championshipPermissionsChangedTopic() {
        return TopicBuilder.name(ChampionshipPermissionsChangedPayload.TYPE).partitions(3).replicas(1).build();
    }
}
