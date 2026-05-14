package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link WorkshopService}.
 */
public class JdbcWorkshopService implements WorkshopService {

    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        Optional<Workshop> direct = workshopDao.findByTitle(title);
        if (direct.isPresent()) {
            return direct;
        }

        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        return workshopDao.findAll().stream()
                .filter(workshop -> workshop.getTitle() != null
                        && workshop.getTitle().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || member == null) {
            return;
        }
        workshopDao.createBooking(workshop, member);
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null) {
            return Collections.emptyList();
        }
        return workshopDao.findBookingsByMember(member);
    }
}
