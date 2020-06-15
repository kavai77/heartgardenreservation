package com.himadri.heartgardenreservation;

import com.himadri.heartgardenreservation.annotations.AdminAuthorization;
import com.himadri.heartgardenreservation.annotations.FirebaseTokenParam;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AdminControllerTest {
    @Test
    public void everyMethodIsAnnotatedByAdminAuthorization() {
        for (Method method: AdminController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class) &&
                !method.getAnnotation(RequestMapping.class).value()[0].equals("/")) {
                assertTrue(method.isAnnotationPresent(AdminAuthorization.class),
                    () -> String.format("Method %s is not annotated with %s", method.getName(), AdminAuthorization.class.getSimpleName()));
            }
        }
    }

    @Test
    public void everyAdminAuthorizationHasFirebaseTokenParam() {
        methodLoop:
        for (Method method: AdminController.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AdminAuthorization.class)) {
                for (Parameter parameter: method.getParameters()) {
                    if (parameter.getAnnotation(FirebaseTokenParam.class) != null) {
                        continue methodLoop;
                    }
                }
                fail(() -> String.format("Method %s has no parameter annotated with %s", method.getName(), FirebaseTokenParam.class.getSimpleName()));
            }
        }
    }

}