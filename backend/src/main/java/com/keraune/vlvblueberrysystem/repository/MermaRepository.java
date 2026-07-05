package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Merma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MermaRepository extends JpaRepository<Merma, Long> {
    List<Merma> findAllByOrderByFechaMermaDescIdDesc();
    List<Merma> findByLoteTrazableId(Long loteTrazableId);
}
