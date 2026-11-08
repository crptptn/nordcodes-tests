package ru.nordcodes.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nordcodes.client.ApiClient;

@Epic("Сервис обработки действий пользователя")
@DisplayName("Эндпоинт /endpoint")
public class EndpointTest extends BaseTest {

    // Коды ответов приложения в негативных сценариях
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final int STATUS_INTERNAL_ERROR = 500;

    // ===================== Позитивные сценарии =====================

    @Test
    @Feature("Позитивные сценарии")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Пользователь успешно входит в систему")
    @Description("Внешний сервис подтверждает вход. Приложение должно ответить OK и передать токен во внешний сервис.")
    void loginWithValidTokenReturnsOk() {
        String token = generateValidToken();
        stubAuth(200);

        Response response = ApiClient.login(requestSpec, token);

        checkSuccess(response);
        verifyMockCalled(AUTH_PATH, token);
    }

    @Test
    @Feature("Позитивные сценарии")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Пользователь после входа успешно выполняет действие")
    @Description("После успешного входа пользователь выполняет действие. Приложение должно ответить OK.")
    void actionAfterLoginReturnsOk() {
        String token = generateValidToken();
        stubAuth(200);
        stubDoAction(200);

        checkSuccess(ApiClient.login(requestSpec, token));
        Response response = ApiClient.action(requestSpec, token);

        checkSuccess(response);
        verifyMockCalled(DO_ACTION_PATH, token);
    }

    @Test
    @Feature("Позитивные сценарии")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Пользователь после входа успешно выходит из системы")
    @Description("После успешного входа пользователь завершает сессию. Приложение должно ответить OK.")
    void logoutAfterLoginReturnsOk() {
        String token = generateValidToken();
        stubAuth(200);

        checkSuccess(ApiClient.login(requestSpec, token));
        Response response = ApiClient.logout(requestSpec, token);

        checkSuccess(response);
    }

    // ===================== Неверный токен =====================

    @ParameterizedTest(name = "Вход с некорректным токеном: \"{0}\"")
    @ValueSource(strings = {
            "", // пустой токен
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234", // 31 символ
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456", // 33 символа
            "abcdefghijklmnopqrstuvwxyz012345", // строчные буквы
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234!", // спецсимвол
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234Я" // кириллица
    })
    @Feature("Неверный токен")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Токен не соответствует формату (32 символа A-Z0-9). Приложение должно отклонить запрос и не обращаться к внешнему сервису.")
    void loginWithInvalidTokenReturnsError(String invalidToken) {
        stubAuth(200);

        Response response = ApiClient.login(requestSpec, invalidToken);

        checkError(response, STATUS_BAD_REQUEST);
        verifyMockNotCalled(AUTH_PATH);
    }

    // ===================== Падение внешнего сервиса =====================

    @Test
    @Feature("Сбой внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход невозможен, если внешний сервис авторизации недоступен (код 500)")
    @Description("Внешний сервис авторизации отвечает ошибкой 500. Приложение должно сообщить об ошибке.")
    void loginWhenAuthServiceFailsReturnsError() {
        String token = generateValidToken();
        stubAuth(500);

        Response response = ApiClient.login(requestSpec, token);

        checkError(response, STATUS_INTERNAL_ERROR);
        verifyMockCalled(AUTH_PATH, token);
    }

    @Test
    @Feature("Сбой внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие невозможно, если внешний сервис действий недоступен (код 500)")
    @Description("Пользователь успешно вошёл, но внешний сервис действий отвечает ошибкой 500. Приложение должно сообщить об ошибке.")
    void actionWhenDoActionServiceFailsReturnsError() {
        String token = generateValidToken();
        stubAuth(200);
        stubDoAction(500);

        checkSuccess(ApiClient.login(requestSpec, token));
        Response response = ApiClient.action(requestSpec, token);

        checkError(response, STATUS_INTERNAL_ERROR);
        verifyMockCalled(DO_ACTION_PATH, token);
    }

    // ===================== Отсутствие обязательного заголовка =====================

    @Test
    @Feature("Проверка API-ключа")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Запрос без API-ключа отклоняется")
    @Description("В запросе нет заголовка X-Api-Key. Приложение должно отклонить запрос и не обращаться к внешнему сервису.")
    void requestWithoutApiKeyReturnsError() {
        String token = generateValidToken();
        stubAuth(200);

        Response response = ApiClient.login(requestSpecWithoutApiKey, token);

        checkError(response, STATUS_UNAUTHORIZED);
        verifyMockNotCalled(AUTH_PATH);
    }

    @Test
    @Feature("Проверка API-ключа")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Запрос с неверным API-ключом отклоняется")
    @Description("В заголовке X-Api-Key передан неверный ключ. Приложение должно отклонить запрос и не обращаться к внешнему сервису.")
    void requestWithWrongApiKeyReturnsError() {
        String token = generateValidToken();
        stubAuth(200);

        RequestSpecification wrongKeySpec = new RequestSpecBuilder()
                .addRequestSpecification(requestSpecWithoutApiKey)
                .addHeader("X-Api-Key", "wrongKey123")
                .build();

        Response response = ApiClient.login(wrongKeySpec, token);

        checkError(response, STATUS_UNAUTHORIZED);
        verifyMockNotCalled(AUTH_PATH);
    }
}