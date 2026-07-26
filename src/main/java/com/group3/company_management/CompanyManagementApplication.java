package com.group3.company_management;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableScheduling
public class CompanyManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanyManagementApplication.class, args);
	}

	@Bean
	public CommandLineRunner dropCheckConstraint(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {	
				jdbcTemplate.execute("ALTER TABLE contracts DROP CONSTRAINT IF EXISTS contracts_status_check");
				System.out.println("✅ Successfully dropped constraint 'contracts_status_check'");
			} catch (Exception e) {
				System.err.println("❌ Failed to drop constraint: " + e.getMessage());
			}
		};
	}

}

