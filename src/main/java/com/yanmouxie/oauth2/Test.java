package com.yanmouxie.oauth2;

import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.context.ContextLoaderListener;

public class Test {

	private AuthorizationServerTokenServices tokenServices;
	
	private ClientDetailsService clientDetails;
	
	private AccessTokenConverter accessTokenConverter = new CheckTokenAccessTokenConverter();
	
	public String requestAccessToken(String clientId,String clientSecret)
	{
		/*
		System.out.println("=================================================== Beans:=====================================");
		String []strs= ContextLoaderListener.getCurrentWebApplicationContext().getBeanDefinitionNames();  
		for(int i=0;i<strs.length;i++){  
			System.out.println(strs[i]);  
		}*/ 
		
		String response = "";
		
		ClientDetails clientDetail = null;
		
		try {
			tokenServices = (AuthorizationServerTokenServices)ContextLoaderListener.getCurrentWebApplicationContext().getBean ("tokenServices");
			clientDetails = (ClientDetailsService)ContextLoaderListener.getCurrentWebApplicationContext().getBean ("clientDetails");
			clientDetail = clientDetails.loadClientByClientId(clientId);
			
		} catch (Exception e) {
			return "Failed";
		}

		if ( clientDetail != null && !StringUtils.equals(clientDetail.getClientSecret(), clientSecret)) {
			return "Invalid Client.";
		}
		
		// Use Oauth2 grant_type: client_credentials
		TokenRequest tokenRequest = new TokenRequest(MapUtils.EMPTY_MAP, clientId, clientDetail.getScope(),
				"client_credentials");

		OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetail);

		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, null);

		OAuth2AccessToken token = tokenServices.createAccessToken(oAuth2Authentication);
		
		if(token != null && StringUtils.isNotEmpty(token.getValue()))
		{
			response = token.getValue();
		}else
		{
			return "Access Token service error.";
		}
		
		return response;
	}
	
	public String check(String accessToken)
	{
		ResourceServerTokenServices resourceServerTokenServices = (ResourceServerTokenServices)ContextLoaderListener.getCurrentWebApplicationContext().getBean ("tokenServices");
		
		OAuth2AccessToken token = resourceServerTokenServices.readAccessToken(accessToken);
		
		if (token == null) {
			//throw new InvalidTokenException("Token was not recognised");
			return "Token was not recognised";
		}

		if (token.isExpired()) {
			//throw new InvalidTokenException("Token has expired");
			return "Token has expired";
		}

		OAuth2Authentication authentication = resourceServerTokenServices.loadAuthentication(token.getValue());

		return accessTokenConverter.convertAccessToken(token, authentication).toString();
	}
	
	
	static class CheckTokenAccessTokenConverter implements AccessTokenConverter {
		private final AccessTokenConverter accessTokenConverter;

		CheckTokenAccessTokenConverter() {
			this(new DefaultAccessTokenConverter());
		}

		CheckTokenAccessTokenConverter(AccessTokenConverter accessTokenConverter) {
			this.accessTokenConverter = accessTokenConverter;
		}

		public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
			Map<String, Object> claims = (Map<String, Object>) this.accessTokenConverter.convertAccessToken(token, authentication);
			claims.put("active", true); // Always true if token exists and not expired
			return claims;
		}

		public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
			return this.accessTokenConverter.extractAccessToken(value, map);
		}

		public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
			return this.accessTokenConverter.extractAuthentication(map);
		}
	}
}
