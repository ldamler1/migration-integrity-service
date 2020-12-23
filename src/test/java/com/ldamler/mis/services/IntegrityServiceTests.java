package com.ldamler.mis.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;

import com.ldamler.mis.models.User;
import com.ldamler.mis.models.UserDiff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class IntegrityServiceTests {
    
    @InjectMocks private IntegrityService integrityService;

    private List<UserDiff> EXPECTED_DIFF = List.of(UserDiff.builder().id("3").oldName("Jack Black").oldEmail("jack@tenaciousD.com").migratedName("Jack White").migratedEmail("jack@tenaciousD.com").status("MISMATCHED NAME").build(),
                                                   UserDiff.builder().id("4").oldName("Tom").oldEmail("tom@hanks.com").migratedName("Tom").migratedEmail("tim@hanks.com").status("MISMATCHED EMAIL").build(),
                                                   UserDiff.builder().id("5").oldName("Peter").oldEmail("peter@spider-man.com").migratedName("Miles").migratedEmail("miles@spider-man.com").status("MISMATCHED NAME AND EMAIL").build(),
                                                   UserDiff.builder().id("6").oldName("").oldEmail("").migratedName("Mr Bean").migratedEmail("mr@bean.com").status("NEWLY CREATED").build());

    @Test
    public void testIntegrityReport() {
        var oldAccounts = new HashMap<String, User>();//Map.of("1",oldUser1, "2",oldUser2, "3",oldUser3, "4",oldUser4, "5",oldUser5);
        oldAccounts.put("1", createUser("1", "John Shepard", "john@normandy.com"));
        oldAccounts.put("2", createUser("2", "Jane Shepard", "jane@normandy.com"));
        oldAccounts.put("3", createUser("3", "Jack Black", "jack@tenaciousD.com"));
        oldAccounts.put("4", createUser("4", "Tom", "tom@hanks.com"));
        oldAccounts.put("5", createUser("5", "Peter", "peter@spider-man.com"));

        var newAccounts = new HashMap<String, User>();//Map.of("1",newUser1, "2",newUser2, "3",newUser3, "4",newUser4, "5",newUser5);
        newAccounts.put("1", createUser("1", "John Shepard", "john@normandy.com"));
        newAccounts.put("2", createUser("2", "Jane Shepard", "jane@normandy.com"));
        newAccounts.put("3", createUser("3", "Jack White", "jack@tenaciousD.com"));
        newAccounts.put("4", createUser("4", "Tom", "tim@hanks.com"));
        newAccounts.put("5", createUser("5", "Miles", "miles@spider-man.com"));
        newAccounts.put("6", createUser("6", "Mr Bean", "mr@bean.com"));

        List<UserDiff> diff = integrityService.integrityReport(oldAccounts, newAccounts);

        assertEquals(4, diff.size());
        assertEquals(EXPECTED_DIFF.get(0), diff.get(0));
        assertEquals(EXPECTED_DIFF.get(1), diff.get(1));
        assertEquals(EXPECTED_DIFF.get(2), diff.get(2));
        assertEquals(EXPECTED_DIFF.get(3), diff.get(3));
    }

    @Test
    public void testGenerateCsv() {
        byte[] response = integrityService.generateCsv(EXPECTED_DIFF, UserDiff.class);
        assertNotNull(response);
    }

    private User createUser(String id, String name, String email) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email).build();
    }
    
}
