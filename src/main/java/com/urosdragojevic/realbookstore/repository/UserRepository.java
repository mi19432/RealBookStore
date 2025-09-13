package com.urosdragojevic.realbookstore.repository;

import com.urosdragojevic.realbookstore.audit.AuditLogger;
import com.urosdragojevic.realbookstore.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    private DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findUser(String username) {
        String query = "SELECT id, username, password FROM users WHERE username='" + username + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                int id = rs.getInt(1);
                String username1 = rs.getString(2);
                String password = rs.getString(3);
                return new User(id, username1, password);
            }else{
                LOG.warn("Korisnik '{}' nije pronađen u bazi", username);
            }
        } catch (SQLException e) {
            LOG.error("Greška prilikom pretrage korisnika '{}'", username, e);
        }
        return null;
    }

    public boolean validCredentials(String username, String password) {
        String query = "SELECT username FROM users WHERE username='" + username + "' AND password='" + password + "'";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
             boolean valid = rs.next();
             if (valid) {
                LOG.info("Uspešna validacija kredencijala za korisnika '{}'", username);
             } else {
                LOG.warn("Neuspešna validacija kredencijala za korisnika '{}'", username);
             }
             return valid;
        } catch (SQLException e) {
            LOG.error("Greška prilikom provere kredencijala za korisnika '{}'", username, e);
        }
        return false;
    }

    public void delete(int userId) {
        String query = "DELETE FROM users WHERE id = " + userId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
        ) {
            int affectedRows = statement.executeUpdate(query);
            if (affectedRows > 0) {
                LOG.info("Uspešno obrisan korisnik sa ID={}", userId);
            } else {
                LOG.warn("Nije pronađen korisnik sa ID={} za brisanje", userId);
            }
        } catch (SQLException e) {
            LOG.error("Greška prilikom brisanja korisnika sa ID={}", userId, e);
        }
    }
}
