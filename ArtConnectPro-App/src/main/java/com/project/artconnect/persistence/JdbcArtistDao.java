package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcArtistDao implements ArtistDao {

    private final Connection connection;

    public JdbcArtistDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Artist> findAll() {

        List<Artist> artists = new ArrayList<>();

        String sql = "SELECT * FROM Artist";

        try {

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {

                Artist artist = new Artist();

                artist.setIdArtist(
                        resultSet.getInt("Id_Artist")
                );

                artist.setName(
                        resultSet.getString("name")
                );

                artist.setBio(
                        resultSet.getString("bio")
                );

                artist.setBirthYear(
                        resultSet.getInt("birthyear")
                );

                artist.setContactEmail(
                        resultSet.getString("contactEmail")
                );

                artist.setCity(
                        resultSet.getString("city")
                );

                artist.setActive(
                        resultSet.getBoolean("isActive")
                );

                artists.add(artist);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return artists;
    }

    @Override
    public void save(Artist artist) {

        String sql =
                "INSERT INTO Artist(name, bio, birthyear, contactEmail, city, isActive) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try {

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, artist.getName());
            statement.setString(2, artist.getBio());
            statement.setInt(3, artist.getBirthYear());
            statement.setString(4, artist.getContactEmail());
            statement.setString(5, artist.getCity());
            statement.setBoolean(6, artist.isActive());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Artist artist) {

        String sql =
                "UPDATE Artist " +
                "SET bio = ?, birthyear = ?, contactEmail = ?, city = ?, isActive = ? " +
                "WHERE name = ?";

        try {

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, artist.getBio());
            statement.setInt(2, artist.getBirthYear());
            statement.setString(3, artist.getContactEmail());
            statement.setString(4, artist.getCity());
            statement.setBoolean(5, artist.isActive());
            statement.setString(6, artist.getName());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String artistName) {

        String sql =
                "DELETE FROM Artist WHERE name = ?";

        try {

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, artistName);

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Artist> findByCity(String city) {

        List<Artist> artists = new ArrayList<>();

        String sql =
                "SELECT * FROM Artist WHERE city = ?";

        try {

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, city);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {

                Artist artist = new Artist();

                artist.setIdArtist(
                        resultSet.getInt("Id_Artist")
                );

                artist.setName(
                        resultSet.getString("name")
                );

                artist.setBio(
                        resultSet.getString("bio")
                );

                artist.setBirthYear(
                        resultSet.getInt("birthyear")
                );

                artist.setContactEmail(
                        resultSet.getString("contactEmail")
                );

                artist.setCity(
                        resultSet.getString("city")
                );

                artist.setActive(
                        resultSet.getBoolean("isActive")
                );

                artists.add(artist);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return artists;
    }
}