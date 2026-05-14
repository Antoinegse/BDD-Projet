package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.ArtworkTag;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC implementation of {@link ArtworkDao}.
 */
public class JdbcArtworkDao implements ArtworkDao {

    private static final String SELECT_ARTWORKS = """
            SELECT aw.Id_Artwork, aw.title, aw.creationYear, aw.type, aw.price, aw.status,
                   aw.medium, aw.dimensions, aw.description,
                   a.Id_Artist, a.name AS artist_name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
            FROM Artwork aw
            JOIN Artist a ON a.Id_Artist = aw.Id_Artist
            """;

    @Override
    public List<Artwork> findAll() {
        return findByWhereClause("", statement -> {
        });
    }

    @Override
    public Optional<Artwork> findByTitle(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        List<Artwork> artworks = findByWhereClause("WHERE aw.title = ?", statement -> statement.setString(1, title));
        return artworks.stream().findFirst();
    }

    @Override
    public void save(Artwork artwork) {
        Objects.requireNonNull(artwork, "artwork must not be null");

        String insertSql = """
                INSERT INTO Artwork(title, creationYear, type, price, Id_Artist, status, medium, dimensions, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                int artistId = resolveArtistId(connection, artwork.getArtist());

                statement.setString(1, artwork.getTitle());
                if (artwork.getCreationYear() == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, artwork.getCreationYear());
                }
                statement.setString(3, artwork.getType());
                statement.setDouble(4, artwork.getPrice());
                statement.setInt(5, artistId);
                statement.setString(6, toDatabaseStatus(artwork.getStatus()));
                statement.setString(7, artwork.getMedium());
                statement.setString(8, artwork.getDimensions());
                statement.setString(9, artwork.getDescription());
                statement.executeUpdate();

                int artworkId = extractGeneratedId(statement, "artwork");
                syncTags(connection, artworkId, artwork.getTags());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save artwork: " + artwork.getTitle(), exception);
        }
    }

    @Override
    public void update(Artwork artwork) {
        Objects.requireNonNull(artwork, "artwork must not be null");

        String updateSql = """
                UPDATE Artwork
                SET creationYear = ?, type = ?, price = ?, Id_Artist = ?, status = ?, medium = ?, dimensions = ?, description = ?
                WHERE Id_Artwork = ?
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer artworkId = findArtworkId(connection, artwork.getTitle(), artwork.getArtist());
                if (artworkId == null) {
                    throw new SQLException("Artwork not found: " + artwork.getTitle());
                }

                int artistId = resolveArtistId(connection, artwork.getArtist());
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    if (artwork.getCreationYear() == null) {
                        statement.setNull(1, Types.INTEGER);
                    } else {
                        statement.setInt(1, artwork.getCreationYear());
                    }
                    statement.setString(2, artwork.getType());
                    statement.setDouble(3, artwork.getPrice());
                    statement.setInt(4, artistId);
                    statement.setString(5, toDatabaseStatus(artwork.getStatus()));
                    statement.setString(6, artwork.getMedium());
                    statement.setString(7, artwork.getDimensions());
                    statement.setString(8, artwork.getDescription());
                    statement.setInt(9, artworkId);
                    statement.executeUpdate();
                }

