package com.championship.ranking.infrastructure.persistence;

import com.championship.ranking.domain.GroupStanding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupStandingRepository extends JpaRepository<GroupStanding, UUID> {

    Optional<GroupStanding> findByGroupIdAndTeamId(UUID groupId, UUID teamId);

    List<GroupStanding> findByGroupIdOrderByPointsDesc(UUID groupId);
}
