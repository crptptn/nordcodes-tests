package ru.nordcodes.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.security.SecureRandom;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class BaseTest {

    protected static final String BASE_URI = "http://localhost:8080";
    protected static final String API_KEY = "qazWSXedc";
    protected static final int MOCK_PORT = 8888;

    protected static final String AUTH_PATH = "/auth";
    protected static final String DO_ACTION_PATH = "/doAction";

    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    protected static WireMockServer wireMockServer;
    protected static RequestSpecification requestSpec;
    protected static RequestSpecification requestSpecWithoutApiKey;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().port(MOCK_PORT));
        wireMockServer.start();

        requestSpecWithoutApiKey = new RequestSpecBuilder()
                .setBaseUri(BASE_URI)
                .setContentType(ContentType.URLENC)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .build();

        requestSpec = new RequestSpecBuilder()
                .addRequestSpecification(requestSpecWithoutApiKey)
                .addHeader("X-Api-Key", API_KEY)
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetMock() {
        wireMockServer.resetAll();
    }

    protected static String generateValidToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }

    @Step("Внешний сервис авторизации (/auth) отвечает кодом {statusCode}")
    protected static void stubAuth(int statusCode) {
        wireMockServer.stubFor(post(urlEqualTo(AUTH_PATH))
                .willReturn(aResponse().withStatus(statusCode)));
    }

    @Step("Внешний сервис действий (/doAction) отвечает кодом {statusCode}")
    protected static void stubDoAction(int statusCode) {
        wireMockServer.stubFor(post(urlEqualTo(DO_ACTION_PATH))
                .willReturn(aResponse().withStatus(statusCode)));
    }

    @Step("Проверить, что ответ успешный: код 200, result = OK")
    protected static void checkSuccess(Response response) {
        response.then()
                .statusCode(200)
                .body("result", equalTo("OK"));
    }

    @Step("Проверить, что запрос отклонён: код {expectedStatus}, result = ERROR, есть описание причины")
    protected static void checkError(Response response, int expectedStatus) {
        response.then()
                .statusCode(expectedStatus)
                .body("result", equalTo("ERROR"))
                .body("message", not(emptyOrNullString()));
    }

    @Step("Проверить, что внешний сервис получил запрос на {path} с токеном {token}")
    protected static void verifyMockCalled(String path, String token) {
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(path))
                .withRequestBody(containing("token=" + token)));
    }

    @Step("Проверить, что внешний сервис НЕ получал запросов на {path}")
    protected static void verifyMockNotCalled(String path) {
        wireMockServer.verify(0, postRequestedFor(urlEqualTo(path)));
    }
}