package ru.nordcodes.client;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class ApiClient {

    private static final String ENDPOINT = "/endpoint";

    @Step("Отправить запрос в приложение: action = {action}, token = {token}")
    public static Response sendRequest(RequestSpecification spec, String token, String action) {
        return given()
                .spec(spec)
                .formParam("token", token)
                .formParam("action", action)
                .when()
                .post(ENDPOINT)
                .then()
                .extract()
                .response();
    }

    @Step("Войти в систему (LOGIN) с токеном {token}")
    public static Response login(RequestSpecification spec, String token) {
        return sendRequest(spec, token, "LOGIN");
    }

    @Step("Выполнить действие (ACTION) с токеном {token}")
    public static Response action(RequestSpecification spec, String token) {
        return sendRequest(spec, token, "ACTION");
    }

    @Step("Выйти из системы (LOGOUT) с токеном {token}")
    public static Response logout(RequestSpecification spec, String token) {
        return sendRequest(spec, token, "LOGOUT");
    }
}