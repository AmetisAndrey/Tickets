# Tickets

Backend-приложение для управления IT-заявками (tickets). Проект
реализован на Kotlin и Java с использованием Spring Boot, PostgreSQL и
Flyway. Приложение предоставляет REST API для создания и сопровождения
заявок, управления их статусами, просмотра истории изменений и получения
сводной статистики.

## Возможности

-   Создание и просмотр IT-заявок.
-   Обновление описания, приоритета и срока выполнения.
-   Получение списка заявок с фильтрацией и пагинацией.
-   Управление категориями заявок.
-   Изменение статуса с проверкой допустимых переходов.
-   Сохранение истории изменения статусов.
-   Получение сводного отчёта за выбранный период.
-   Документация API через Swagger UI.
-   Миграции схемы и демонстрационные данные через Flyway.
-   Контейнеризация приложения и PostgreSQL с помощью Docker Compose.
-   CI/CD на GitHub Actions: тестирование, сборка JAR и публикация
    Docker-образа в Docker Hub.

## Технологический стек

  -----------------------------------------------------------------------
  Технология                          Назначение
  ----------------------------------- -----------------------------------
  Java 17                             JVM и Java-компоненты приложения

  Kotlin 1.9.22                       Kotlin-компоненты приложения

  Spring Boot 3.2.5                   Основа backend-приложения

  Spring Web                          REST API

  Spring Data JPA / Hibernate         Работа с базой данных

  PostgreSQL 16                       Реляционная база данных

  Flyway                              Версионирование схемы БД и миграции

  Gradle 8.5                          Сборка проекта и запуск тестов

  Springdoc OpenAPI                   Swagger UI и OpenAPI-документация

  JUnit 5, Mockito, MockK,            Автоматизированное тестирование
  Testcontainers                      

  JaCoCo                              Формирование отчёта о покрытии
                                      тестами

  Docker, Docker Compose              Контейнеризация и локальный запуск

  GitHub Actions                      CI/CD

  Docker Hub                          Хранение Docker-образов
  -----------------------------------------------------------------------

## Архитектура и структура проекта

Основной код находится в `src/main`.

``` text
src/
├── main/
│   ├── java/com/example/tickets/
│   │   ├── config/       # Конфигурация приложения и Clock
│   │   ├── domain/       # Сущности, статусы и приоритеты
│   │   ├── repository/   # Spring Data JPA репозитории
│   │   ├── service/      # Бизнес-логика и машина состояний
│   │   └── web/          # REST-контроллеры, DTO и обработка ошибок
│   ├── kotlin/com/example/tickets/report/
│   │                       # Сервис формирования отчётов
│   └── resources/
│       ├── db/migration/ # SQL-миграции Flyway
│       └── application.yml
└── test/
    ├── java/              # Unit, integration и E2E тесты
    └── kotlin/            # Тесты Kotlin-компонентов

.github/workflows/ci-cd.yml # GitHub Actions pipeline
Dockerfile
compose.yaml
build.gradle.kts
```

## Предметная область

### Статусы заявок

Используются следующие статусы:

-   `NEW` --- новая заявка.
-   `IN_PROGRESS` --- заявка в работе.
-   `RESOLVED` --- заявка решена.
-   `CANCELLED` --- заявка отменена.

Допустимые переходы статусов:

``` text
NEW ───────────> IN_PROGRESS ───────────> RESOLVED
  └────────────> CANCELLED                 (конечный)
                    (конечный)
```

Из `RESOLVED` и `CANCELLED` дальнейшие переходы не разрешены.
Допустимость переходов проверяет `TicketStatusMachine`. Изменения
статусов записываются в историю.

### Приоритеты

-   `LOW`
-   `MEDIUM`
-   `HIGH`

### Категории

В начальной миграции создаются категории `Hardware`, `Software`,
`Network` и `Access`.

## REST API

