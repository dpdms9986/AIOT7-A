package com.aloha.project.security.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

  // private RequestCache requestCache = new HttpSessionRequestCache();

  @Override
   public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
      log.info("onAuthenticationFailure");
    	
    	log.info("onAuthenticationFailure exception " + exception);
    	
      String targetUrl = "/login?error";
    	// SavedRequest savedRequest = requestCache.getRequest(request, response);
      // if( savedRequest != null )
      //   targetUrl = savedRequest.getRedirectUrl();

      log.info("Login Failure targetUrl = " + targetUrl);

      response.sendRedirect(targetUrl);
    }
  
}
