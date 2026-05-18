package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of {@link WorkshopDao}.
 */
public class JdbcWorkshopDao implements WorkshopDao {

    private static final String WORKSHOP_SELECT = """
            SELECT w.Id_Workshop, w.title, w.date_, w.price, w.level, w.durationMinutes, w.maxParticipants, w.location, w.description,
                   a.Id_Artist, a.name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
            FROM Workshop w
            JOIN Artist a ON a.Id_Artist = w.Id_Artist
            """;

    @Override
    public Optional<Workshop> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Workshop> workshops = fetchWorkshops("WHERE w.Id_Workshop = ?", statement -> statement.setLong(1, id));
        return workshops.stream().findFirst();
    }

    @Override
    public Optional<Workshop> findByTitle(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        List<Workshop> workshops = fetchWorkshops("WHERE w.title = ?", statement -> statement.setString(1, title));
        return workshops.stream().findFirst();
    }

    @Override
    public List<Workshop> findAll() {
        return fetchWorkshops("", statement -> {
        });
    }

    @Override
    public void save(Workshop workshop) {
        Objects.requireNonNull(workshop, "workshop must not be null");

        String sql = """
                INSERT INTO Workshop(title, date_, price, Id_Artist, level, durationMinutes, maxParticipants, location, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int artistId = resolveArtistId(connection, workshop.getInstructor());
            statement.setString(1, workshop.getTitle());
            statement.setTimestamp(2, toTimestamp(workshop.getDate()));
            statement.setDouble(3, workshop.getPrice());
            statement.setInt(4, artistId);
            statement.setString(5, normalizeLevel(workshop.getLevel()));
            statement.setInt(6, workshop.getDurationMinutes());
            statement.setInt(7, workshop.getMaxParticipants());
            statement.setString(8, workshop.getLocation());
            statement.setString(9, workshop.getDescription());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save workshop: " + workshop.getTitle(), exception);
        }
    }

    @Override
    public void update(Workshop workshop) {
        Objects.requireNonNull(workshop, "workshop must not be null");

        String sql = """
                UPDATE Workshop
                SET date_ = ?, price = ?, Id_Artist = ?, level = ?, durationMinutes = ?, maxParticipants = ?, location = ?, description = ?
                WHERE title = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int artistId = resolveArtistId(connection, workshop.getInstructor());
            statement.setTimestamp(1, toTimestamp(workshop.getDate()));
            statement.setDouble(2, workshop.getPrice());
            statement.setInt(3, artistId);
            statement.setString(4, normalizeLevel(workshop.getLevel()));
            statement.setInt(5, workshop.getDurationMinutes());
            statement.setInt(6, workshop.getMaxParticipants());
            statement.setString(7, workshop.getLocation());
            statement.setString(8, workshop.getDescription());
            statement.setString(9, workshop.getTitle());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update workshop: " + workshop.getTitle(), exception);
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
                try (PreparedStatement deleteBooking = connection.prepareStatement("""
                        DELETE b
                        FROM booking b
                        JOIN Workshop w ON w.Id_Workshop = b.Id_Workshop
                        WHERE w.title = ?
                        """)) {
                    deleteBooking.setString(1, title);
                    deleteBooking.executeUpdate();
                }

                try (PreparedStatement deleteWorkshop = connection.prepareStatement(
                        "DELETE FROM Workshop WHERE title = ?")) {
                    deleteWorkshop.setString(1, title);
                    deleteWorkshop.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete workshop: " + title, exception);
        }
    }

    @Override
    public void createBooking(Workshop workshop, CommunityMember member) {
        Objects.requireNonNull(workshop, "workshop must not be null");
        Objects.requireNonNull(member, "member must not be null");

        String sql = """
                INSERT INTO booking(Id_Workshop, Id_CommunityMember, paymentStatus)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    bookingDate = CURRENT_TIMESTAMP,
                    paymentStatus = VALUES(paymentStatus)
                """;

        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int workshopId = resolveWorkshopId(connection, workshop);
            int memberId = resolveMemberId(connection, member);

            statement.setInt(1, workshopId);
            statement.setInt(2, memberId);
            statement.setString(3, "PENDING");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to create workshop booking.", exception);
        }
    }

    @Override
    public List<Booking> findBookingsByMember(CommunityMember member) {
        if (member == null || member.getName() == null || member.getName().isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT b.bookingDate, b.paymentStatus,
                       w.Id_Workshop, w.title, w.date_, w.price, w.level, w.durationMinutes, w.maxParticipants, w.location, w.description,
                       a.Id_Artist, a.name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
                FROM booking b
                JOIN Workshop w ON w.Id_Workshop = b.Id_Workshop
                JOIN Artist a ON a.Id_Artist = w.Id_Artist
                WHERE b.Id_CommunityMember = ?
                ORDER BY b.bookingDate DESC
                """;

        try (Connection connection = ConnectionManager.getConnection()) {
            int memberId = resolveMemberId(connection, member);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, memberId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    Map<Integer, Artist> artistsById = new LinkedHashMap<>();
                    Map<Integer, Workshop> workshopsById = new LinkedHashMap<>();
                    List<Booking> bookings = new ArrayList<>();

                    while (resultSet.next()) {
                        int artistId = resultSet.getInt("Id_Artist");
                        Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(resultSet));

                        int workshopId = resultSet.getInt("Id_Workshop");
                        Workshop workshop = workshopsById.get(workshopId);
                        if (workshop == null) {
                            workshop = mapWorkshop(resultSet, artist);
                            workshopsById.put(workshopId, workshop);
                        }

                        Booking booking = new Booking();
                        booking.setWorkshop(workshop);
                        booking.setMember(member);
                        booking.setPaymentStatus(resultSet.getString("paymentStatus"));
                        booking.setBookingDate(toLocalDateTime(resultSet.getTimestamp("bookingDate")));
                        bookings.add(booking);
                    }
                    return bookings;
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load bookings for member: " + member.getName(), exception);
        }
    }

    private List<Workshop> fetchWorkshops(String whereClause, StatementConfigurer configurer) {
        String sql = WORKSHOP_SELECT + " " + whereClause + " ORDER BY w.date_";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, Artist> artistsById = new LinkedHashMap<>();
                Map<Integer, Workshop> workshopsById = new LinkedHashMap<>();

                while (resultSet.next()) {
                    int artistId = resultSet.getInt("Id_Artist");
                    Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(resultSet));

                    int workshopId = resultSet.getInt("Id_Workshop");
                    Workshop workshop = workshopsById.get(workshopId);
                    if (workshop == null) {
                        workshop = mapWorkshop(resultSet, artist);
                        workshopsById.put(workshopId, workshop);
                    }
                }
                return new ArrayList<>(workshopsById.values());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load workshops.", exception);
        }
    }

    private Artist mapArtist(ResultSet resultSet) {
        try {
            Artist artist = new Artist(
                    resultSet.getString("name"),
                    resultSet.getString("bio"),
                    resultSet.getInt("birthyear"),
                    resultSet.getString("contactEmail"),
                    resultSet.getString("city")
            );
            artist.setActive(resultSet.getBoolean("isActive"));
            return artist;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map workshop instructor.", exception);
        }
    }

    private Workshop mapWorkshop(ResultSet resultSet, Artist artist) {
        try {
            Workshop workshop = new Workshop(
                    resultSet.getString("title"),
                    toLocalDateTime(resultSet.getTimestamp("date_")),
                    artist,
                    resultSet.getDouble("price")
            );
            workshop.setLevel(resultSet.getString("level"));
            workshop.setDurationMinutes(resultSet.getInt("durationMinutes"));
            workshop.setMaxParticipants(resultSet.getInt("maxParticipants"));
            workshop.setLocation(resultSet.getString("location"));
            workshop.setDescription(resultSet.getString("description"));
            return workshop;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to map workshop.", exception);
        }
    }

    private int resolveArtistId(Connection connection, Artist artist) throws SQLException {
        if (artist == null || artist.getName() == null || artist.getName().isBlank()) {
            throw new SQLException("Workshop instructor is required.");
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

        throw new SQLException("Instructor not found in database: " + artist.getName());
    }

    private int resolveMemberId(Connection connection, CommunityMember member) throws SQLException {
        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT Id_CommunityMember FROM CommunityMember WHERE email = ? LIMIT 1")) {
                statement.setString(1, member.getEmail());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_CommunityMember");
                    }
                }
            }
        }

        if (member.getName() != null && !member.getName().isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT Id_CommunityMember FROM CommunityMember WHERE name = ? LIMIT 1")) {
                statement.setString(1, member.getName());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("Id_CommunityMember");
                    }
                }
            }
        }

        throw new SQLException("Member not found in database.");
    }

    private int resolveWorkshopId(Connection connection, Workshop workshop) throws SQLException {
        String sql = """
                SELECT Id_Workshop
                FROM Workshop
                WHERE title = ? AND Id_Artist = ? AND date_ = ?
                LIMIT 1
                """;

        int artistId = resolveArtistId(connection, workshop.getInstructor());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workshop.getTitle());
            statement.setInt(2, artistId);
            Timestamp timestamp = toTimestamp(workshop.getDate());
            if (timestamp == null) {
                statement.setNull(3, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(3, timestamp);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Workshop");
                }
            }
        }

        String byTitle = "SELECT Id_Workshop FROM Workshop WHERE title = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(byTitle)) {
            statement.setString(1, workshop.getTitle());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("Id_Workshop");
                }
            }
        }

        throw new SQLException("Workshop not found in database: " + workshop.getTitle());
    }

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "BEGINNER";
        }
        return level.toUpperCase();
    }

    private Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
