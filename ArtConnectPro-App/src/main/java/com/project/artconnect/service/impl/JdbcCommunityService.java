package com.project.artconnect.service.impl;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.CommunityService;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link CommunityService}.
 */
public class JdbcCommunityService implements CommunityService {

    private final CommunityMemberDao communityMemberDao;

    public JdbcCommunityService(CommunityMemberDao communityMemberDao) {
        this.communityMemberDao = communityMemberDao;
    }

    @Override
    public List<CommunityMember> getAllMembers() {
        return communityMemberDao.findAll();
    }

    @Override
    public Optional<CommunityMember> getMemberByName(String name) {
        Optional<CommunityMember> direct = communityMemberDao.findByName(name);
        if (direct.isPresent()) {
            return direct;
        }

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        return communityMemberDao.findAll().stream()
                .filter(member -> member.getName() != null
                        && member.getName().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public List<Review> getReviewsByMember(CommunityMember member) {
        if (member == null) {
            return Collections.emptyList();
        }
        return communityMemberDao.findReviewsByMember(member);
    }
}
