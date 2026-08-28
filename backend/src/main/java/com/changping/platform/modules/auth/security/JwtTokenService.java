package com.changping.platform.modules.auth.security;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.auth.model.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author lxy
 * @Description //JWT 令牌服务，负责生成访问令牌和解析令牌中的已认证用户信息，签名密钥和有效期从配置文件读取
 * @Date 2026/04/18 09:50
 */
@Service
public class JwtTokenService {

    private final Key signingKey;
    private final Duration accessTokenTtl;

    /**
     * @Author lxy
     * @Description //构造函数，从配置文件读取 JWT 密钥和令牌有效期，初始化 HMAC-SHA 签名密钥
     * @Date 2026/04/18 09:50
     * @Param [jwtSecret JWT 签名密钥字符串（至少32字符）, accessTokenExpireMinutes 访问令牌有效期（分钟）]
     * @return void
     */
    public JwtTokenService(
            @Value("${security.auth.jwt-secret}") String jwtSecret,
            @Value("${security.auth.access-token-expire-minutes}") long accessTokenExpireMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(normalizeSecret(jwtSecret));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenExpireMinutes);
    }

    /**
     * @Author lxy
     * @Description //根据已认证用户信息生成 JWT 访问令牌，令牌中包含用户ID、账号、姓名、客户端类型和密码版本等声明
     * @Date 2026/04/18 09:50
     * @Param [user 已认证用户对象]
     * @return String 签名后的 JWT 访问令牌字符串
     */
    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("userId", user.id())
                .claim("account", user.account())
                .claim("userName", user.userName())
                .claim("clientType", user.clientType())
                .claim("pwv", user.passwordVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * @Author lxy
     * @Description //解析 JWT 令牌并构造已认证用户对象（仅包含基础声明信息，角色和权限码为空列表），令牌无效时抛出业务异常
     * @Date 2026/04/18 09:50
     * @Param [token JWT 令牌字符串]
     * @return AuthenticatedUser 从令牌解析出的已认证用户对象
     */
    public AuthenticatedUser parseAuthenticatedUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Integer pwv = claims.get("pwv", Integer.class);
            return new AuthenticatedUser(
                    claims.get("userId", Long.class),
                    claims.get("account", String.class),
                    claims.get("userName", String.class),
                    claims.get("clientType", String.class),
                    java.util.List.<String>of(),
                    java.util.List.<String>of(),
                    pwv != null ? pwv : 0);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException("AUTH_TOKEN_INVALID", "认证令牌无效");
        }
    }

    /**
     * @Author lxy
     * @Description //对 JWT 密钥字符串进行规范化处理，去除首尾空白并校验长度不少于 32 字符
     * @Date 2026/04/18 09:50
     * @Param [jwtSecret 原始 JWT 密钥字符串]
     * @return byte[] 规范化后的密钥字节数组
     */
    private static byte[] normalizeSecret(String jwtSecret) {
        String normalized = jwtSecret == null ? "" : jwtSecret.trim();
        if (normalized.isEmpty() || normalized.length() < 32) {
            throw new IllegalStateException("security.auth.jwt-secret must be configured with at least 32 characters");
        }
        return normalized.getBytes(StandardCharsets.UTF_8);
    }
}
