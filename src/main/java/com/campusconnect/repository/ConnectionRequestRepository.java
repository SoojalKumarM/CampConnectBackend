package com.campusconnect.repository;

import com.campusconnect.entity.ConnectionRequest;
import com.campusconnect.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    List<ConnectionRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, RequestStatus status);
    List<ConnectionRequest> findBySenderIdAndStatusOrderByCreatedAtDesc(Long senderId, RequestStatus status);

    long countByReceiverIdAndStatus(Long receiverId, RequestStatus status);
    long countBySenderIdAndStatus(Long senderId, RequestStatus status);
    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant after);

    @Query("""
        select r from ConnectionRequest r
        where (r.sender.id = :a and r.receiver.id = :b) or (r.sender.id = :b and r.receiver.id = :a)
        order by r.createdAt desc""")
    List<ConnectionRequest> between(@Param("a") Long a, @Param("b") Long b);

    @Query("""
        select r from ConnectionRequest r
        where r.status in :statuses and (r.sender.id = :me or r.receiver.id = :me)""")
    List<ConnectionRequest> activeFor(@Param("me") Long me, @Param("statuses") Collection<RequestStatus> statuses);

    @Query("""
        select r from ConnectionRequest r
        where r.status = :status and (r.sender.id = :me or r.receiver.id = :me)
        order by r.respondedAt desc""")
    List<ConnectionRequest> connections(@Param("me") Long me, @Param("status") RequestStatus status);

    @Query("""
        select count(r) from ConnectionRequest r
        where r.status = :status and (r.sender.id = :me or r.receiver.id = :me)""")
    long countConnections(@Param("me") Long me, @Param("status") RequestStatus status);
}
