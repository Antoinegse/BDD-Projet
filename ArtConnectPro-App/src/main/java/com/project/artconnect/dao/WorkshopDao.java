package com.project.artconnect.dao;

import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import java.util.List;
import java.util.Optional;

public interface WorkshopDao {
    Optional<Workshop> findById(Long id);

    Optional<Workshop> findByTitle(String title);

    List<Workshop> findAll();

    void save(Workshop workshop);

    void update(Workshop workshop);

    void delete(String title);

    void createBooking(Workshop workshop, CommunityMember member);

    List<Booking> findBookingsByMember(CommunityMember member);
}
