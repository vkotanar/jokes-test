package com.nokia.jokesapi.service;

import com.nokia.jokesapi.exception.DataAccessException;
import com.nokia.jokesapi.model.JokeEntity;
import com.nokia.jokesapi.model.JokeSaveDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;


@Service
public class JokeInsertService {

	Logger log= LoggerFactory.getLogger(JokeInsertService.class);
	@Autowired
	private R2dbcEntityTemplate r2dbcEntityTemplate;

	 Mono<JokeSaveDto> insertIfNotExists(JokeSaveDto jokeSaveDto) {
		
		log.info("Attempting to insert joke with ID: {}", jokeSaveDto.getId());

		
		Query query = Query.query(Criteria.where("id").is(jokeSaveDto.getId()));

		return r2dbcEntityTemplate.select(JokeEntity.class).matching(query).one().flatMap(existingJoke -> {
			// Joke already exists
			log.warn("Joke with ID {} already exists. No insertion performed.", jokeSaveDto.getId());
			return Mono.just(jokeSaveDto);
		}).switchIfEmpty(r2dbcEntityTemplate.insert(JokeEntity.class)
				.using(new JokeEntity(jokeSaveDto.getId(), jokeSaveDto.getQuestion(), jokeSaveDto.getAnswer()))
				.doOnSuccess(savedJoke -> log.info("Successfully inserted joke with ID: {}", savedJoke.getId()))
				.doOnError(ex -> log.error("Failed to insert joke with ID: {}. Error: {}", jokeSaveDto.getId(),
						ex.getMessage()))
				.map(savedJoke -> jokeSaveDto) // Return the saved joke DTO
				.onErrorResume(ex -> {
		
					return Mono.error(new DataAccessException("Database insertion failed: " + ex.getMessage()));
				})).onErrorResume(ex -> {
		
					log.error("An unexpected error occurred: {}", ex.getMessage());
					return Mono
							.error(new DataAccessException("An unexpected error occurred while inserting the joke."));
				});
	}

	
}
