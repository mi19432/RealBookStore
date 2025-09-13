package com.urosdragojevic.realbookstore.repository;

import com.urosdragojevic.realbookstore.domain.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CommentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CommentRepository.class);


    private DataSource dataSource;

    public CommentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Comment comment) {
        String query = "INSERT INTO comments(bookId, userId, comment) VALUES (?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, comment.getBookId());
            preparedStatement.setInt(2, comment.getUserId());
            preparedStatement.setString(3, comment.getComment());

            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                LOG.info("Uspešno dodat novi komentar za knjigu sa ID={} od korisnika ID={}",
                        comment.getBookId(), comment.getUserId());
            } else {
                LOG.warn("Pokušaj dodavanja komentara za knjigu sa ID={} nije uspeo", comment.getBookId());
            }

        } catch (SQLException e) {
            LOG.error("Greška prilikom dodavanja komentara za knjigu sa ID={}", comment.getBookId(), e);
        }
    }

    public List<Comment> getAll(int bookId) {
        List<Comment> commentList = new ArrayList<>();
        String query = "SELECT bookId, userId, comment FROM comments WHERE bookId = " + bookId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                commentList.add(new Comment(rs.getInt(1), rs.getInt(2), rs.getString(3)));
            }
            LOG.info("Preuzeto {} komentara za knjigu sa ID={}", commentList.size(), bookId);
        } catch (SQLException e) {
            LOG.error("Greška prilikom preuzimanja komentara za knjigu sa ID={}", bookId, e);
        }
        return commentList;
    }
}
