package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Review;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
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
 * JDBC implementation of {@link CommunityMemberDao}.
 */
public class JdbcCommunityMemberDao implements CommunityMemberDao {

    private static final String MEMBER_BASE_SELECT = """
            SELECT Id_CommunityMember, name, email, birthyear, phone, membershipType, city
            FROM CommunityMember
            """;

    @Override
    public Optional<CommunityMember> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        List<CommunityMember> members = fetchMembers("WHERE Id_CommunityMember = ?", statement -> statement.setLong(1, id));
        return members.stream().findFirst();
    }

    @Override
    public Optional<CommunityMember> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        List<CommunityMember> members = fetchMembers("WHERE name = ?", statement -> statement.setString(1, name));
        return members.stream().findFirst();
    }

    @Override
    public List<CommunityMember> findAll() {
        return fetchMembers("", statement -> {
        });
    }

    @Override
    public void save(CommunityMember member) {
        Objects.requireNonNull(member, "member must not be null");

        String sql = """
                INSERT INTO CommunityMember(name, email, birthyear, phone, membershipType, city)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.getName());
            statement.setString(2, member.getEmail());
            if (member.getBirthYear() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, member.getBirthYear());
            }
            statement.setString(4, member.getPhone());
            statement.setString(5, normalizeMembershipType(member.getMembershipType()));
            statement.setString(6, member.getCity());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save community member: " + member.getName(), exception);
        }
    }

    @Override
    public void update(CommunityMember member) {
        Objects.requireNonNull(member, "member must not be null");

        String sql = """
                UPDATE CommunityMember
                SET email = ?, birthyear = ?, phone = ?, membershipType = ?, city = ?
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.getEmail());
            if (member.getBirthYear() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, member.getBirthYear());
            }
            statement.setString(3, member.getPhone());
            statement.setString(4, normalizeMembershipType(member.getMembershipType()));
            statement.setString(5, member.getCity());
            statement.setString(6, member.getName());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update community member: " + member.getName(), exception);
        }
    }

    @Override
    public void delete(String memberName) {
        if (memberName == null || memberName.isBlank()) {
            return;
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<Integer> memberIds = findMemberIdsByName(connection, memberName);
                for (Integer memberId : memberIds) {
                    try (PreparedStatement deleteFavorites = connection.prepareStatement(
                            "DELETE FROM favoriteDisciplines WHERE Id_CommunityMember = ?")) {
                        deleteFavorites.setInt(1, memberId);
                        deleteFavorites.executeUpdate();
                    }

                    try (PreparedStatement deleteBookings = connection.prepareStatement(
                            "DELETE FROM booking WHERE Id_CommunityMember = ?")) {
                        deleteBookings.setInt(1, memberId);
                        deleteBookings.executeUpdate();
                    }

                    try (PreparedStatement deleteReviews = connection.prepareStatement(
                            "DELETE FROM Review WHERE Id_CommunityMember = ?")) {
                        deleteReviews.setInt(1, memberId);
                        deleteReviews.executeUpdate();
                    }
                }

                try (PreparedStatement deleteMember = connection.prepareStatement(
                        "DELETE FROM CommunityMember WHERE name = ?")) {
                    deleteMember.setString(1, memberName);
                    deleteMember.executeUpdate();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete community member: " + memberName, exception);
        }
    }

    @Override
    public List<Review> findReviewsByMember(CommunityMember member) {
        if (member == null) {
            return List.of();
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            Integer memberId = resolveMemberId(connection, member);
            if (memberId == null) {
                return List.of();
            }

            member.getReviews().clear();
            Map<Integer, CommunityMember> membersById = new LinkedHashMap<>();
            membersById.put(memberId, member);
            populateReviews(connection, membersById);
            return new ArrayList<>(member.getReviews());
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load reviews for member: " + member.getName(), exception);
        }
    }

    private List<CommunityMember> fetchMembers(String whereClause, StatementConfigurer configurer) {
        String sql = MEMBER_BASE_SELECT + " " + whereClause + " ORDER BY name";
        try (Connection connection = ConnectionManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Integer, CommunityMember> membersById = mapMembers(resultSet);
                populateFavoriteDisciplines(connection, membersById);
                populateBookings(connection, membersById);
                populateReviews(connection, membersById);
                return new ArrayList<>(membersById.values());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load community members.", exception);
        }
    }

    private Map<Integer, CommunityMember> mapMembers(ResultSet resultSet) throws SQLException {
        Map<Integer, CommunityMember> membersById = new LinkedHashMap<>();
        while (resultSet.next()) {
            int memberId = resultSet.getInt("Id_CommunityMember");
            CommunityMember member = new CommunityMember(
                    resultSet.getString("name"),
                    resultSet.getString("email")
            );
            int birthYear = resultSet.getInt("birthyear");
            if (!resultSet.wasNull()) {
                member.setBirthYear(birthYear);
            }
            member.setPhone(resultSet.getString("phone"));
            member.setMembershipType(resultSet.getString("membershipType"));
            member.setCity(resultSet.getString("city"));
            membersById.put(memberId, member);
        }
        return membersById;
    }

    private void populateFavoriteDisciplines(Connection connection, Map<Integer, CommunityMember> membersById)
            throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT fd.Id_CommunityMember, d.name
                FROM favoriteDisciplines fd
                JOIN Discipline d ON d.Id_Discipline = fd.Id_Discipline
                ORDER BY fd.Id_CommunityMember, d.name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("Id_CommunityMember"));
                if (member == null) {
                    continue;
                }
                member.getFavoriteDisciplines().add(new Discipline(resultSet.getString("name")));
            }
        }
    }

    private void populateBookings(Connection connection, Map<Integer, CommunityMember> membersById) throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT b.Id_CommunityMember, b.bookingDate, b.paymentStatus,
                       w.Id_Workshop, w.title, w.date_, w.price, w.level, w.durationMinutes, w.maxParticipants, w.location, w.description,
                       a.Id_Artist, a.name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
                FROM booking b
                JOIN Workshop w ON w.Id_Workshop = b.Id_Workshop
                JOIN Artist a ON a.Id_Artist = w.Id_Artist
                ORDER BY b.bookingDate DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, Artist> artistsById = new LinkedHashMap<>();
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("Id_CommunityMember"));
                if (member == null) {
                    continue;
                }

                int artistId = resultSet.getInt("Id_Artist");
                Artist instructor = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(resultSet));
                Workshop workshop = mapWorkshop(resultSet, instructor);

                Booking booking = new Booking();
                booking.setMember(member);
                booking.setWorkshop(workshop);
                booking.setPaymentStatus(resultSet.getString("paymentStatus"));
                booking.setBookingDate(toLocalDateTime(resultSet.getTimestamp("bookingDate")));
                member.getBookings().add(booking);
            }
        }
    }

    private void populateReviews(Connection connection, Map<Integer, CommunityMember> membersById) throws SQLException {
        if (membersById.isEmpty()) {
            return;
        }

        String sql = """
                SELECT r.Id_CommunityMember, r.rating, r.comment, r.reviewDate,
                       aw.Id_Artwork, aw.title, aw.creationYear, aw.type, aw.price, aw.status, aw.medium, aw.dimensions, aw.description,
                       a.Id_Artist, a.name, a.bio, a.birthyear, a.contactEmail, a.city, a.isActive
                FROM Review r
                JOIN Artwork aw ON aw.Id_Artwork = r.Id_Artwork
                JOIN Artist a ON a.Id_Artist = aw.Id_Artist
                ORDER BY r.reviewDate DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Map<Integer, Artist> artistsById = new LinkedHashMap<>();
            while (resultSet.next()) {
                CommunityMember member = membersById.get(resultSet.getInt("Id_CommunityMember"));
                if (member == null) {
                    continue;
                }

                int artistId = resultSet.getInt("Id_Artist");
                Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(resultSet));

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

                Review review = new Review();
                review.setReviewer(member);
                review.setArtwork(artwork);
                review.setRating(resultSet.getInt("rating"));
                review.setComment(resultSet.getString("comment"));
                review.setReviewDate(toLocalDate(resultSet.getDate("reviewDate")));
                member.getReviews().add(review);
            }
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
            throw new RuntimeException("Failed to map artist.", exception);
        }
    }

    private Workshop mapWorkshop(ResultSet resultSet, Artist instructor) {
        try {
            Workshop workshop = new Workshop(
                    resultSet.getString("title"),
                    toLocalDateTime(resultSet.getTimestamp("date_")),
                    instructor,
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

    private Integer resolveMemberId(Connection connection, CommunityMember member) throws SQLException {
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
        return null;
    }

    private List<Integer> findMemberIdsByName(Connection connection, String memberName) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT Id_CommunityMember FROM CommunityMember WHERE name = ?")) {
            statement.setString(1, memberName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("Id_CommunityMember"));
                }
            }
        }
        return ids;
    }

    private String normalizeMembershipType(String membershipType) {
        if (membershipType == null || membershipType.isBlank()) {
            return "FREE";
        }
        return membershipType.toUpperCase();
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

    private java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private java.time.LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
