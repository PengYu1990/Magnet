package com.pengyu.magnet.repository.match;

import com.pengyu.magnet.domain.match.MatchingIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchingIndexRepository extends JpaRepository<MatchingIndex, Long> {
    Optional<MatchingIndex> findByJobIdAndResumeId(Long jobId, Long resumeId);
}
