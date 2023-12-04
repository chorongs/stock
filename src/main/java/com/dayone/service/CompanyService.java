package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRapository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.apache.commons.collections4.Trie;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {
    private final Trie trie;
    private final Scraper yahooFinanceScraper;
    private final CompanyRepository companyRepository;
    private final DividendRapository dividendRapository;


    public Company save(String ticker){
        // 회사의 ticker 존재여부
        boolean exists = this.companyRepository.existsByTicker(ticker);

        if(exists) {
            throw new RuntimeException("already exists ticker -> "+ ticker);
        }
        return this.storeCompanyAndDIvidend(ticker);
    }

    // 모든 회사명 가져오기
    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDIvidend(String ticker) {
        // ticker를 기준으로 회사를 스크래핑
        Company company = this.yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        // 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(company);

        // 스크래핑 결과
        CompanyEntity companyEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                                                                .map(e -> new DividendEntity(companyEntity.getId(), e))
                                                                .collect(Collectors.toList());

        this.dividendRapository.saveAll((dividendEntities));
        return company;
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    // 회사명 조회
    public List<String> autocomplete(String keyword) {
        return (List<String>)this.trie.prefixMap(keyword).keySet()
                .stream().collect(Collectors.toList());

    }

    // 회사명 조회(쿼리)
    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0,10);
        Page<CompanyEntity> companyEntities = this.companyRepository.findByNameStartingWithIgnoreCase(keyword, limit);
        return companyEntities.stream()
                                .map(e -> e.getName())
                                .collect(Collectors.toList());
    }

    // 키워드 삭제
    public void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        var company = this.companyRepository.findByTicker(ticker)
                          .orElseThrow(()-> new NoCompanyException());

        this.dividendRapository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);
        this.deleteAutocompleteKeyword(company.getName());

        return company.getName();
    }
}
