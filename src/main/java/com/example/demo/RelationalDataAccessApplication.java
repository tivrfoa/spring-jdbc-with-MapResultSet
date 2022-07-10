package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.tivrfoa.mapresultset.api.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class RelationalDataAccessApplication implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(RelationalDataAccessApplication.class);

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Query
  final String listByFirstName = """
        SELECT id, first_name, last_name
        FROM customers
        WHERE first_name = ?
        """;

  @Override
  public void run(String... strings) throws Exception {

    createTables();
    insertRecords();
    queryUsingJdbcTemplate();

    System.out.println("-------------  Using MapResultSet ----------------------");

    Connection connection = jdbcTemplate.getDataSource().getConnection();
    PreparedStatement preparedStatement = connection.prepareStatement(listByFirstName);
    preparedStatement.setString(1, "Josh");
    ResultSet resultSet = preparedStatement.executeQuery();
    List<Customer> customers = MapResultSet.listByFirstName(resultSet);
    System.out.println(customers);

  }

  private void queryUsingJdbcTemplate() {
    log.info("Querying for customer records where first_name = 'Josh':");
    jdbcTemplate.query(
        "SELECT id, first_name, last_name FROM customers WHERE first_name = ?", new Object[] { "Josh" },
        (rs, rowNum) -> new Customer(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"))
    ).forEach(customer -> log.info(customer.toString()));
  }

  private void insertRecords() {
    // Split up the array of whole names into an array of first/last names
    List<Object[]> splitUpNames = Arrays.asList("John Woo", "Jeff Dean", "Josh Bloch", "Josh Long").stream()
        .map(name -> name.split(" "))
        .collect(Collectors.toList());

    // Use a Java 8 stream to print out each tuple of the list
    splitUpNames.forEach(name -> log.info(String.format("Inserting customer record for %s %s", name[0], name[1])));

    // Uses JdbcTemplate's batchUpdate operation to bulk load data
    jdbcTemplate.batchUpdate("INSERT INTO customers(first_name, last_name) VALUES (?,?)", splitUpNames);
  }

  public void createTables() {
    log.info("Creating tables");

    jdbcTemplate.execute("DROP TABLE customers IF EXISTS");
    jdbcTemplate.execute("CREATE TABLE customers(" +
        "id SERIAL, first_name VARCHAR(255), last_name VARCHAR(255))");
  }

  public static void main(String args[]) {
    SpringApplication.run(RelationalDataAccessApplication.class, args);
  }
}