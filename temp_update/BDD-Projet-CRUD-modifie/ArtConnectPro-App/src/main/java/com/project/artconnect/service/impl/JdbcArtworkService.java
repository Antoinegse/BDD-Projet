package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtworkService;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC-backed implementation of {@link ArtworkService}.
 */
public class JdbcArtworkService implements ArtworkService {

    private final ArtworkDao artworkDao;

    public JdbcArtworkService(ArtworkDao artworkDao) {
        this.artworkDao = artworkDao;
    }

    @Override
    public List<Artwork> getAllArtworks() {
        return artworkDao.findAll();
    }

    @Override
    public Optional<Artwork> getArtworkByTitle(String title) {
        Optional<Artwork> direct = artworkDao.findByTitle(title);
        if (direct.isPresent()) {
            return direct;
        }

        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        return artworkDao.findAll().stream()
                .filter(artwork -> artwork.getTitle() != null
                        && artwork.getTitle().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public List<Artwork> getArtworksByArtist(Artist artist) {
        if (artist == null || artist.getName() == null || artist.getName().isBlank()) {
            return Collections.emptyList();
        }
        return artworkDao.findByArtistName(artist.getName());
    }

    @Override
    public void createArtwork(Artwork artwork) {
        artworkDao.save(artwork);
    }

    @Override
    public void updateArtwork(Artwork artwork) {
        artworkDao.update(artwork);
    }

    @Override
    public void deleteArtwork(String title) {
        artworkDao.delete(title);
    }
}
