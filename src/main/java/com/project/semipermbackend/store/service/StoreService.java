package com.project.semipermbackend.store.service;

import com.project.semipermbackend.common.error.ErrorCode;
import com.project.semipermbackend.common.error.exception.EntityAlreadyExistsException;
import com.project.semipermbackend.common.error.exception.EntityNotFoundException;
import com.project.semipermbackend.domain.code.PostSorting;
import com.project.semipermbackend.domain.member.Member;
import com.project.semipermbackend.domain.store.MemberZzimStore;
import com.project.semipermbackend.domain.store.MemberZzimStoreRepository;
import com.project.semipermbackend.domain.store.Store;
import com.project.semipermbackend.domain.store.StoreRepository;
import com.project.semipermbackend.member.service.MemberService;
import com.project.semipermbackend.store.dto.StoreZzimCreationDto;
import com.project.semipermbackend.store.dto.StoreZzimFindDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StoreService {
    private final MemberService memberService;
    private final StoreRepository storeRepository;
    private final MemberZzimStoreRepository memberZzimStoreRepository;

    // TODO 테스트 필요
    // base64 : binary를 ascii 영역의 문자열로 인코딩
    @Transactional
    public StoreZzimCreationDto.Response create(Long memberId, StoreZzimCreationDto.Request storeSaveCreation) {
        String encodedPlaceId = Base64.getEncoder().encodeToString(storeSaveCreation.getPlaceId().getBytes());
        Member member = memberService.getMemberByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND_MEMBER));

        // 1. Store 조회
        Store store = createOrFindExistingStore(encodedPlaceId);

        // 2. 찜 생성
        // 이미 찜했으면 Error
        Optional<MemberZzimStore> optionalMemberZzimStore = memberZzimStoreRepository.findByMemberAndStore(member, store);
        if (optionalMemberZzimStore.isPresent()) {
            throw new EntityAlreadyExistsException(ErrorCode.ALREADY_MEMBER_ZZIM_STORE);
        }
        MemberZzimStore memberZzimStore = MemberZzimStore.builder().member(member).store(store).build();
        memberZzimStoreRepository.save(memberZzimStore);

        // 3. 연관 엔티티 처리
        store.addZzimStore(memberZzimStore);
        member.addZzimStore(memberZzimStore);

        return new StoreZzimCreationDto.Response(memberZzimStore.getMemberZzimStoreId());
    }

    /**
     * 나의 찜 조회
     * @param page
     * @param perSize
     * @param memberId
     */
    public Page<StoreZzimFindDto.Response> find(int page, int perSize, Long memberId) {
        Member member = memberService.getMemberByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NOT_FOUND_MEMBER));

        Pageable pageable = PageRequest.of(page, perSize);

        return memberZzimStoreRepository.findAllByMemberOrderBy(pageable, member, PostSorting.LATEST)   // default 정렬
                .map(StoreZzimFindDto.Response::from);
    }

    public Store createOrFindExistingStore(String placeId) {
        Optional<Store> optionalStore = storeRepository.getStoreByEncodedPlaceId(placeId);
        // 1.1 없는 사업장이면 placeId 와 함께 Store 테이블 저장 (찜/리뷰 이력 X)
        if (optionalStore.isEmpty()) {
            // 사업장 생성
            return storeRepository.save(Store.create(placeId));
        }

        // 1.2 사업장 존재하면
        return optionalStore.get();
    }

    // 찜 제거


}
