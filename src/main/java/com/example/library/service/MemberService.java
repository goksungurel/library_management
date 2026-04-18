package com.example.library.service;

import com.example.library.exception.MemberNotFoundException;
import com.example.library.model.Member;
import com.example.library.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    @Transactional
    public Member createMember(Member member) {
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new IllegalStateException("A member with email " + member.getEmail() + " already exists");
        }
        return memberRepository.save(member);
    }

    @Transactional
    public Member updateMember(Long id, Member updatedMember) {
        Member existing = getMemberById(id);
        existing.setName(updatedMember.getName());
        existing.setEmail(updatedMember.getEmail());
        existing.setPhone(updatedMember.getPhone());
        existing.setMembershipType(updatedMember.getMembershipType());
        return memberRepository.save(existing);
    }

    @Transactional
    public void deactivateMember(Long id) {
        Member member = getMemberById(id);
        member.setActive(false);
        memberRepository.save(member);
    }

    public List<Member> getActiveMembers() {
        return memberRepository.findByActiveTrue();
    }
}
