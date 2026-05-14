package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of {@link GalleryDao}.
 */
public class JdbcGalleryDao implements GalleryDao {

    private static final String SELECT_GALLERIES_WITH_EXHIBITIONS = """
            SELECT g.Id_Gallerie, g.name, g.adress, g.rating, g.ownerName, g.openingHours, g.contactPhone, g.website,
                   e.Id_Exhibition, e.title AS exhibition_title, e.start_date, e.end_date, e.theme,
                   e.description AS exhibition_description, e.curatorName
            FROM Gallerie g
            LEFT JOIN Exhibition e ON e.Id_Gallerie = g.Id_Gallerie
            """;

    @Override
    public Optional<Gallery> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        String sql = SELECT_GALLERIES_WITH_EXHIBITIONS + " WHERE g.Id_Gallerie = ? ORDER BY e.start_date";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Gallery> galleriesById = mapGalleries(resultSet);
                return galleriesById.values().stream().findFirst();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find gallery by id: " + id, exception);
        }
    }

    @Override
    public Optional<Gallery> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String sql = SELECT_GALLERIES_WITH_EXHIBITIONS + " WHERE g.name = ? ORDER BY e.start_date";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Gallery> galleriesById = mapGalleries(resultSet);
                return galleriesById.values().stream().findFirst();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find gallery by name: " + name, exception);
        }
    }

    @Override
    public List<Gallery> findAll() {
        String sql = SELECT_GALLERIES_WITH_EXHIBITIONS + " ORDER BY g.name, e.start_date";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            Map<Integer, Gallery> galleriesById = mapGalleries(resultSet);
            return new ArrayList<>(galleriesById.values());
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load galleries.", exception);
        }
    }

    @Override
    public void save(Gallery gallery) {
        Objects.requireNonNull(gallery, "gallery must not be null");

        String sql = """
                INSERT INTO Gallerie(name, adress, rating, ownerName, openingHours, contactPhone, website)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, gallery.getName());
            statement.setString(2, gallery.getAddress());
            statement.setDouble(3, gallery.getRating());
            statement.setString(4, gallery.getOwnerName());
            statement.setString(5, gallery.getOpeningHours());
            statement.setString(6, gallery.getContactPhone());
            statement.setString(7, gallery.getWebsite());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save gallery: " + gallery.getName(), exception);
        }
    }

    @Override
    public void update(Gallery gallery) {
        Objects.requireNonNull(gallery, "gallery must not be null");

        String sql = """
                UPDATE Gallerie
                SET adress = ?, rating = ?, ownerName = ?, openingHours = ?, contactPhone = ?, website = ?
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gallery.getAddress());
            statement.setDouble(2, gallery.getRating());
            statement.setString(3, gallery.getOwnerName());
            statement.setString(4, gallery.getOpeningHours());
            statement.setString(5, gallery.getContactPhone());
            statement.setString(6, gallery.getWebsite());
            statement.setString(7, gallery.getName());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update gallery: " + gallery.getName(), exception);
        }
    }

    @Override
    public void delete(String galleryName) {
        if (galleryName == null || galleryName.isBlank()) {
            return;
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer galleryId = findGalleryId(connection, galleryName);
                if (galleryId != null) {
                    List<Integer> exhibitionIds = findExhibitionIdsByGalleryId(connection, galleryId);
                    for (Integer exhibitionId : exhibitionIds) {
                        try (PreparedStatement clearArtworkExhibition = connection.prepareStatement(
                                "UPDATE Artwork SET Id_Exhibition = NULL WHERE Id_Exhibition = ?")) {
                            clearArtworkExhibition.setInt(1, exhibitionId);
                            clearArtworkExhibition.executeUpdate();
                        }
                    }

                    try (PreparedStatement deleteExhibitions = connection.prepareStatement(
                            "DELETE FROM Exhibition WHERE Id_Gallerie = ?")) {
                        deleteExhibitions.setInt(1, galleryId);
                        deleteExhibitions.executeUpdate();
                    }
                }

                try (PreparedStatement deleteGallery = connection.prepareStatement(
                        "DELETE FROM Gallerie WHERE name = ?")) {
                    deleteGallery.setString(1, galleryName);
                    deleteGallery.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete gallery: " + galleryName, exception);
        }
    }

    @Override
    public List<Exhibition> findExhibitionsByGallery(Gallery gallery) {
        if (gallery == null || gallery.getName() == null || gallery.getName().isBlank()) {
            return List.of();
        }
        return findByName(gallery.getName())
                .map(Gallery::getExhibitions)
                .orElse(List.of());
    }

    private Map<Integer, Gallery> mapGalleries(ResultSet resultSet) throws SQLException {
        Map<Integer, Gallery> galleriesById = new LinkedHashMap<>();

        while (resultSet.next()) {
            int galleryId = resultSet.getInt("Id_Gallerie");
            Gallery gallery = galleriesById.get(galleryId);
            if (gallery == null) {
                gallery = new Gallery(
                        resultSet.getString("name"),
                        resultSet.getString("adress"),
                        resultSet.getDouble("rating")
                );
                gallery.setOwnerName(resultSet.getString("ownerName"));
                gallery.setOpeningHours(resultSet.getString("openingHours"));
                gallery.setContactPhone(resultSet.getString("contactPhone"));
                gallery.setWebsite(resultSet.getString("website"));
                galleriesById.put(galleryId, gallery);
            }

            int exhibitionId = resultSet.getInt("Id_Exhibition");
            if (!resultSet.wasNull()) {
                Exhibition exhibition = mapExhibition(resultSet, gallery);
                gallery.getExhibitions().add(exhibition);
            }
        }

        return galleriesById;
    }

    private Exhibition mapExhibition(ResultSet resultSet, Gallery gallery) throws SQLException {
        Exhibition exhibition = new Exhibition(
                resultSet.getString("exhibition_title"),
                toLocalDate(resultSet.getDate("start_date")),
                toLocalDate(resultSet.getDate("end_date")),
                gallery
        );
        exhibition.setTheme(resultSet.getString("theme"));
        exhibition.setDescription(resultSet.getString("exhibition_description"));
        exhibition.setCuratorName(resultSet.getString("curatorName"));
        return exhibition;
    }

    private Integer findGalleryId(Connection connection, String galleryName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_Gallerie FROM Gallerie WHERE name = ? LIMIT 1")) {
            statement.setString(1, galleryName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Gallerie");
                }
            }
        }
        return null;
    }

    private List<Integer> findExhibitionIdsByGalleryId(Connection connection, int galleryId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_Exhibition FROM Exhibition WHERE Id_Gallerie = ?")) {
            statement.setInt(1, galleryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("Id_Exhibition"));
                }
            }
        }
        return ids;
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
