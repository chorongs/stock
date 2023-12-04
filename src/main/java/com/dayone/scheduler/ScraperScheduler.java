package com.dayone.scheduler;

import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRapository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@EnableCaching
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final Scraper yahooFinanceScraper;
    private final DividendRapository dividendRapository;

    // 일정 주기마다 실행
    //@Scheduled(cron = "0 0 0 * * *") // 매 정각
    // 배당금 데이터를 조회할때마다, 캐시 데이터를 관리해줌
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true) // allEntries = true : value에 해당하느 값을 모두 지움
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");
        // 저장된 회사 목록을 조회
        List<CompanyEntity> companies = this.companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크래핑

        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(new Company(company.getTicker(), company.getName()));

            // 스크래핑한 배당금 정보중 데이터베이스에 없는 값 저장
            // 한번에 저장할 경우 유니크 키로 지정해놓은 부분 때문에 에러가 발생하면 전체가 입력 안될수 있다.
            // 때문에 saveAll는 사용하지 않음
            // this.dividendRapository.saveAll()
            scrapedResult.getDividends().stream()
                    // Dividen모델을 DividendEntity 로 맵핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    // 엘리먼트를 하나씩 dividendRapository에 삽입
                    .forEach(e-> {
                        boolean exists = this.dividendRapository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRapository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            // 연속적으로 스크래핑 하지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3초
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
