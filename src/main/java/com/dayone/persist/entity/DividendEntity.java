package com.dayone.persist.entity;

import com.dayone.model.Dividend;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "DIVIDEND")
@Getter
@Setter
@ToString
@NoArgsConstructor
// 유니크 키 설정
// 일종의 인덱스, 제약조건 (중복데이터 저장을 방지하는 제약조건)
// 단일컴럼 뿐만 아니라, 복합 컬럼을 지정할 수도 있음
@Table (
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"companyId","date"}
                )
        }
)
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long companyId;

    private LocalDateTime date;

    private String dividend;


    public DividendEntity(Long companyId, Dividend dividend) {
        this.companyId = companyId;
        this.date = dividend.getDate();
        this.dividend = dividend.getDividend();
    }
}
