/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.repository.isolated;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cassandra.support.exception.CassandraInvalidQueryException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.convert.CustomConversions;
import org.springframework.data.cassandra.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.domain.AllPossibleTypes;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraType;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.test.integration.support.AbstractSpringDataEmbeddedCassandraIntegrationTest;
import org.springframework.data.cassandra.test.integration.support.IntegrationTestConfig;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.datastax.driver.core.DataType.Name;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;

/**
 * Integration tests for various query method parameter types.
 *
 * @author Mark Paluch
 * @see DATACASS-296
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@SuppressWarnings("Since15")
public class RepositoryQueryMethodParameterTypesIntegrationTests
		extends AbstractSpringDataEmbeddedCassandraIntegrationTest {

	@Configuration
	@EnableCassandraRepositories(basePackageClasses = RepositoryQueryMethodParameterTypesIntegrationTests.class,
			considerNestedRepositories = true)
	public static class Config extends IntegrationTestConfig {

		@Override
		public String[] getEntityBasePackages() {
			return new String[] { AllPossibleTypes.class.getPackage().getName() };
		}

		@Override
		public SchemaAction getSchemaAction() {
			return SchemaAction.RECREATE_DROP_UNUSED;
		}
	}

	@Autowired AllPossibleTypesRepository allPossibleTypesRepository;
	@Autowired Session session;
	@Autowired BasicCassandraMappingContext mappingContext;
	@Autowired MappingCassandraConverter converter;

	@Before
	public void setUp() throws Exception {
		allPossibleTypesRepository.deleteAll();
	}

	/**
	 * @see DATACASS-296
	 */
	@Test
	public void shouldFindByLocalDate() {

		session.execute("CREATE INDEX IF NOT EXISTS allpossibletypes_localdate ON allpossibletypes ( localdate )");

		AllPossibleTypes allPossibleTypes = new AllPossibleTypes();

		allPossibleTypes.setId("id");
		allPossibleTypes.setLocalDate(LocalDate.now());

		allPossibleTypesRepository.save(allPossibleTypes);

		List<AllPossibleTypes> result = allPossibleTypesRepository.findWithCreatedDate(allPossibleTypes.getLocalDate());

		assertThat(result, hasSize(1));
		assertThat(result, contains(allPossibleTypes));
	}

	/**
	 * @see DATACASS-296
	 */
	@Test
	public void shouldFindByAnnotatedDateParameter() {

		CustomConversions customConversions = new CustomConversions(
			Collections.singletonList(new DateToLocalDateConverter()));

		mappingContext.setCustomConversions(customConversions);
		converter.setCustomConversions(customConversions);
		converter.afterPropertiesSet();

		session.execute("CREATE INDEX IF NOT EXISTS allpossibletypes_date ON allpossibletypes ( date )");

		AllPossibleTypes allPossibleTypes = new AllPossibleTypes();

		LocalDate localDate = LocalDate.now();
		Instant instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC);

		allPossibleTypes.setId("id");
		allPossibleTypes.setDate(com.datastax.driver.core.LocalDate.fromYearMonthDay(localDate.getYear(),
				localDate.getMonthValue(), localDate.getDayOfMonth()));

		allPossibleTypesRepository.save(allPossibleTypes);

		List<AllPossibleTypes> result = allPossibleTypesRepository.findWithAnnotatedDateParameter(Date.from(instant));

		assertThat(result, hasSize(1));
		assertThat(result, contains(allPossibleTypes));
	}

	/**
	 * @see <a href="https://jira.spring.io/browse/DATACASS-296">DATACASS-296</a>
	 * @see <a href="https://jira.spring.io/browse/DATACASS-304">DATACASS-304</a>
	 */
	@Test(expected = CassandraInvalidQueryException.class)
	public void shouldThrowExceptionUsingWrongMethodParameter() {
		session.execute("CREATE INDEX IF NOT EXISTS allpossibletypes_date ON allpossibletypes ( date )");
		allPossibleTypesRepository.findWithDateParameter(Date.from(Instant.ofEpochSecond(44234123421L)));
	}

	/**
	 * @see DATACASS-296
	 */
	@Test
	public void shouldFindByZoneId() {

		ZoneId zoneId = ZoneId.of("Europe/Paris");
		session.execute("CREATE INDEX IF NOT EXISTS allpossibletypes_zoneid ON allpossibletypes ( zoneid )");

		AllPossibleTypes allPossibleTypes = new AllPossibleTypes();

		allPossibleTypes.setId("id");
		allPossibleTypes.setZoneId(zoneId);

		allPossibleTypesRepository.save(allPossibleTypes);

		List<AllPossibleTypes> result = allPossibleTypesRepository.findWithZoneId(zoneId);

		assertThat(result, hasSize(1));
		assertThat(result, contains(allPossibleTypes));
	}

	/**
	 * @see DATACASS-296
	 */
	@Test
	public void shouldFindByOptionalOfZoneId() {

		ZoneId zoneId = ZoneId.of("Europe/Paris");
		session.execute("CREATE INDEX IF NOT EXISTS allpossibletypes_zoneid ON allpossibletypes ( zoneid )");

		AllPossibleTypes allPossibleTypes = new AllPossibleTypes();

		allPossibleTypes.setId("id");
		allPossibleTypes.setZoneId(zoneId);

		allPossibleTypesRepository.save(allPossibleTypes);

		List<AllPossibleTypes> result = allPossibleTypesRepository.findWithZoneId(Optional.of(zoneId));

		assertThat(result, hasSize(1));
		assertThat(result, contains(allPossibleTypes));
	}

	private interface AllPossibleTypesRepository extends CrudRepository<AllPossibleTypes, String> {

		@Query("select * from allpossibletypes where localdate = ?0")
		List<AllPossibleTypes> findWithCreatedDate(java.time.LocalDate createdDate);

		@Query("select * from allpossibletypes where zoneid = ?0")
		List<AllPossibleTypes> findWithZoneId(ZoneId zoneId);

		@Query("select * from allpossibletypes where date = ?0")
		List<AllPossibleTypes> findWithAnnotatedDateParameter(@CassandraType(type = Name.DATE) Date timestamp);

		@Query("select * from allpossibletypes where date = ?0")
		List<AllPossibleTypes> findWithDateParameter(Date timestamp);

		@Query("select * from allpossibletypes where zoneid = ?0")
		List<AllPossibleTypes> findWithZoneId(Optional<ZoneId> zoneId);
	}

	private static class DateToLocalDateConverter implements Converter<Date, com.datastax.driver.core.LocalDate> {

		@Override
		public com.datastax.driver.core.LocalDate convert(Date source) {

			LocalDate localDate = LocalDateTime.ofInstant(source.toInstant(), ZoneOffset.UTC.normalized()).toLocalDate();

			return com.datastax.driver.core.LocalDate.fromYearMonthDay(localDate.getYear(), localDate.getMonthValue(),
					localDate.getDayOfMonth());
		}
	}
}
