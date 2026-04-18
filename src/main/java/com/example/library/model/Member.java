package com.example.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Valid email is required")
    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    private LocalDate membershipDate;

    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    private boolean active = true;

    public Member() {}

    public Member(String name, String email, MembershipType membershipType) {
        this.name = name;
        this.email = email;
        this.membershipType = membershipType;
        this.membershipDate = LocalDate.now();
        this.active = true;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getMembershipDate() { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate) { this.membershipDate = membershipDate; }

    public MembershipType getMembershipType() { return membershipType; }
    public void setMembershipType(MembershipType membershipType) { this.membershipType = membershipType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
