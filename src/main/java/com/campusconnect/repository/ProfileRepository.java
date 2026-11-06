package com.campusconnect.repository;

import com.campusconnect.entity.Gender;
import com.campusconnect.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    @Query("""
        select p from Profile p join p.user u
        where u.id <> :me and u.active = true and u.profileComplete = true and p.visible = true
          and (:gender is null or p.gender = :gender)
          and (:university is null or lower(p.university) like lower(concat('%', :university, '%')))
          and (:department is null or lower(p.department) like lower(concat('%', :department, '%')))
          and (:year is null or p.yearOfStudy = :year)
          and (:interest is null or :interest member of p.interests)
          and (:q is null or lower(p.fullName) like lower(concat('%', :q, '%'))
                          or lower(p.bio) like lower(concat('%', :q, '%')))
          and u.id not in (select b.blocked.id from Block b where b.blocker.id = :me)
          and u.id not in (select b.blocker.id from Block b where b.blocked.id = :me)
        """)
    Page<Profile> discover(@Param("me") Long me, @Param("gender") Gender gender,
                           @Param("university") String university, @Param("department") String department,
                           @Param("year") Integer year, @Param("interest") String interest,
                           @Param("q") String q, Pageable pageable);

    @Query("select p from Profile p where p.user.id in :ids")
    List<Profile> findAllByUserIds(@Param("ids") List<Long> ids);
}
