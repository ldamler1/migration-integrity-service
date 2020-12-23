package com.ldamler.mis.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDiff {
    private String id;
    private String oldName;
    private String oldEmail;
    private String migratedName;
    private String migratedEmail;
    private String status;
}
