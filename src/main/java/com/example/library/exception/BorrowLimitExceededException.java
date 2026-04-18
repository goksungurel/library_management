package com.example.library.exception;

public class BorrowLimitExceededException extends RuntimeException {
    public BorrowLimitExceededException(String memberName, int maxBooks) {
        super(memberName + " has reached the borrowing limit of " + maxBooks + " books");
    }
}
