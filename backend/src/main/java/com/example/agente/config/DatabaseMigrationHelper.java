package com.example.agente.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationHelper implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if column username exists in owners table
            Boolean hasUsername = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'owners' AND column_name = 'username')",
                Boolean.class
            );
            
            if (Boolean.TRUE.equals(hasUsername)) {
                System.out.println("[Migration] Found old 'username' column in 'owners' table. Migrating data to 'email'...");
                
                // Copy values from username to email where email is null
                jdbcTemplate.execute("UPDATE owners SET email = username WHERE email IS NULL");
                
                // Verify no null emails exist
                Integer nullEmailCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM owners WHERE email IS NULL",
                    Integer.class
                );
                
                if (nullEmailCount != null && nullEmailCount == 0) {
                    // Set email to NOT NULL
                    jdbcTemplate.execute("ALTER TABLE owners ALTER COLUMN email SET NOT NULL");
                    System.out.println("[Migration] 'email' column set to NOT NULL.");
                    
                    // Drop old username column
                    jdbcTemplate.execute("ALTER TABLE owners DROP COLUMN username");
                    System.out.println("[Migration] Old 'username' column dropped successfully.");
                } else {
                    System.err.println("[Migration] Cannot set 'email' column to NOT NULL because some records still have null email values.");
                }
            } else {
                System.out.println("[Migration] No old 'username' column found. Database is up to date.");
            }
        } catch (Exception e) {
            System.err.println("[Migration] Error during database migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
