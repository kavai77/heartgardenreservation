package com.himadri.heartgardenreservation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.himadri.heartgardenreservation.annotations.FirebaseTokenParam;
import com.himadri.heartgardenreservation.entity.AdminAccess;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Aspect
@Component
public class AdminAuthorizationAspect {
    @Value("${tokenIssuer}")
    private String tokenIssuer;

    @Value("${accountsWhitelistedForAdmin}")
    private List<String> accountsWhitelistedForAdmin;

    @Around("@annotation(com.himadri.heartgardenreservation.annotations.AdminAuthorization)")
    public Object adminAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {
        String token = findFirebaseToken(joinPoint);
        checkAuthorization(token);
        return joinPoint.proceed();
    }

    private String findFirebaseToken(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameters()[i].getAnnotation(FirebaseTokenParam.class) != null) {
                return (String) joinPoint.getArgs()[i];
            }
        }
        throw new IllegalStateException("No FirebaseTokenParam in the annotated class");
    }

    private void checkAuthorization(String firebaseToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
        if (!StringUtils.equals(decodedToken.getIssuer(), tokenIssuer) ||
            !decodedToken.isEmailVerified() ||
            !accountsWhitelistedForAdmin.contains(decodedToken.getEmail())) {
            ofy().save().entity(new AdminAccess(
                UUID.randomUUID().toString(),
                decodedToken.getName(),
                decodedToken.getEmail(),
                decodedToken.getUid(),
                decodedToken.getIssuer(),
                decodedToken.isEmailVerified(),
                new Date()
            ));
            throw new AdminAuthorizationException();
        }
    }

    @ResponseStatus(value= HttpStatus.UNAUTHORIZED)
    public static class AdminAuthorizationException extends RuntimeException{}
}
