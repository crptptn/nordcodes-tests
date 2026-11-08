# Автотесты для эндпоинта /endpoint

Стек: Java 17, Maven, JUnit 5, REST Assured, WireMock, Allure.

## Запуск

Перед запуском тестов приложение должно быть поднято на `http://localhost:8080`.

Запуск тестов:
```bash
mvn clean test
```

Просмотр Allure-отчета:
```bash
mvn allure:serve
```
(или `mvn allure:report` для генерации статики в `target/site/allure-maven-plugin`).