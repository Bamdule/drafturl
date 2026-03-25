package com.drafturl.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.MethodParameter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        this.mockMvc = MockMvcBuilders.standaloneSetup(initController())
                .setMessageConverters(messageConverter)
                .setControllerAdvice(initControllerAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalResolver())
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    protected abstract Object initController();

    protected Object[] initControllerAdvice() {
        return new Object[0];
    }

    /**
     * @AuthenticationPrincipal 파라미터에 주입할 기본 principal 객체를 반환한다.
     * 인증이 필요한 테스트 클래스에서 오버라이드하여 사용한다.
     * null을 반환하면 비인증 요청으로 처리된다.
     */
    protected Object defaultPrincipal() {
        return null;
    }

    /**
     * standaloneSetup에서 @AuthenticationPrincipal을 해석하기 위한 커스텀 리졸버.
     */
    private class AuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return defaultPrincipal();
        }
    }
}
