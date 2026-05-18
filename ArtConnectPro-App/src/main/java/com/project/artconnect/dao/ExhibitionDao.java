package com.project.artconnect.dao;

import com.project.artconnect.model.Exhibition;
import java.util.List;
import java.util.Optional;

public interface ExhibitionDao {
    List<Exhibition> findAll();

    Optional<Exhibition> findByTitle(String title);

    void save(Exhibition exhibition);

    void update(Exhibition exhibition);

    void delete(String title);

    List<Exhibition> findByGalleryName(String galleryName);
}
