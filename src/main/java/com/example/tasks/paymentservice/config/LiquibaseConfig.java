package com.example.tasks.paymentservice.config;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

	@Value("${spring.data.mongodb.uri}")
	private String mongoUri;

	@Value("${spring.liquibase.change-log}")
	private String changeLogPath;

	@PostConstruct
	public void runLiquibaseMigration() throws Exception {
		MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
				.openDatabase(mongoUri, null, null, null, null);
		try (Liquibase liquibase = new Liquibase(changeLogPath, new ClassLoaderResourceAccessor(), database)) {
			liquibase.update(new Contexts(), new LabelExpression());
		}
	}
}