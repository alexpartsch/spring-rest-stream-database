package com.github.shwocase.spring.stream_db2rest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface FakeDataRepository extends JpaRepository<FakeDataEntity, UUID> {

    Stream<FakeDataEntity> findByIdNotNull();

}
