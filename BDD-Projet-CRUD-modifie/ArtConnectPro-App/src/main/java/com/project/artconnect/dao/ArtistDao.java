package com.project.artconnect.dao;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import java.util.List;
import java.util.Optional;

public interface ArtistDao {

    List<Artist> findAll();

    Optional<Artist> findByName(String name);

    void save(Artist artist);

    void update(Artist artist);

    void delete(String artistName);

    List<Artist> findByCity(String city);

    List<Discipline> findAllDisciplines();
}
