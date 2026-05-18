package com.project.artconnect.dao;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import java.util.List;
import java.util.Optional;

public interface CommunityMemberDao {
    Optional<CommunityMember> findById(Long id);

    Optional<CommunityMember> findByName(String name);

    List<CommunityMember> findAll();

    void save(CommunityMember member);

    void update(CommunityMember member);

    void delete(String memberName);

    List<Review> findReviewsByMember(CommunityMember member);
}
