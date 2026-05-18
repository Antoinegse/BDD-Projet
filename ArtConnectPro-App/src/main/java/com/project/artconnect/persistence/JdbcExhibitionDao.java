package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of {@link ExhibitionDao}.
 */
public class JdbcExhibitionDao implements ExhibitionDao {

    private static final String EXHIBITION_SELECT = """
            SELECT e.Id_Exhibition, e.title, e.start_date, e.end_date, e.theme, e.description, e.curatorName,
                   g.Id_Gallerie, g.name AS gallery_name, g.adress, g.rating, g.ownerName, g.openingHours, g.contactPhone, g.website
            FROM Exhibition e
            JOIN Gallerie g ON g.Id_Gallerie = e.Id_Gallerie
            """;

    @Override
    public List<Exhibition> findAll() {
        return fetchExhibitions("", statement -> {
        });
    }

    @Override
    public Optional<Exhibition> findByTitle(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        return fetchExhibitions("WHERE e.title = ?", statement -> statement.setString(1, title)).stream().findFirst();
    }

    @Override
    public void save(Exhibition exhibition) {
        Objects.requireNonNull(exhibition, "exhibition must not be null");

        String sql = """
                INSERT INTO Exhibition(title, start_date, end_date, Id_Gallerie, theme, description, curatorName)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int galleryId = resolveGalleryId(connection, exhibition.getGallery());
            statement.setString(1, exhibition.getTitle());
            statement.setDate(2, toSqlDate(exhibition.getStartDate()));
            statement.setDate(3, toSqlDate(exhibition.getEndDate()));
            statement.setInt(4, galleryId);
            statement.setString(5, exhibition.getTheme());
            statement.setString(6, exhibition.getDescription());
            statement.setString(7, exhibition.getCuratorName());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save exhibition: " + exhibition.getTitle(), exception);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        Objects.requireNonNull(exhibition, "exhibition must not be null");

        String sql = """
                UPDATE Exhibition
                SET start_date = ?, end_date = ?, Id_Gallerie = ?, theme = ?, description = ?, curatorName = ?
                WHERE title = ?
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int galleryId = resolveGalleryId(connection, exhibition.getGallery());
            statement.setDate(1, toSqlDate(exhibition.getStartDate()));
            statement.setDate(2, toSqlDate(exhibition.getEndDate()));
            statement.setInt(3, galleryId);
            statement.setString(4, exhibition.getTheme());
            statement.setString(5, exhibition.getDescription());
            statement.setString(6, exhibition.getCuratorName());
            statement.setString(7, exhibition.getTitle());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update exhibition: " + exhibition.getTitle(), exception);
        }
    }

    @Override
    public void delete(String title) {
        if (title == null || title.isBlank()) {
            return;
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Integer> exhibitionIds = findExhibitionIdsByTitle(connection, title);
                for (Integer exhibitionId : exhibitionIds) {
                    try (PreparedStatement clearArtworkLink = connection.prepareStatement(
                            "UPDATE Artwork SET Id_Exhibition = NULL WHERE Id_Exhibition = ?")) {
                        clearArtworkLink.setInt(1, exhibitionId);
                        clearArtworkLink.executeUpdate();
                    }
                }

                try (PreparedStatement deleteExhibition = connection.prepareStatement(
                        "DELETE FROM Exhibition WHERE title = ?")) {
                    deleteExhibition.setString(1, title);
                    deleteExhibition.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete exhibition: " + title, exception);
        }
    }

    @Override
    public List<Exhibition> findByGalleryName(String galleryName) {
        if (galleryName == null || galleryName.isBlank()) {
            return List.of();
        }
        return fetchExhibitions("WHERE g.name = ?", statement -> statement.setString(1, galleryName));
    }

    private List<Exhibition> fetchExhibitions(String whereClause, StatementConfigurer configurer) {
        String sql = EXHIBITION_SELECT + " " + whereClause + " ORDER BY e.start_date, e.title";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Gallery> galleriesById = new LinkedHashMap<>();
                Map<Integer, Exhibition> exhibitionsById = new LinkedHashMap<>();

                while (resultSet.next()) {
                    int galleryId = resultSet.getInt("Id_Gallerie");
                    Gallery gallery = galleriesById.computeIfAbsent(galleryId, ignored -> mapGallery(resultSet));

                    int exhibitionId = resultSet.getInt("Id_Exhibition");
                    Exhibition exhibition = exhibitionsById.get(exhibitionId);
                    if (exhibition == null) {
                        exhibition = mapExhibition(resultSet, gallery);
                        exhibitionsById.put(exhibitionId, exhibition);
                        gallery.getExhibitions().add(exhibition);
                    }
                }

                loadArtworks(connection, exhibitionsById);
                return new ArrayList<>(exhibitionsById.values());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load exhibitions.", exception);
        }
    }

    private Gallery mapGallery(ResultSet resultSet) {
        try {
            Gallery gallery = new Gallery(
                    resultSet.getString("gallery_name"),
                    resultSet.getString("adress"),
                    resultSet.getDouble("rating")
            );
            gallery.setOwnerName(resultSet.getString("ownerName"));
            gallery.setOpeningHours(resultSet.getString("openingHours"));
            gallery.setContactPhone(resultSet.getString("contactPhone"));
            gallery.setWebsite(resultSet.getString("website"));
            return gallery;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map gallery.", exception);
        }
    }

    private Exhibition mapExhibition(ResultSet resultSet, Gallery gallery) {
        try {
            Exhibition exhibition = new Exhibition(
                    resultSet.getString("title"),
                    toLocalDate(resultSet.getDate("start_date")),
                    toLocalDate(resultSet.getDate("end_date")),
                    gallery
            );
            exhibition.setTheme(resultSet.getString("theme"));
            exhibition.setDescription(resultSet.getString("description"));
            exhibition.setCuratorName(resultSet.getString("curatorName"));
            return exhibition;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map exhibition.", exception);
        }
    }

    private void loadArtworks(Connection connection, Map<Integer, Exhibition> exhibitionsById) throws SQLException {
        if (exhibitionsById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT aw.Id_Artwork, aw.title, aw.creationYear, aw.type, aw.price, aw.status,
                       aw.medium, aw.dimensions, aw.description, aw.Id_Exhibition,
                       a.Id_Artist, a.name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
                FROM Artwork aw
                JOIN Artist a ON a.Id_Artist = aw.Id_Artist
                WHERE aw.Id_Exhibition IS NOT NULL
                ORDER BY aw.Id_Exhibition, aw.title
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            Map<Integer, Artist> artistsById = new LinkedHashMap<>();
            while (resultSet.next()) {
                int exhibitionId = resultSet.getInt("Id_Exhibition");
                Exhibition exhibition = exhibitionsById.get(exhibitionId);
                if (exhibition == null) {
                    continue;
                }

                int artistId = resultSet.getInt("Id_Artist");
                Artist artist = artistsById.computeIfAbsent(artistId, ignored -> {
                    Artist mappedArtist = new Artist(
                            safeString(resultSet, "name"),
                            safeString(resultSet, "bio"),
                            safeInt(resultSet, "birthyear"),
                            safeString(resultSet, "contactEmail"),
                            safeString(resultSet, "city")
                    );
                    mappedArtist.setActive(safeBoolean(resultSet, "isActive"));
                    return mappedArtist;
                });

                Artwork artwork = new Artwork(
                        resultSet.getString("title"),
                        resultSet.getInt("creationYear"),
                        resultSet.getString("type"),
                        resultSet.getDouble("price"),
                        artist
                );
                artwork.setStatus(parseStatus(resultSet.getString("status")));
                artwork.setMedium(resultSet.getString("medium"));
                artwork.setDimensions(resultSet.getString("dimensions"));
                artwork.setDescription(resultSet.getString("description"));

                artist.getArtworks().add(artwork);
                exhibition.getArtworks().add(artwork);
            }
        }
    }

    private int resolveGalleryId(Connection connection, Gallery gallery) throws SQLException {
        if (gallery == null || gallery.getName() == null || gallery.getName().isBlank()) {
            throw new SQLException("Exhibition gallery is required.");
        }

        String byNameAndAddress = "SELECT Id_Gallerie FROM Gallerie WHERE name = ? AND adress = ? LIMIT 1";
        if (gallery.getAddress() != null && !gallery.getAddress().isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(byNameAndAddress)) {
                statement.setString(1, gallery.getName());
                statement.setString(2, gallery.getAddress());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_Gallerie");
                    }
                }
            }
        }

        String byName = "SELECT Id_Gallerie FROM Gallerie WHERE name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(byName)) {
            statement.setString(1, gallery.getName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Gallerie");
                }
            }
        }
        throw new SQLException("Gallery not found in database: " + gallery.getName());
    }

    private List<Integer> findExhibitionIdsByTitle(Connection connection, String title) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_Exhibition FROM Exhibition WHERE title = ?")) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("Id_Exhibition"));
                }
            }
        }
        return ids;
    }

    private Date toSqlDate(java.time.LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private Artwork.Status parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return Artwork.Status.FOR_SALE;
        }
        try {
            return Artwork.Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Artwork.Status.FOR_SALE;
        }
    }

    private String safeString(ResultSet resultSet, String column) {
        try {
            return resultSet.getString(column);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to read column: " + column, exception);
        }
    }

    private int safeInt(ResultSet resultSet, String column) {
        try {
            int value = resultSet.getInt(column);
            return resultSet.wasNull() ? 0 : value;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to read column: " + column, exception);
        }
    }

    private boolean safeBoolean(ResultSet resultSet, String column) {
        try {
            return resultSet.getBoolean(column);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to read column: " + column, exception);
        }
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
