package com.championship.ranking.api;

import com.championship.ranking.domain.GroupStanding;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Contrato do endpoint GET /groups/{groupId}/standings — é o mesmo formato
 * que o mcp-agent-service consome (rankingServiceClient.ts). Mudança
 * incompatível aqui quebra a tool get_group_standings.
 */
public class StandingsDtos {

    public record StandingEntry(
            UUID teamId,
            String teamName,
            int points,
            int wins,
            int draws,
            int losses,
            int goalsFor,
            int goalsAgainst
    ) {
        public static StandingEntry from(GroupStanding standing) {
            return new StandingEntry(
                    standing.getTeamId(),
                    standing.getTeamName(),
                    standing.getPoints(),
                    standing.getWins(),
                    standing.getDraws(),
                    standing.getLosses(),
                    standing.getGoalsFor(),
                    standing.getGoalsAgainst());
        }
    }

    public record GroupStandingsResponse(
            UUID groupId,
            Instant updatedAt,
            List<StandingEntry> standings
    ) {
        public static GroupStandingsResponse from(UUID groupId, List<GroupStanding> standings) {
            Instant updatedAt = standings.stream()
                    .map(GroupStanding::getUpdatedAt)
                    .max(Comparator.naturalOrder())
                    .orElse(Instant.EPOCH);
            return new GroupStandingsResponse(groupId, updatedAt,
                    standings.stream().map(StandingEntry::from).toList());
        }
    }

    private StandingsDtos() {
    }
}
