package com.championship.ranking.api;

import com.championship.ranking.api.StandingsDtos.GroupStandingsResponse;
import com.championship.ranking.application.RankingProjectionService;
import com.championship.ranking.domain.GroupStanding;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class StandingsController {

    private final RankingProjectionService rankingProjectionService;

    public StandingsController(RankingProjectionService rankingProjectionService) {
        this.rankingProjectionService = rankingProjectionService;
    }

    @GetMapping("/{groupId}/standings")
    public ResponseEntity<GroupStandingsResponse> standings(@PathVariable UUID groupId) {
        List<GroupStanding> standings = rankingProjectionService.classificacaoDoGrupo(groupId);
        if (standings.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(GroupStandingsResponse.from(groupId, standings));
    }
}
