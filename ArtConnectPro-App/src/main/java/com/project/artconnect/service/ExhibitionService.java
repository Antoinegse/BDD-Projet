package com.project.artconnect.service;

import com.project.artconnect.model.Exhibition;
import java.util.List;
import java.util.Optional;

/**
 * Service autonome pour la gestion des expositions.
 */
public interface ExhibitionService {
    List<Exhibition> getAllExhibitions();
    Optional<Exhibition> getExhibitionByTitle(String title);
    List<Exhibition> getExhibitionsByGallery(String galleryName);
    void createExhibition(Exhibition exhibition);
    void updateExhibition(Exhibition exhibition);
    void deleteExhibition(String title);
}
