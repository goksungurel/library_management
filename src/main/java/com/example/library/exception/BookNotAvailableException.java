package com.example.library.exception;

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String title) {
        super("No available copies of: " + title);
    }
}
