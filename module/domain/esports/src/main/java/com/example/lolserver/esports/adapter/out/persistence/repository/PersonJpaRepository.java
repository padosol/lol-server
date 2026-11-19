package com.example.lolserver.esports.adapter.out.persistence.repository;

import com.example.lolserver.esports.adapter.out.persistence.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonJpaRepository extends JpaRepository<PersonEntity, String> {
}
