package com.example.library.controller;

import com.example.library.dto.BorrowRequest;
import com.example.library.dto.BorrowResponse;
import com.example.library.service.BorrowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public ResponseEntity<BorrowResponse> borrowBook(@Valid @RequestBody BorrowRequest request) {
        BorrowResponse response = borrowService.borrowBook(request.getBookId(), request.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/return")
    public BorrowResponse returnBook(@PathVariable Long id) {
        return borrowService.returnBook(id);
    }

    @GetMapping("/member/{memberId}")
    public List<BorrowResponse> getMemberHistory(@PathVariable Long memberId) {
        return borrowService.getMemberBorrowHistory(memberId);
    }

    @GetMapping("/member/{memberId}/active")
    public List<BorrowResponse> getActiveBorrows(@PathVariable Long memberId) {
        return borrowService.getActiveBorrows(memberId);
    }

    @GetMapping("/overdue")
    public List<BorrowResponse> getOverdueRecords() {
        return borrowService.getOverdueRecords();
    }
}
