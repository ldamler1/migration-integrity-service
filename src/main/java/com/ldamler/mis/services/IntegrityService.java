package com.ldamler.mis.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.ldamler.mis.models.User;
import com.ldamler.mis.models.UserDiff;
import com.ldamler.mis.models.UserDiff.UserDiffBuilder;

import org.springframework.stereotype.Service;

@Service
public class IntegrityService {
    
    /**
     * @param oldAccounts hash map of old accounts
     * @param newAccounts hashmap of migrated accounts
     * @return list of differences between old and new account maps
     */
    public List<UserDiff> integrityReport(Map<String, User> oldAccounts, Map<String, User> newAccounts) {
        var differenceReport = new ArrayList<UserDiff>();

        //loop through map of old accounts
        for (Entry<String, User> oldUser : oldAccounts.entrySet()) {
            /*
             * 1. if new account contains current oldUser key compare values of each at the given key
             * 2. otherwise if newAccounts does not include the current key then the given oldUser was not migrated to the new DB
             */
            if (newAccounts.containsKey(oldUser.getKey())) {
                User matchingAccount = newAccounts.get(oldUser.getKey());

                // true if old email does not match migrated email
                boolean mismatchedEmail = !matchingAccount.getEmail().equals(oldUser.getValue().getEmail());
                // true if old name does not match migrated name
                boolean mismatchedName = !matchingAccount.getName().equals(oldUser.getValue().getName());

                UserDiffBuilder tempDiff = UserDiff.builder()
                        .id(oldUser.getKey())
                        .migratedName(matchingAccount.getName())
                        .migratedEmail(matchingAccount.getEmail())
                        .oldName(oldUser.getValue().getName())
                        .oldEmail(oldUser.getValue().getEmail());

                /* 
                 * It's easier to lump all mistmatched data into one status code with one incluse check but breaking
                 * out the conditions into seperate checks allows for a more descriptive integrity check report
                 * 
                 * Running against all records the first scenario doesn't occurs but it's better to be safe than sorry.
                 * As far as testing goes if the scenario does occur the proper status code will be returned 
                 */
                if (mismatchedEmail && mismatchedName) {
                    tempDiff.status("MISMATCHED NAME AND EMAIL").build();
                    differenceReport.add(tempDiff.build());
                } else if (mismatchedName && !mismatchedEmail) {
                    tempDiff.status("MISMATCHED NAME").build();
                    differenceReport.add(tempDiff.build());
                } else if (mismatchedEmail) {
                    tempDiff.status("MISMATCHED EMAIL").build();
                    differenceReport.add(tempDiff.build());
                }
                /*
                 * remove key value pair from newAccounts regardless of mismatched data. Needed for later step
                 * 
                 * by this point any mismatches have been added to the report and if there are
                 * no mismatches we dont care about the given key
                 */
                newAccounts.remove(oldUser.getKey());
            } else {
                UserDiff tempDiff = UserDiff.builder().id(oldUser.getKey()).migratedName("").migratedEmail("")
                        .oldName(oldUser.getValue().getName()).oldEmail(oldUser.getValue().getEmail())
                        .status("NOT MIGRATED").build();
                differenceReport.add(tempDiff);
            }
        }

        /* 
         * once all matching keys have been found, verified, and removed between oldAccounts and newAccounts any remaining keys
         * do not exist in the oldAccounts/oldDb meaning they are new to the new accounts db
         */
        for (Entry<String, User> remainingUsers : newAccounts.entrySet()) {
            UserDiff tempDiff = UserDiff.builder().id(remainingUsers.getKey())
                    .migratedName(remainingUsers.getValue().getName())
                    .migratedEmail(remainingUsers.getValue().getEmail()).oldName("").oldEmail("")
                    .status("NEWLY CREATED").build();
            differenceReport.add(tempDiff);
        }

        return differenceReport;
    }

    /**
     * @param result result of the query as User.class
     * @param accounts map of accounts to be added
     * @throws SQLException
     */
    public void getAccounts(ResultSet result, Map<String, User> accounts) throws SQLException {
        while (result.next()) {
            // based on the story criteria each field should have a value so calling get should never result in a null pointer error
            String id = result.getString("id");
            String name = result.getString("name");
            String email = result.getString("email");

            User user = User.builder().id(id).name(name).email(email).build();
            accounts.put(id, user);
        }
    }


    // generic csv writer method
    public byte[] generateCsv(List<?> result, Class<?> classType) {
        CsvMapper mapper = new CsvMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CsvSchema schema = mapper.schemaFor(classType).withHeader();

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            mapper.writer(schema).writeValue(writer, result);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write csv", e);
        }

        return outputStream.toByteArray();
    }
}
