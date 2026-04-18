package com.example.library.model;

public enum MembershipType {
    STANDARD(3),    // max 3 books
    PREMIUM(5),     // max 5 books
    STUDENT(2);     // max 2 books

    private final int maxBooks;

    MembershipType(int maxBooks) {
        this.maxBooks = maxBooks;
    }

    public int getMaxBooks() {
        return maxBooks;
    }
}
