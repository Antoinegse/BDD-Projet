package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC-backed implementation of {@link ArtistService}.
 */
public class JdbcArtistService implements ArtistService {

    private final ArtistDao artistDao;

    public JdbcArtistService(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    @Override
    public Optional<Artist> getArtistByName(String name) {
        Optional<Artist> direct = artistDao.findByName(name);
        if (direct.isPresent()) {
            return direct;
        }

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String normalized = name.toLowerCase(Locale.ROOT);
        return artistDao.findAll().stream()
                .filter(artist -> artist.getName() != null
                        && artist.getName().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    @Override
    public void createArtist(Artist artist) {
        artistDao.save(artist);
    }

    @Override
    public void updateArtist(Artist artist) {
        artistDao.update(artist);
    }

    @Override
    public void deleteArtist(String name) {
        artistDao.delete(name);
    }

    @Override
    public List<Discipline> getAllDisciplines() {
        List<Discipline> persisted = artistDao.findAllDisciplines();
        if (!persisted.isEmpty()) {
            return persisted;
        }

        Map<String, Discipline> byName = new LinkedHashMap<>();
        for (Artist artist : artistDao.findAll()) {
            if (artist.getDisciplines() == null) {
                continue;
            }
            for (Discipline discipline : artist.getDisciplines()) {
                if (discipline != null && discipline.getName() != null && !discipline.getName().isBlank()) {
                    byName.putIfAbsent(discipline.getName(), new Discipline(discipline.getName()));
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        List<Artist> source = (city == null || city.isBlank())
                ? artistDao.findAll()
                : artistDao.findByCity(city);

        String normalizedQuery = query == null ? null : query.toLowerCase(Locale.ROOT).trim();
        String normalizedDiscipline = disciplineName == null ? null : disciplineName.toLowerCase(Locale.ROOT).trim();
        String normalizedCity = city == null ? null : city.toLowerCase(Locale.ROOT).trim();

        return source.stream()
                .filter(artist -> normalizedQuery == null || normalizedQuery.isBlank()
                        || (artist.getName() != null
                        && artist.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .filter(artist -> normalizedCity == null || normalizedCity.isBlank()
                        || (artist.getCity() != null
                        && artist.getCity().toLowerCase(Locale.ROOT).equals(normalizedCity)))
                .filter(artist -> normalizedDiscipline == null || normalizedDiscipline.isBlank()
                        || artist.getDisciplines().stream().anyMatch(discipline ->
                        discipline.getName() != null
                                && discipline.getName().toLowerCase(Locale.ROOT).equals(normalizedDiscipline)))
                .collect(Collectors.toList());
    }
}
