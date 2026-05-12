package com.project.artconnect.dao;

import com.project.artconnect.model.Artist;
import java.util.List;

public interface ArtistDao {

    List<Artist> findAll();

    void save(Artist artist);

    void update(Artist artist);

    void delete(String artistName);

    List<Artist> findByCity(String city);
}