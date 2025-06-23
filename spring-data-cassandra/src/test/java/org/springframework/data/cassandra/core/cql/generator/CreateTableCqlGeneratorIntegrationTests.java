/*
 * Copyright 2017-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.cassandra.core.cql.generator;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.keyspace.CreateTableSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.TableOption;
import org.springframework.data.cassandra.test.util.AbstractKeyspaceCreatingIntegrationTests;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataTypes;

/**
 * Integration tests for {@link CreateTableCqlGenerator}.
 *
 * @author Matthew T. Adams
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Seungho Kang
 */
class CreateTableCqlGeneratorIntegrationTests extends AbstractKeyspaceCreatingIntegrationTests {

	@BeforeEach
	void setUp() {

		session.execute("DROP TABLE IF EXISTS person;");
		session.execute("DROP TABLE IF EXISTS address;");

		session.execute("DROP KEYSPACE IF EXISTS CqlGenerator_it;");
		session.execute(
				"CREATE KEYSPACE CqlGenerator_it WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};");
	}

	@Test // DATACASS-518
	void shouldGenerateSimpleTable() {

		CreateTableSpecification table = CreateTableSpecification.createTable("person") //
				.partitionKeyColumn("id", DataTypes.ASCII) //
				.clusteredKeyColumn("date_of_birth", DataTypes.DATE) //
				.column("name", DataTypes.ASCII);

		session.execute(CqlGenerator.toCql(table));
	}

	@Test // DATACASS-518
	void shouldGenerateTableWithClusterKeyOrdering() {

		CreateTableSpecification table = CreateTableSpecification.createTable("person") //
				.partitionKeyColumn("id", DataTypes.ASCII) //
				.partitionKeyColumn("country", DataTypes.ASCII) //
				.clusteredKeyColumn("date_of_birth", DataTypes.DATE, Ordering.ASCENDING) //
				.clusteredKeyColumn("age", DataTypes.SMALLINT) //
				.column("name", DataTypes.ASCII);

		session.execute(CqlGenerator.toCql(table));

		TableMetadata person = session.getMetadata().getKeyspace(getKeyspace()).flatMap(it -> it.getTable("person")).get();
		assertThat(person.getPartitionKey()).hasSize(2);
		assertThat(person.getClusteringColumns()).hasSize(2);
	}

	@Test // DATACASS-518
	void shouldGenerateTableWithClusterKeyAndOptions() {

		CreateTableSpecification table = CreateTableSpecification.createTable("person") //
				.partitionKeyColumn("id", DataTypes.ASCII) //
				.clusteredKeyColumn("date_of_birth", DataTypes.DATE, Ordering.ASCENDING) //
				.column("name", DataTypes.ASCII).with(TableOption.COMPACT_STORAGE);

		session.execute(CqlGenerator.toCql(table));

		TableMetadata person = session.getMetadata().getKeyspace(getKeyspace()).flatMap(it -> it.getTable("person")).get();
		assertThat(person.getPartitionKey()).hasSize(1);
		assertThat(person.getClusteringColumns()).hasSize(1);
	}

	@Test // GH-921
	void shouldGenerateTableInOtherKeyspace() {

		CreateTableSpecification table = CreateTableSpecification
				.createTable(CqlIdentifier.fromCql("CqlGenerator_it"), CqlIdentifier.fromCql("person")) //
				.partitionKeyColumn("id", DataTypes.ASCII) //
				.clusteredKeyColumn("date_of_birth", DataTypes.DATE, Ordering.ASCENDING) //
				.column("name", DataTypes.ASCII).with(TableOption.COMPACT_STORAGE);

		session.execute(CqlGenerator.toCql(table));

		TableMetadata person = session.getMetadata().getKeyspace("CqlGenerator_it").flatMap(it -> it.getTable("person"))
				.get();
		assertThat(person.getPartitionKey()).hasSize(1);
		assertThat(person.getClusteringColumns()).hasSize(1);
	}

	@Test // GH-1584
	void shouldGenerateTableWithOptions() {

		CreateTableSpecification spec = CreateTableSpecification.createTable("person")
				.partitionKeyColumn("id", DataTypes.INT) //
				.clusteredKeyColumn("date_of_birth", DataTypes.DATE, Ordering.ASCENDING) //
				.column("name", DataTypes.ASCII) //
				.with(TableOption.GC_GRACE_SECONDS, 86400L).with(TableOption.DEFAULT_TIME_TO_LIVE, 3600L)
				.with(TableOption.CDC, true).with(TableOption.SPECULATIVE_RETRY, "99PERCENTILE")
				.with(TableOption.MEMTABLE_FLUSH_PERIOD_IN_MS, 10000L).with(TableOption.CRC_CHECK_CHANCE, 0.9d)
				.with(TableOption.MIN_INDEX_INTERVAL, 128L).with(TableOption.MAX_INDEX_INTERVAL, 2048L)
				.with(TableOption.READ_REPAIR, "BLOCKING");

		session.execute(CqlGenerator.toCql(spec));

		TableMetadata meta = session.getMetadata().getKeyspace(getKeyspace()).flatMap(it -> it.getTable("person")).get();
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.GC_GRACE_SECONDS.getName()), 86400);

		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.DEFAULT_TIME_TO_LIVE.getName()), 3600);
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.SPECULATIVE_RETRY.getName()), "99p");
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.MEMTABLE_FLUSH_PERIOD_IN_MS.getName()), 10000);
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.CRC_CHECK_CHANCE.getName()), 0.9);
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.MIN_INDEX_INTERVAL.getName()), 128);
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.MAX_INDEX_INTERVAL.getName()), 2048);
		assertThat(meta.getOptions()) //
				.containsEntry(CqlIdentifier.fromCql(TableOption.READ_REPAIR.getName()), "BLOCKING");
	}
}
