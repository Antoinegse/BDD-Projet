package com.project.artconnect.dao;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Exhibition;
import java.util.List;
import java.util.Optional;

public interface GalleryDao {
    Optional<Gallery> findById(Long id);

    Optional<Gallery> findByName(String name);

    List<Gallery> findAll();

    void save(Gallery gallery);

    void update(Gallery gallery);

    void delete(String galleryName);

    List<Exhibition> findExhibitionsByGallery(Gallery gallery);
}
