package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRapository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class FinanceService {
    private final CompanyRepository companyRepository;
    private final DividendRapository dibDividendRapository;

    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    /*
    캐시에 데이터가 없으면 이 로직을 실행
    캐시에 데이터가 있으면 이 로직을 실행하지 않고, 캐시에 있는 데이터를 반환해준다.

     */
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search company -> " + companyName);
        // 1. 회사명을 기준으로 회사정보를 조회
        // companyName이 없거나 오타로 인해 검색결과가 없을 수도 있다.
        CompanyEntity company = this.companyRepository.findByName(companyName)
                                    .orElseThrow(() -> new NoCompanyException());

        // 2. 조회된 회사 ID로 배당금 정보 조회
        // CompanyId 값으로 조회를 하여 List 형태의 dividendEntities를 받아오자
        List<DividendEntity> dividendEntities = this.dibDividendRapository.findAllByCompanyId(company.getId());

        // 3. 정보 조합 후 반환
        List<Dividend> dividends = new ArrayList<>();

        /*
        for문으로 구현

        for(var entity : dividendEntities) {
            dividends.add(Dividend.builder()
                            .date(entity.getDate())
                            .dividend(entity.getDividend())
                            .build());
        }

        스트림으로 구현하기
        
        dividends = dividendEntities.stream()
                                                    .map(e -> Dividend.builder()
                                                            .date(e.getDate())
                                                            .dividend(e.getDividend())
                                                            .build())
                                                    .collect(Collectors.toList());
         */

        // 캐시적용 후 수정 (builder 사용x)
        dividends = dividendEntities.stream()
                .map(e -> new Dividend(e.getDate(), e.getDividend()))
                .collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()),
                                dividends);
    }
}
