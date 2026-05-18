package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC implementation of {@link ArtistDao}.
 */
public class JdbcArtistDao implements ArtistDao {

    private static final String ARTIST_BASE_SELECT = """
            SELECT Id_Artist, name, bio, birthyear, contactEmail, city, isActive
            FROM Artist
            """;

    @Override
    public List<Artist> findAll() {
        String sql = ARTIST_BASE_SELECT + " ORDER BY name";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            Map<Integer, Artist> artistsById = mapArtists(resultSet);
            loadDisciplines(connection, artistsById);
            return new ArrayList<>(artistsById.values());
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load artists.", exception);
        }
    }

    @Override
    public Optional<Artist> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String sql = ARTIST_BASE_SELECT + " WHERE name = ? LIMIT 1";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Artist> artistsById = mapArtists(resultSet);
                if (artistsById.isEmpty()) {
                    return Optional.empty();
                }
                loadDisciplines(connection, artistsById);
                return Optional.of(artistsById.values().iterator().next());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load artist by name: " + name, exception);
        }
    }

    @Override
    public void save(Artist artist) {
        Objects.requireNonNull(artist, "artist must not be null");

        String sql = """
                INSERT INTO Artist(name, bio, birthyear, contactEmail, city, isActive)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, artist.getName());
                statement.setString(2, artist.getBio());
                if (artist.getBirthYear() == null) {
                    statement.setNull(3, Types.INTEGER);
                } else {
                    statement.setInt(3, artist.getBirthYear());
                }
                statement.setString(4, artist.getContactEmail());
                statement.setString(5, artist.getCity());
                statement.setBoolean(6, artist.isActive());
                statement.executeUpdate();

                int artistId = extractGeneratedId(statement, "artist");
                syncDisciplines(connection, artistId, artist.getDisciplines());
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save artist: " + artist.getName(), exception);
        }
    }

    @Override
    public void update(Artist artist) {
        Objects.requireNonNull(artist, "artist must not be null");

        String sql = """
                UPDATE Artist
                SET bio = ?, birthyear = ?, contactEmail = ?, city = ?, isActive = ?
                WHERE name = ?
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, artist.getBio());
                if (artist.getBirthYear() == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, artist.getBirthYear());
                }
                statement.setString(3, artist.getContactEmail());
                statement.setString(4, artist.getCity());
                statement.setBoolean(5, artist.isActive());
                statement.setString(6, artist.getName());
                statement.executeUpdate();

                Integer artistId = findArtistId(connection, artist);
                if (artistId != null) {
                    syncDisciplines(connection, artistId, artist.getDisciplines());
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update artist: " + artist.getName(), exception);
        }
    }

    @Override
    public void delete(String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return;
        }

        String sql = "DELETE FROM Artist WHERE name = ?";
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer artistId = findArtistIdByName(connection, artistName);
                if (artistId != null) {
                    deleteArtistDependencies(connection, artistId);
                }

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, artistName);
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
            throw new RuntimeException("Failed to delete artist: " + artistName, exception);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        if (city == null || city.isBlank()) {
            return List.of();
        }

        String sql = ARTIST_BASE_SELECT + " WHERE city = ? ORDER BY name";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, city);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Artist> artistsById = mapArtists(resultSet);
                loadDisciplines(connection, artistsById);
                return new ArrayList<>(artistsById.values());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load artists by city: " + city, exception);
        }
    }

    @Override
    public List<Discipline> findAllDisciplines() {
        String sql = "SELECT name FROM Discipline ORDER BY name";
        List<Discipline> disciplines = new ArrayList<>();

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                disciplines.add(new Discipline(resultSet.getString("name")));
            }
            return disciplines;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load disciplines.", exception);
        }
    }

    private Map<Integer, Artist> mapArtists(ResultSet resultSet) throws SQLException {
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        while (resultSet.next()) {
            int artistId = resultSet.getInt("Id_Artist");
            Artist artist = new Artist(
                    resultSet.getString("name"),
                    resultSet.getString("bio"),
                    resultSet.getInt("birthyear"),
                    resultSet.getString("contactEmail"),
                    resultSet.getString("city")
            );
            artist.setActive(resultSet.getBoolean("isActive"));
            artistsById.put(artistId, artist);
        }
        return artistsById;
    }

    private void loadDisciplines(Connection connection, Map<Integer, Artist> artistsById) throws SQLException {
        if (artistsById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT m.Id_Artist, d.name
                FROM master m
                JOIN Discipline d ON d.Id_Discipline = m.Id_Discipline
                ORDER BY m.Id_Artist, d.name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int artistId = resultSet.getInt("Id_Artist");
                Artist artist = artistsById.get(artistId);
                if (artist == null) {
                    continue;
                }
                artist.getDisciplines().add(new Discipline(resultSet.getString("name")));
            }
        }
    }

    private void syncDisciplines(Connection connection, int artistId, List<Discipline> disciplines) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM master WHERE Id_Artist = ?")) {
            deleteStatement.setInt(1, artistId);
            deleteStatement.executeUpdate();
        }

        if (disciplines == null || disciplines.isEmpty()) {
            return;
        }

        Set<String> uniqueNames = new LinkedHashSet<>();
        for (Discipline discipline : disciplines) {
            if (discipline != null && discipline.getName() != null && !discipline.getName().isBlank()) {
                uniqueNames.add(discipline.getName().trim());
            }
        }

        String insertSql = "INSERT INTO master(Id_Artist, Id_Discipline) VALUES (?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            for (String disciplineName : uniqueNames) {
                int disciplineId = ensureDisciplineId(connection, disciplineName);
                insertStatement.setInt(1, artistId);
                insertStatement.setInt(2, disciplineId);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private int ensureDisciplineId(Connection connection, String disciplineName) throws SQLException {
        String selectSql = "SELECT Id_Discipline FROM Discipline WHERE name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, disciplineName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Discipline");
                }
            }
        }

        String insertSql = "INSERT INTO Discipline(name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, disciplineName);
            statement.executeUpdate();
            return extractGeneratedId(statement, "discipline");
        }
    }

    private Integer findArtistId(Connection connection, Artist artist) throws SQLException {
        if (artist.getName() == null || artist.getName().isBlank()) {
            return null;
        }

        if (artist.getContactEmail() != null && !artist.getContactEmail().isBlank()) {
            String sql = "SELECT Id_Artist FROM Artist WHERE name = ? AND contactEmail = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, artist.getName());
                statement.setString(2, artist.getContactEmail());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_Artist");
                    }
                }
            }
        }

        return findArtistIdByName(connection, artist.getName());
    }

    private Integer findArtistIdByName(Connection connection, String artistName) throws SQLException {
        String sql = "SELECT Id_Artist FROM Artist WHERE name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, artistName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Artist");
                }
            }
        }
        return null;
    }

    private void deleteArtistDependencies(Connection connection, int artistId) throws SQLException {
        List<Integer> artworkIds = findIds(
                connection,
                "SELECT Id_Artwork FROM Artwork WHERE Id_Artist = ?",
                artistId,
                "Id_Artwork"
        );
        if (!artworkIds.isEmpty()) {
            deleteByIds(connection, "DELETE FROM qualified WHERE Id_Artwork IN (%s)", artworkIds);
            deleteByIds(connection, "DELETE FROM Review WHERE Id_Artwork IN (%s)", artworkIds);
            deleteByIds(connection, "DELETE FROM Artwork WHERE Id_Artwork IN (%s)", artworkIds);
        }

        List<Integer> workshopIds = findIds(
                connection,
                "SELECT Id_Workshop FROM Workshop WHERE Id_Artist = ?",
                artistId,
                "Id_Workshop"
        );
        if (!workshopIds.isEmpty()) {
            deleteByIds(connection, "DELETE FROM booking WHERE Id_Workshop IN (%s)", workshopIds);
            deleteByIds(connection, "DELETE FROM Workshop WHERE Id_Workshop IN (%s)", workshopIds);
        }

        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM master WHERE Id_Artist = ?")) {
            statement.setInt(1, artistId);
            statement.executeUpdate();
        }
    }

    private List<Integer> findIds(Connection connection, String sql, int id, String column) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(column));
                }
            }
        }
        return ids;
    }

    private void deleteByIds(Connection connection, String sqlPattern, Collection<Integer> ids) throws SQLException {
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = sqlPattern.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Integer value : ids) {
                statement.setInt(index++, value);
            }
            statement.executeUpdate();
        }
    }

    private int extractGeneratedId(PreparedStatement statement, String entityName) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("No generated key returned for " + entityName + ".");
    }
}
