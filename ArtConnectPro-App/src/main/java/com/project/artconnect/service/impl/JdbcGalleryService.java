package com.project.artconnect.service.impl;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link GalleryService}.
 */
public class JdbcGalleryService implements GalleryService {

    private final GalleryDao galleryDao;

    public JdbcGalleryService(GalleryDao galleryDao) {
        this.galleryDao = galleryDao;
    }

    @Override
    public List<Gallery> getAllGalleries() {
        return galleryDao.findAll();
    }

    @Override
    public Optional<Gallery> getGalleryByName(String name) {
        Optional<Gallery> direct = galleryDao.findByName(name);
        if (direct.isPresent()) {
            return direct;
        }

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        return galleryDao.findAll().stream()
                .filter(gallery -> gallery.getName() != null
                        && gallery.getName().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public List<Exhibition> getExhibitionsByGallery(Gallery gallery) {
        if (gallery == null) {
            return Collections.emptyList();
        }
        return galleryDao.findExhibitionsByGallery(gallery);
    }
}
