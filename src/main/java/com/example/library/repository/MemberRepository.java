package com.example.library.repository;

import com.example.library.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    List<Member> findByActiveTrue();

    List<Member> findByNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);
}
