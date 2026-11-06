package com.campusconnect.repository;

import com.campusconnect.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    List<Block> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
        select count(b) > 0 from Block b
        where (b.blocker.id = :a and b.blocked.id = :b) or (b.blocker.id = :b and b.blocked.id = :a)""")
    boolean blockedEitherWay(@Param("a") Long a, @Param("b") Long b);
}
