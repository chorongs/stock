package com.dayone.security;

import com.dayone.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {
    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1시간을 의미
    private static final String KEY_ROLES = "roles";
    private final MemberService memberService;

    @Value("{spring.jwt.secret}")
    private String secretKey;

    /*
    토큰 생성(발급)
     */
    public String generateToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        // 1시간동안 토큰이 유효함을 의미
        var now = new Date();
        var expireDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setClaims(claims) // 사용자정보
                .setIssuedAt(now)  // 토큰 생성 시간
                .setExpiration(expireDate) // 토큰 만료시간
                .signWith(SignatureAlgorithm.ES512, this.secretKey) // 사용할 암호화 알고리즘, 비밀키
                .compact();

    }

    public UsernamePasswordAuthenticationToken getAuthentication(String jwt) {
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));
        // 스프링에서 지원하는 형태의 토큰으로 변환
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }


    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) return false;

        var claims = this.parseClaims(token);
        return !claims.getExpiration().before(new Date());
    }

    /*
    토큰 유효한지?
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody();

        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
