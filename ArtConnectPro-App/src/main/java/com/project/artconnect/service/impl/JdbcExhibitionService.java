package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.service.ExhibitionService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link ExhibitionService}.
 */
public class JdbcExhibitionService implements ExhibitionService {

    private final ExhibitionDao exhibitionDao;

    public JdbcExhibitionService(ExhibitionDao exhibitionDao) {
        this.exhibitionDao = exhibitionDao;
    }

    @Override
    public List<Exhibition> getAllExhibitions() {
        return exhibitionDao.findAll();
    }

    @Override
    public Optional<Exhibition> getExhibitionByTitle(String title) {
        Optional<Exhibition> direct = exhibitionDao.findByTitle(title);
        if (direct.isPresent()) {
            return direct;
        }

        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        String normalized = title.toLowerCase(Locale.ROOT);
        return exhibitionDao.findAll().stream()
                .filter(e -> e.getTitle() != null
                        && e.getTitle().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public List<Exhibition> getExhibitionsByGalleryName(String galleryName) {
        if (galleryName == null || galleryName.isBlank()) {
            return List.of();
        }
        return exhibitionDao.findByGalleryName(galleryName);
    }

    @Override
    public void createExhibition(Exhibition exhibition) {
        exhibitionDao.save(exhibition);
    }

    @Override
    public void updateExhibition(Exhibition exhibition) {
        exhibitionDao.update(exhibition);
    }

    @Override
    public void deleteExhibition(String title) {
        exhibitionDao.delete(title);
    }
}
