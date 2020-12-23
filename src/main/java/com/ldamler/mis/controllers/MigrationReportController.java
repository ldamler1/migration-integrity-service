package com.ldamler.mis.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import com.ldamler.mis.models.User;
import com.ldamler.mis.models.UserDiff;
import com.ldamler.mis.services.IntegrityService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MigrationReportController {

    // static query for shared fields in each database
    private static String ACCOUNTS_QUERY = "SELECT id, name, email FROM accounts;";

    private final IntegrityService integrityService;

    @ResponseBody
    @GetMapping(value = "/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> generateReport(@RequestParam(name = "oldPort") String oldPort,
                                                 @RequestParam(name = "newPort") String newPort) {
        var oldAccounts = new HashMap<String, User>();
        var newAccounts = new HashMap<String, User>();

        try {
            /*
             * Generally data sources should be configured and autowired using CrudRepositry and a porperties file
             * but given there needs to be two connections for the time being it's easier to create two basic
             * database connections and run a static query to gather shared entities between tables
             * 
             * postgresql offers dblink which should allow for writing queries across databases
             * which would speed up the report generation but would also require a more complex query
             * so for now with the given data scale it would be better to stick with two basic queries
             * and compare programtically
             */
            Connection connectionOld = createConnection("localhost:" + oldPort + "/old", "old", "hehehe");
            log.info("Connected to old host on {}", oldPort);
            Connection connectionNew = createConnection("localhost:" + newPort + "/new", "new", "hahaha");
            log.info("Connected to new host on {}", newPort);

            Statement statement = connectionOld.createStatement();

            // execute select on both databases for shared fields. favorite_flavor is irrelavent to the integrity check and is ignored
            ResultSet result = statement.executeQuery(ACCOUNTS_QUERY);
            integrityService.getAccounts(result, oldAccounts);

            statement = connectionNew.createStatement();
            result = statement.executeQuery(ACCOUNTS_QUERY);
            integrityService.getAccounts(result, newAccounts);

            statement.close();
            result.close();
            connectionOld.close();
            connectionNew.close();

        } catch (Exception e) {
            /* 
             * With a larger scale service it's be prefered to make a custom error controller and excpetion handler
             * and catch specific errors. For now we'll throw a runtime exception for simplicity sake
             */
            throw new RuntimeException("Can't Query " + e.getMessage());
        }

        var differenceReport = integrityService.integrityReport(oldAccounts, newAccounts);

        // the resultint diff report is too large to display in swagger so the output is written to an attached, easy to read, csv file
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"integrity_report.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(integrityService.generateCsv(differenceReport, UserDiff.class));
    }


    /*
     * Since we aren't relying on an autowired datasource and CrudRepo we create our basic sql db connection here
     */
    private Connection createConnection(String host, String username, String password) {
        Connection conn;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://" + host, username, password);
            conn.setAutoCommit(true);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Unable to connect to " + host);
        }

        return conn;
    }

}