Базовый адрес при локальном запуске: `http://localhost:8080`.

  Метод     Endpoint                      Назначение
  --------- ----------------------------- ----------------------------------
  `POST`    `/api/tickets`                Создать заявку
  `GET`     `/api/tickets`                Получить список заявок
  `GET`     `/api/tickets/{id}`           Получить заявку по ID
  `PATCH`   `/api/tickets/{id}`           Обновить заявку
  `PATCH`   `/api/tickets/{id}/status`    Изменить статус заявки
  `GET`     `/api/tickets/{id}/history`   Получить историю статусов
  `GET`     `/api/categories`             Получить список категорий
  `GET`     `/api/reports/summary`        Получить сводный отчёт за период
  `GET`     `/actuator/health`            Проверить состояние приложения

### Фильтрация списка заявок

Для `GET /api/tickets` поддерживаются параметры:

-   `status` --- статус заявки;
-   `priority` --- приоритет;
-   `categoryId` --- ID категории;
-   `overdue` --- признак просроченной заявки;
-   `page` --- номер страницы, по умолчанию `0`;
-   `size` --- размер страницы, по умолчанию `20`.

Пример:

``` http
GET /api/tickets?status=IN_PROGRESS&priority=HIGH&page=0&size=10
```

### Пример создания заявки

``` http
POST /api/tickets
Content-Type: application/json
```

``` json
{
  "title": "Не работает VPN",
  "description": "Не удаётся подключиться к корпоративной сети",
  "categoryId": 3,
  "priority": "HIGH",
  "dueAt": "2026-12-01T12:00:00Z"
}
```

Для создания требуются `title`, `categoryId`, `priority` и `dueAt`. Поле
`description` необязательно.

### Пример изменения статуса

``` http
PATCH /api/tickets/1/status
Content-Type: application/json
```

``` json
{
  "status": "IN_PROGRESS"
}
```

### Сводный отчёт

`GET /api/reports/summary` принимает обязательные параметры `from` и
`to` в формате ISO-8601 даты-времени.

Пример:

``` http
GET /api/reports/summary?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z
```

Отчёт содержит количество созданных заявок, распределение по статусам и
приоритетам, количество открытых просроченных заявок и среднее время
решения (если в периоде есть решённые заявки).

## Запуск локально

### Требования

-   JDK 17.
-   Docker Desktop с Docker Compose.
-   Git --- для клонирования репозитория.

### Вариант 1. Запуск через Docker Compose

Клонируйте репозиторий:

``` bash
git clone https://github.com/AmetisAndrey/Tickets.git
cd Tickets
```

Создайте в корне проекта файл `.env`:

``` dotenv
DB_USER=postgres
DB_PASSWORD=postgres
```

> Используйте одинаковые значения `DB_USER` и `DB_PASSWORD` для сервиса
> PostgreSQL и приложения. Не добавляйте файл `.env` с реальными
> секретами в Git.

Соберите и запустите сервисы:

``` bash
docker compose up -d --build
```

Проверьте состояние контейнеров:

``` bash
docker compose ps
```

Оба сервиса должны перейти в состояние `healthy` (для приложения после
завершения запуска).

Проверка health endpoint:

``` bash
curl http://localhost:8080/actuator/health
```

Ожидаемый ответ:

``` json
{
  "status": "UP"
}
```

Остановка:

``` bash
docker compose down
```

Остановка с удалением тома базы данных (удалит сохранённые данные):

``` bash
docker compose down -v
```

Приложение будет доступно на `http://localhost:8080`.

### Вариант 2. Запуск через Gradle

Для запуска вне Docker необходим доступный PostgreSQL 16 и созданная
база данных `tickets`.

По умолчанию приложение использует:

``` text
URL:      jdbc:postgresql://localhost:5432/tickets
Username: postgres
Password: 12345
```

Эти значения можно переопределить переменными окружения:

``` powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/tickets"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "your_password"
```

Запуск приложения:

``` bash
./gradlew bootRun
```

В Windows PowerShell:

``` powershell
.\gradlew.bat bootRun
```

Flyway автоматически применяет миграции при старте приложения.

