package com.project.semipermbackend.auth.jwt;

import com.project.semipermbackend.auth.dto.AuthResponseDto;
import com.project.semipermbackend.auth.exception.TokenInvalidException;
import com.project.semipermbackend.common.error.ErrorCode;
import com.project.semipermbackend.domain.account.Account;
import com.project.semipermbackend.domain.member.Member;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String TOKEN_PREFIX = "Bearer ";

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;


    private final SecretKey secretKey;

    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;
    private final JwtParser jwtParser;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretBaseKey,
            @Value("${jwt.access-token-validity-sec}") long accessTokenValidityInSec,
            @Value("${jwt.refresh-token-validity-sec}") long refreshTokenValidityInSec) {
        this.secretKey = createKey(secretBaseKey);
        this.accessTokenValidityInMillis = accessTokenValidityInSec * 60000;
        this.refreshTokenValidityInMillis = refreshTokenValidityInSec * 60000;
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.secretKey).build();
    }

    private SecretKey createKey(String secretBaseKey) {
        byte[] baseKeyBytes = DatatypeConverter.parseBase64Binary(secretBaseKey);
        return new SecretKeySpec(baseKeyBytes, signatureAlgorithm.getJcaName());
    }

    public String createAccessToken(Member member, Account account) {
        return createToken(member, account, ACCESS_TOKEN);
    }

    public String createRefreshToken(Member member, Account account) {
        return createToken(member, account, REFRESH_TOKEN);
    }

    private String createToken(Member member, Account account, String tokenType) {
        long expiredTimeInMillis = ACCESS_TOKEN.equals(tokenType) ? accessTokenValidityInMillis
                : refreshTokenValidityInMillis;
        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "JWT");
        headers.put("alg", signatureAlgorithm.getValue());

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", member.getMemberId());
        claims.put("accountId", account.getAccountId());

        Date expireTime = new Date();
        expireTime.setTime(expireTime.getTime() + expiredTimeInMillis);

        String jwt = Jwts.builder()
                .setHeader(headers)
                .setClaims(claims)
                .setExpiration(expireTime)
                .signWith(secretKey, signatureAlgorithm)
                .compact();
        return jwt;
    }

    // parseClaimsJwt() : 서명되지 않은 일반 텍스트 JWT 인스턴스를 반환
    // parseClaimsJws() : 결과 Claims JWS 인스턴스를 반환
    public void validateToken(String jwt) {
        try{
            getClaims(jwt);
        } catch (ExpiredJwtException e) {
            log.trace("JWT token is expired : ", e);
            throw new TokenInvalidException(ErrorCode.TOKEN_EXPIRED_ERROR);

        } catch (UnsupportedJwtException e) {
            log.trace("Unsupported JWT token : ", e);
            throw new TokenInvalidException(ErrorCode.UNSUPPORTED_TOKEN_ERROR);

        } catch (JwtException e) {
            log.trace("Jwt Exception : ", e);
            throw new TokenInvalidException(ErrorCode.JWT_ERROR);
        }
    }

    public Claims getClaims(String jwt) {
        return jwtParser.parseClaimsJws(jwt)
                .getBody();
    }

    public String getMemberIdFromToken(String jwt) {
        return getClaims(jwt).get("memberId").toString();
    }
    public String getAccountIdFromToken(String jwt) {
        return getClaims(jwt).get("accountId").toString();
    }
    /**
     * principal : memberId
     * credential : accountId
     * authorities : null
     * @param jwt
     */
    public Authentication getAuthentication(String jwt) {

        UserDetails userDetails = new User(getMemberIdFromToken(jwt), "", AuthorityUtils.NO_AUTHORITIES);
        return new UsernamePasswordAuthenticationToken(userDetails, getAccountIdFromToken(jwt), userDetails.getAuthorities());
    }

    public static Long getMemberIdFromContext() {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return Long.valueOf(principal.getUsername());
    }
    public static Long getAccountIdFromContext() {
        String accountIdOfCredentials = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        return Long.valueOf(accountIdOfCredentials);
    }

    public AuthResponseDto.AuthTokens reissueTokens(Member member, Account account) {
        String accessToken = createAccessToken(member, account);
        String refreshToken = createRefreshToken(member, account);
        AuthResponseDto.AuthTokens responseDto = new AuthResponseDto.AuthTokens(accessToken, refreshToken);

        account.setLoginStatus(refreshToken);
        return responseDto;
    }
}