                syncTags(connection, artworkId, artwork.getTags());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update artwork: " + artwork.getTitle(), exception);
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
                List<Integer> artworkIds = findArtworkIdsByTitle(connection, title);
                for (Integer artworkId : artworkIds) {
                    deleteArtworkDependencies(connection, artworkId);
                }

                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Artwork WHERE title = ?")) {
                    statement.setString(1, title);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete artwork: " + title, exception);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return List.of();
        }
        return findByWhereClause("WHERE a.name = ?", statement -> statement.setString(1, artistName));
    }

    private List<Artwork> findByWhereClause(String whereClause, StatementConfigurer configurer) {
        String sql = SELECT_ARTWORKS + " " + whereClause + " ORDER BY aw.title";

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Artist> artistsById = new LinkedHashMap<>();
                Map<Integer, Artwork> artworksById = new LinkedHashMap<>();

                while (resultSet.next()) {
                    int artistId = resultSet.getInt("Id_Artist");
                    Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(resultSet));

                    int artworkId = resultSet.getInt("Id_Artwork");
                    Artwork artwork = artworksById.get(artworkId);
                    if (artwork == null) {
                        artwork = mapArtwork(resultSet, artist);
                        artworksById.put(artworkId, artwork);
                        artist.getArtworks().add(artwork);
                    }
                }

                loadTags(connection, artworksById);
                return new ArrayList<>(artworksById.values());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load artworks.", exception);
        }
    }

    private Artist mapArtist(ResultSet resultSet) {
        try {
            Artist artist = new Artist(
                    resultSet.getString("artist_name"),
                    resultSet.getString("bio"),
                    resultSet.getInt("birthyear"),
                    resultSet.getString("contactEmail"),
                    resultSet.getString("city")
            );
            artist.setActive(resultSet.getBoolean("isActive"));
            return artist;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map artist.", exception);
        }
    }

    private Artwork mapArtwork(ResultSet resultSet, Artist artist) {
        try {
            Artwork artwork = new Artwork(
                    resultSet.getString("title"),
                    resultSet.getInt("creationYear"),
                    resultSet.getString("type"),
                    resultSet.getDouble("price"),
                    artist
            );
            artwork.setMedium(resultSet.getString("medium"));
            artwork.setDimensions(resultSet.getString("dimensions"));
            artwork.setDescription(resultSet.getString("description"));
            artwork.setStatus(parseStatus(resultSet.getString("status")));
            return artwork;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map artwork.", exception);
        }
    }

    private void loadTags(Connection connection, Map<Integer, Artwork> artworksById) throws SQLException {
        if (artworksById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT q.Id_Artwork, t.name
                FROM qualified q
                JOIN Tag t ON t.Id_Tag = q.Id_Tag
                ORDER BY q.Id_Artwork, t.name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Artwork artwork = artworksById.get(resultSet.getInt("Id_Artwork"));
                if (artwork == null) {
                    continue;
                }
                artwork.getTags().add(new ArtworkTag(resultSet.getString("name")));
            }
        }
    }

    private int resolveArtistId(Connection connection, Artist artist) throws SQLException {
        if (artist == null || artist.getName() == null || artist.getName().isBlank()) {
            throw new SQLException("Artwork artist is required.");
        }

        String byNameAndEmail = "SELECT Id_Artist FROM Artist WHERE name = ? AND contactEmail = ? LIMIT 1";
        if (artist.getContactEmail() != null && !artist.getContactEmail().isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(byNameAndEmail)) {
                statement.setString(1, artist.getName());
                statement.setString(2, artist.getContactEmail());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_Artist");
                    }
                }
            }
        }

        String byName = "SELECT Id_Artist FROM Artist WHERE name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(byName)) {
            statement.setString(1, artist.getName());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Artist");
                }
            }
        }

        throw new SQLException("Artist not found in database: " + artist.getName());
    }

    private Integer findArtworkId(Connection connection, String title, Artist artist) throws SQLException {
        if (title == null || title.isBlank()) {
            return null;
        }

        String byTitleAndArtist = """
                SELECT Id_Artwork
                FROM Artwork
                WHERE title = ? AND Id_Artist = ?
                LIMIT 1
                """;
        if (artist != null) {
            int artistId = resolveArtistId(connection, artist);
            try (PreparedStatement statement = connection.prepareStatement(byTitleAndArtist)) {
                statement.setString(1, title);
                statement.setInt(2, artistId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_Artwork");
                    }
                }
            }
        }

        String byTitle = "SELECT Id_Artwork FROM Artwork WHERE title = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(byTitle)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Artwork");
                }
            }
        }
        return null;
    }

    private List<Integer> findArtworkIdsByTitle(Connection connection, String title) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_Artwork FROM Artwork WHERE title = ?")) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("Id_Artwork"));
                }
            }
        }
        return ids;
    }

    private void deleteArtworkDependencies(Connection connection, int artworkId) throws SQLException {
        try (PreparedStatement deleteQualified = connection.prepareStatement(
                "DELETE FROM qualified WHERE Id_Artwork = ?")) {
            deleteQualified.setInt(1, artworkId);
            deleteQualified.executeUpdate();
        }

        try (PreparedStatement deleteReviews = connection.prepareStatement(
                "DELETE FROM Review WHERE Id_Artwork = ?")) {
            deleteReviews.setInt(1, artworkId);
            deleteReviews.executeUpdate();
        }
    }

    private void syncTags(Connection connection, int artworkId, List<ArtworkTag> tags) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM qualified WHERE Id_Artwork = ?")) {
            deleteStatement.setInt(1, artworkId);
            deleteStatement.executeUpdate();
        }

        if (tags == null || tags.isEmpty()) {
            return;
        }

        Set<String> uniqueTagNames = new LinkedHashSet<>();
        for (ArtworkTag tag : tags) {
            if (tag != null && tag.getName() != null && !tag.getName().isBlank()) {
                uniqueTagNames.add(tag.getName().trim());
            }
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO qualified(Id_Artwork, Id_Tag) VALUES (?, ?)")) {
            for (String tagName : uniqueTagNames) {
                int tagId = ensureTagId(connection, tagName);
                insertStatement.setInt(1, artworkId);
                insertStatement.setInt(2, tagId);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private int ensureTagId(Connection connection, String tagName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_Tag FROM Tag WHERE name = ? LIMIT 1")) {
            statement.setString(1, tagName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Tag");
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Tag(name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tagName);
            statement.executeUpdate();
            return extractGeneratedId(statement, "tag");
        }
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

    private String toDatabaseStatus(Artwork.Status status) {
        return status == null ? Artwork.Status.FOR_SALE.name() : status.name();
    }

    private int extractGeneratedId(PreparedStatement statement, String entityName) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("No generated key returned for " + entityName + ".");
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
