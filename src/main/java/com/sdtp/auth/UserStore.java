package com.sdtp.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserStore {

    public static class User {
        public String username;
        public String password;
        public String tenantId;
        public Set<String> roles;

        public User(String username, String password, String tenantId, Set<String> roles) {
            this.username = username;
            this.password = password;
            this.tenantId = tenantId;
            this.roles = roles;
        }
    }

    private static final Map<String, User> USERS = new HashMap<>();

    static {
        USERS.put("adminA", new User("adminA", "adminApass", "tenant-A", Set.of("admin")));
        USERS.put("viewerA", new User("viewerA", "viewerApass", "tenant-A", Set.of("viewer")));
        USERS.put("adminB", new User("adminB", "adminBpass", "tenant-B", Set.of("admin")));
        USERS.put("viewerB", new User("viewerB", "viewerBpass", "tenant-B", Set.of("viewer")));
    }

    public static User getByUsername(String username) {
        return USERS.get(username);
    }
}