## Тестирование

Запуск полного набора тестов:

``` bash
./gradlew clean test
```

Windows PowerShell:

``` powershell
.\gradlew.bat clean test
```

Сборка исполняемого Spring Boot JAR:

``` bash
./gradlew bootJar
```

Полная CI-команда, используемая в pipeline:

``` bash
./gradlew clean test bootJar --no-daemon
```

В проекте присутствуют unit-, integration- и E2E-тесты. Для
интеграционных сценариев используется Testcontainers с PostgreSQL. Отчёт
JaCoCo формируется после выполнения тестов:

``` text
build/reports/jacoco/test/html/index.html
```

Отчёт Gradle по тестам:

``` text
build/reports/tests/test/index.html
```

## Docker-образ

Опубликованный образ:

``` text
ametisgame/tickets:latest
```

Загрузка образа:

``` bash
docker pull ametisgame/tickets:latest
```

Сборка локального образа:

``` bash
docker build -t ametisgame/tickets:latest .
```

Запуск приложения вместе с базой данных рекомендуется выполнять через
Docker Compose, чтобы приложение получило сетевой адрес PostgreSQL
внутри Compose-сети.

## CI/CD --- GitHub Actions

Workflow находится в `.github/workflows/ci-cd.yml` и запускается при:

-   `push` в ветку `master`;
-   `pull_request` в ветку `master`.

Pipeline выполняет следующие этапы:

1.  Получает исходный код репозитория.
2.  Настраивает JDK 17 и Gradle.
3.  Запускает PostgreSQL 16 как service container и ожидает готовности
    базы.
4.  Выполняет `clean test bootJar`.
5.  Настраивает Docker Buildx.
6.  При `push` в `master` авторизуется в Docker Hub.
7.  Собирает и публикует Docker-образ с двумя тегами:
    -   `ametisgame/tickets:latest`;
    -   `ametisgame/tickets:<commit-sha>`.

Для публикации образа в настройках GitHub Repository → **Settings →
Secrets and variables → Actions** должны быть добавлены secrets:

  -----------------------------------------------------------------------
  Secret                              Значение
  ----------------------------------- -----------------------------------
  `DOCKERHUB_USERNAME`                Имя пользователя Docker Hub

  `DOCKERHUB_TOKEN`                   Docker Hub Personal Access Token с
                                      правами на публикацию образов
  -----------------------------------------------------------------------

Токен не следует хранить в репозитории или указывать непосредственно в
workflow-файле.

## Конфигурация

Основные параметры приложения задаются в
`src/main/resources/application.yml` и могут переопределяться
переменными окружения:

  --------------------------------------------------------------------------------------------
  Переменная              Назначение              Значение по умолчанию
  ----------------------- ----------------------- --------------------------------------------
  `DB_URL`                JDBC URL PostgreSQL     `jdbc:postgresql://localhost:5432/tickets`

  `DB_USER`               Пользователь БД         `postgres`

  `DB_PASSWORD`           Пароль БД               `12345`
  --------------------------------------------------------------------------------------------

Для Docker Compose адрес базы данных внутри сети Compose задаётся как
`db:5432`. Не используйте `localhost` для подключения приложения из
контейнера к соседнему контейнеру PostgreSQL.

## Ссылки

-   GitHub: https://github.com/AmetisAndrey/Tickets
-   GitHub Actions: https://github.com/AmetisAndrey/Tickets/actions
-   Docker Hub: https://hub.docker.com/r/ametisgame/tickets
-   Swagger UI после запуска:
    http://localhost:8080/swagger-ui/index.html
-   Health check: http://localhost:8080/actuator/health

## Результат работы

В рамках проекта реализован backend для управления IT-заявками,
настроены миграции PostgreSQL, автоматизированные тесты, запуск
приложения в Docker Compose и CI/CD pipeline на GitHub Actions. Успешный
запуск pipeline и загрузка опубликованного Docker-образа
`ametisgame/tickets:latest` были проверены.
