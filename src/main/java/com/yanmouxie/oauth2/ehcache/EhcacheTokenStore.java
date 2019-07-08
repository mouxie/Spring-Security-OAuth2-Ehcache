package com.yanmouxie.oauth2.ehcache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.context.ContextLoaderListener;

public class EhcacheTokenStore implements TokenStore {

	private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();
	
	public Cache getCache(String cacheName) {
		CacheManager cacheManager = (CacheManager)ContextLoaderListener.getCurrentWebApplicationContext().getBean ("oauthCacheManager");
		if( cacheManager != null )
		{
			return cacheManager.getCache(cacheName);
		}
		return null;
	}
	
	@Override
	public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
		return readAuthentication(token.getValue());
	}

	@Override
	public OAuth2Authentication readAuthentication(String token) {
        Cache cache = getCache("authenticationStore");
        Element element = cache.get(token);
        if (element != null) {
        	return (OAuth2Authentication)element.getObjectValue();
        }
		return null;
	}

	@Override
	public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		
		Cache cache = getCache("accessTokenStore");
		Element e = new Element(token.getValue(), token);
        if (token.getExpiration() != null) {
        	e.setTimeToLive(token.getExpiresIn());
        }
		cache.put(e);
        
		cache = getCache("authenticationStore");
		e = new Element(token.getValue(), authentication);
		if (token.getExpiration() != null) {
        	e.setTimeToLive(token.getExpiresIn());
        }
		cache.put(e);
		
		cache = getCache("authenticationToAccessTokenStore");
		e = new Element(authenticationKeyGenerator.extractKey(authentication), token);
		if (token.getExpiration() != null) {
        	e.setTimeToLive(token.getExpiresIn());
        }
		cache.put(e);
		
		if (!authentication.isClientOnly()) {
			cache = getCache("userNameToAccessTokenStore");
			String key = getApprovalKey(authentication);
			Element element = cache.get(key);
	        if (element == null) {
	        	HashSet<OAuth2AccessToken> tokenSet = new HashSet<OAuth2AccessToken>();
	        	tokenSet.add(token);
	        	cache.put(new Element(key, tokenSet));
	        }else
	        {
	        	HashSet<OAuth2AccessToken> tokenSet = (HashSet<OAuth2AccessToken>)element.getObjectValue();
	        	tokenSet.add(token);
	        	cache.put(new Element(key, tokenSet));
	        }
		}
		
		cache = getCache("clientIdToAccessTokenStore");
		String key = authentication.getOAuth2Request().getClientId();
		Element element = cache.get(key);
        if (element == null) {
        	HashSet<OAuth2AccessToken> tokenSet = new HashSet<OAuth2AccessToken>();
        	tokenSet.add(token);
        	cache.put(new Element(key, tokenSet));
        }else
        {
        	//clear expired token in clientIdToAccessTokenStore
    		Collection<OAuth2AccessToken> result = null;
    		List<String> list = cache.getKeys();
    		if(list!=null)
    		{
    	        for (String s : list) {
    	        	if(cache.get(s)!=null) {
    	        		element = cache.get(s);
    	                if (element != null) {
    	                	result = (Collection<OAuth2AccessToken>)element.getObjectValue();
    	                    for (OAuth2AccessToken accessToken : result) {
    	                    	if(accessToken.isExpired())
    	                    	{
    	                    		result.remove(accessToken);
    	                    		cache.put(new Element(s,result));
    	                    	}
    	                    }
    	                }
    	        	}
    	        }
    		}
        	HashSet<OAuth2AccessToken> tokenSet = (HashSet<OAuth2AccessToken>)element.getObjectValue();
        	tokenSet.add(token);
        	cache.put(new Element(key, tokenSet));
        }
		
		if (token.getRefreshToken() != null && token.getRefreshToken().getValue() != null) {
			cache = getCache("refreshTokenToAccessTokenStore");
			e = new Element(token.getRefreshToken().getValue(), token.getValue());
			if (token.getExpiration() != null) {
	        	e.setTimeToLive(token.getExpiresIn());
	        }
			cache.put(e);
			
			cache = getCache("accessTokenToRefreshTokenStore");
			e = new Element(token.getValue(), token.getRefreshToken().getValue());
			if (token.getExpiration() != null) {
	        	e.setTimeToLive(token.getExpiresIn());
	        }
			cache.put(e);
		}
	}

	@Override
	public OAuth2AccessToken readAccessToken(String tokenValue) {
        Cache cache = getCache("accessTokenStore");
        Element element = cache.get(tokenValue);
        if (element != null) {
        	return (OAuth2AccessToken)element.getObjectValue();
        }
        return null;
	}

	@Override
	public void removeAccessToken(OAuth2AccessToken token) {
		removeAccessToken(token.getValue());
	}
	
	public void removeAccessToken(String tokenValue) {
        OAuth2AccessToken removed = null;
        Cache cache = getCache("accessTokenStore");
        Element element = cache.get(tokenValue);
        if (element != null) {
        	removed = (OAuth2AccessToken)element.getObjectValue();
        }
        cache.remove(tokenValue);
        
		cache = getCache("accessTokenToRefreshTokenStore");
        cache.remove(tokenValue);
        
		// Don't remove the refresh token - it's up to the caller to do that
		
		cache = getCache("authenticationStore");
		element = cache.get(tokenValue);
		if (element != null) {
			OAuth2Authentication authentication = (OAuth2Authentication)element.getObjectValue();
			
			if (authentication != null) {
				cache = getCache("authenticationToAccessTokenStore");
				cache.remove(authenticationKeyGenerator.extractKey(authentication));
				
				Collection<OAuth2AccessToken> tokens = null;
				String clientId = authentication.getOAuth2Request().getClientId();
				cache = getCache("userNameToAccessTokenStore");
				element = cache.get(getApprovalKey(clientId, authentication.getName()));
				if (element != null) {
					tokens = (Collection<OAuth2AccessToken>)element.getObjectValue();
				}
				
				if (tokens != null) {
					tokens.remove(removed);
					cache.put(new Element(getApprovalKey(clientId, authentication.getName()),tokens));
				}
				
				cache = getCache("clientIdToAccessTokenStore");
				element = cache.get(clientId);
				if (element != null) {
					tokens = (Collection<OAuth2AccessToken>)element.getObjectValue();
				}
				if (tokens != null) {
					tokens.remove(removed);
					cache.put(new Element(clientId,tokens));
				}
				cache = getCache("authenticationToAccessTokenStore");
				cache.remove(authenticationKeyGenerator.extractKey(authentication));
			}
		}
		cache.remove(tokenValue);
		
	}

	@Override
	public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
		Cache cache = getCache("refreshTokenStore");
		Element e = new Element(refreshToken.getValue(), refreshToken);
		cache.put(e);
		
		cache = getCache("refreshTokenAuthenticationStore");
		e = new Element(refreshToken.getValue(), authentication);
		cache.put(e);
	}

	@Override
	public OAuth2RefreshToken readRefreshToken(String tokenValue) {
		Cache cache = getCache("refreshTokenStore");
        Element element = cache.get(tokenValue);
        if (element != null) {
        	return (OAuth2RefreshToken)element.getObjectValue();
        }
        return null;
	}

	@Override
	public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
		return readAuthenticationForRefreshToken(token.getValue());
	}

	public OAuth2Authentication readAuthenticationForRefreshToken(String token) {
		Cache cache = getCache("refreshTokenAuthenticationStore");
        Element element = cache.get(token);
        if (element != null) {
        	return (OAuth2Authentication)element.getObjectValue();
        }
        return null;
	}

	@Override
	public void removeRefreshToken(OAuth2RefreshToken token) {
		removeRefreshToken(token.getValue());
	}

	public void removeRefreshToken(String tokenValue) {
		Cache cache = getCache("refreshTokenStore");
		cache.remove(tokenValue);
		cache = getCache("refreshTokenAuthenticationStore");
		cache.remove(tokenValue);
		cache = getCache("refreshTokenToAccessTokenStore");
		cache.remove(tokenValue);
	}

	@Override
	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		removeAccessTokenUsingRefreshToken(refreshToken.getValue());
	}

	private void removeAccessTokenUsingRefreshToken(String refreshToken) {
		String accessToken = null;
		Cache cache = getCache("refreshTokenToAccessTokenStore");
		Element element = cache.get(refreshToken);
        if (element != null) {
        	accessToken =  (String)element.getObjectValue();
        }
		if (accessToken != null) {
			removeAccessToken(accessToken);
		}
		cache.remove(refreshToken);
	}

	@Override
	public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
		String key = authenticationKeyGenerator.extractKey(authentication);
		OAuth2AccessToken accessToken = null;
		Cache cache = getCache("authenticationToAccessTokenStore");
		Element element = cache.get(key);
        if (element != null) {
        	accessToken = (OAuth2AccessToken)element.getObjectValue();
        }
		if (accessToken != null
				&& !key.equals(authenticationKeyGenerator.extractKey(readAuthentication(accessToken.getValue())))) {
			// Keep the stores consistent (maybe the same user is represented by this authentication but the details
			// have changed)
			storeAccessToken(accessToken, authentication);
		}
		return accessToken;
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
		Collection<OAuth2AccessToken> result = null;
		Cache cache = getCache("userNameToAccessTokenStore");
		Element element = cache.get(getApprovalKey(clientId, userName));
        if (element != null) {
        	result = (Collection<OAuth2AccessToken>)element.getObjectValue();
        	//clear expired token
            for (OAuth2AccessToken accessToken : result) {
            	if(accessToken.isExpired())
            	{
            		//removeAccessToken(accessToken);
            		result.remove(accessToken);
            		cache.put(new Element(getApprovalKey(clientId, userName),result));
            	}
            }
        }
		
		return result != null ? Collections.<OAuth2AccessToken> unmodifiableCollection(result) : Collections
				.<OAuth2AccessToken> emptySet();
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
		Collection<OAuth2AccessToken> result = null;
		Cache cache = getCache("clientIdToAccessTokenStore");
		Element element = cache.get(clientId);
        if (element != null) {
        	result = (Collection<OAuth2AccessToken>)element.getObjectValue();
            //clear expired token
            for (OAuth2AccessToken accessToken : result) {
            	if(accessToken.isExpired())
            	{
            		//removeAccessToken(accessToken);
            		result.remove(accessToken);
            		cache.put(new Element(clientId,result));
            	}
            }
        }
		return result != null ? Collections.<OAuth2AccessToken> unmodifiableCollection(result) : Collections
				.<OAuth2AccessToken> emptySet();
	}
	
	private String getApprovalKey(OAuth2Authentication authentication) {
		String userName = authentication.getUserAuthentication() == null ? "" : authentication.getUserAuthentication()
				.getName();
		return getApprovalKey(authentication.getOAuth2Request().getClientId(), userName);
	}

	private String getApprovalKey(String clientId, String userName) {
		return clientId + (userName==null ? "" : ":" + userName);
	}
	
	public void output() {
		
		System.out.println("===============================  EhCache ===============================");
		CacheManager cacheManager = (CacheManager)ContextLoaderListener.getCurrentWebApplicationContext().getBean ("oauthCacheManager");
		String names[] = cacheManager.getCacheNames();
        for (String name : names) {
            System.out.println(name);
        }
        Cache cache = cacheManager.getCache("accessTokenStore");
        //cache.put(new Element(String.valueOf(System.currentTimeMillis()), String.valueOf(System.currentTimeMillis())));
        System.out.println("accessTokenStore cache siez:" + cache.getSize());
        List<String> list = cache.getKeys();
        for (String s : list) {
        	if(cache.get(s)!=null) {
        		System.out.println(cache.get(s).getObjectValue().toString());
        	}
        }
        
	}
}
