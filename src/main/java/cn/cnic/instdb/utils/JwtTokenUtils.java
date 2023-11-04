package cn.cnic.instdb.utils;

import cn.cnic.instdb.model.rbac.ConsumerDO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Set;


@Component
public class JwtTokenUtils {

    @Value("${jwt.header}")
    private String AUTH_HEADER;

    @Value("${spring.application.name}")
    private String APP_NAME;

    @Value("${jwt.secret}")
    public String SECRET;

    @Value("${jwt.refresh_token_expires}")
    private int TOKEN_EXPIRES;

    @Value("${jwt.token_expires}")
    private int REFRESH_TOKEN_EXPIRES;

    private SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;

    public Token getToken(String tokenStr) {
        Token token;
        try {
            final Claims claims = this.getAllClaimsFromToken(tokenStr);
            token = new Token();
            if(null != claims){
                token.setEmailAccounts(claims.getSubject());
                token.setName((String) claims.get("name"));
                token.setUserId((String) claims.get("userId"));
                token.setRoles((List) claims.get("roles"));
            }
        } catch (Exception ignored) {
            token = null;
        }
        return token;
    }

    public String getUsernameFromToken(String token) {
        String username;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            username = claims.getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    /**
     * Obtain unauthorized paths
     *
     * @param token
     * @return
     */
    public List<String> getUnAuthPath(String token) {
        List<String> unAuthPath;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            unAuthPath = (List<String>) claims.get("unAuthPath");
        } catch (Exception e) {
            unAuthPath = null;
        }
        return unAuthPath;
    }

    /**
     * Obtain user roles
     *
     * @param token
     * @return
     */
    public List<String> getRoles(String token) {
        List<String> roles;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            roles = (List) claims.get("roles");
        } catch (Exception e) {
            roles = null;
        }
        return roles;
    }

    public String getUserIdFromToken(String token) {
        String userId;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            userId = claims.get("userId").toString();
        } catch (Exception e) {
            userId = null;
        }
        return userId;
    }

    public String generateToken(ConsumerDO consumerDO, String type, Set<String> unAuthPath) {
        String audience = generateAudience();
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(consumerDO.getEmailAccounts())
                .setAudience(audience)
                .setIssuedAt(new Date())
                .claim("name", consumerDO.getName())
                .claim("userId", consumerDO.getId())
                .claim("roles", consumerDO.getRoles())
                .claim("unAuthPath", unAuthPath)
                .claim("type", type)
                .setExpiration(generateExpirationDate())
                .signWith(SIGNATURE_ALGORITHM, SECRET)
                .compact();
    }

    public String generateRefreshToken(ConsumerDO consumerDO, String type, Set<String> unAuthPath) {
        String audience = generateAudience();
        return Jwts.builder()
                .setIssuer(APP_NAME)
                .setSubject(consumerDO.getEmailAccounts())
                .setAudience(audience)
                .setIssuedAt(new Date())
                .claim("name", consumerDO.getName())
                .claim("userId", consumerDO.getId())
                .claim("roles", consumerDO.getRoles())
                .claim("unAuthPath", unAuthPath)
                .claim("type", type)
                .setExpiration(generateRefreshExpirationDate())
                .signWith(SIGNATURE_ALGORITHM, SECRET)
                .compact();
    }

    private String generateAudience() {
        return "web";
    }

    private Claims getAllClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    private Date generateExpirationDate() {
        long expiresIn = TOKEN_EXPIRES;
        return new Date(System.currentTimeMillis() + expiresIn * 1000);
    }

    private Date generateRefreshExpirationDate() {
        long expiresIn = REFRESH_TOKEN_EXPIRES;
        return new Date(System.currentTimeMillis() + expiresIn * 1000);
    }

    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = this.getAllClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }


    public Boolean validateToken(String token) {
        final String username = getUsernameFromToken(token);
        final Date expiration = getExpirationDateFromToken(token);
        return (
                username != null && !expiration.before(new Date())
        );
    }

    public String getToken(HttpServletRequest request) {
        return request.getHeader(AUTH_HEADER);
    }

}
