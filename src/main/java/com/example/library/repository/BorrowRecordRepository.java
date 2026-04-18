package com.example.library.repository;

import com.example.library.model.BorrowRecord;
import com.example.library.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByMemberId(Long memberId);

    List<BorrowRecord> findByBookId(Long bookId);

    List<BorrowRecord> findByMemberIdAndStatus(Long memberId, BorrowStatus status);

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.member.id = :memberId AND br.status = 'BORROWED'")
    int countActiveBorrowsByMember(@Param("memberId") Long memberId);

    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWED' AND br.dueDate < :today")
    List<BorrowRecord> findOverdueRecords(@Param("today") LocalDate today);

    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, BorrowStatus status);
}
