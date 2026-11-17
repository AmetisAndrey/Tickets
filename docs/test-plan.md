# План тестирования

## 1. Цель и область

Проверить REST API, бизнес-правила статусов, валидацию, историю, фильтры, расчёты Kotlin-отчёта, PostgreSQL/Flyway и полный жизненный цикл. UI, авторизация и внешние интеграции не входят в текущую область.

## 2. Уровни

| Уровень | Минимум задания | Назначение | Инструменты |
|---|---:|---|---|
| Модульные | 10 | Бизнес-правила и расчёты | JUnit 5, Mockito, AssertJ |
| Интеграционные | 6 | REST, PostgreSQL, Flyway | Spring Boot Test, MockMvc, Testcontainers |
| Сквозные | 2 | Полный HTTP-сценарий | SpringBootTest, TestRestTemplate, Testcontainers |
| Kotlin | 3+ | Kotlin и вызов Java-компонента | JUnit 5, Mockito-Kotlin |

## 3. Тестовые данные и время

Интеграционные/E2E-тесты используют PostgreSQL 16 через Testcontainers. Начальные категории загружаются Flyway. В unit-тестах используется фиксированный Clock; E2E использует изменяемый Clock. Проверять `dueAt == now` и `dueAt < now`: по текущему правилу равенство не является просрочкой.

## 4. Матрица тестов

| ID | Уровень | Сценарий | Ожидаемый результат | Требование |
|---|---|---|---|---|
| TC-01 | Unit | Создать заявку | NEW, время из Clock, история создана | FR-01/02/07 |
| TC-02 | Unit | NEW → IN_PROGRESS | Разрешено | FR-06 |
| TC-03 | Unit | NEW → CANCELLED | Разрешено | FR-06 |
| TC-04 | Unit | NEW → RESOLVED | Запрещено | FR-06 |
| TC-05 | Unit | IN_PROGRESS → RESOLVED | Разрешено | FR-06 |
| TC-06 | Unit | RESOLVED → IN_PROGRESS | Запрещено | FR-05/06 |
| TC-07 | Unit | CANCELLED → NEW | Запрещено | FR-05/06 |
| TC-08 | Unit | dueAt == now, статус NEW | Не просрочена | FR-11 |
| TC-09 | Unit | dueAt < now, статус NEW | Просрочена | FR-11 |
| TC-10 | Unit | RESOLVED с истёкшим сроком | Не открытая просрочка | FR-11 |
| TC-11 | Unit | Редактировать CANCELLED | ConflictException | FR-05 |
| TC-12 | Kotlin | Пустая выборка | total=0, average=null | FR-13 |
| TC-13 | Kotlin | Нет RESOLVED | average=null | FR-13 |
| TC-14 | Kotlin | Распределения, просрочка, среднее | Значения корректны | FR-13 |
| TC-15 | Kotlin | from >= to | IllegalArgumentException / HTTP 400 | FR-13 |
| TC-16 | Integration | Flyway и seed | 4 категории доступны | NFR-05/FR-12 |
| TC-17 | Integration | Создание и история | Начальная запись NEW есть | FR-01/07 |
| TC-18 | Integration | Неизвестный ID | HTTP 404 | FR-03 |
| TC-19 | Integration | NEW → RESOLVED | HTTP 409, статус NEW | FR-06 |
| TC-20 | Integration | Фильтр status + priority | Страница возвращена | FR-09/10 |
| TC-21 | Integration | Endpoint отчёта | 200 и поля summary | FR-13 |
| TC-22 | E2E | Создание → IN_PROGRESS → RESOLVED | closedAt и 3 записи истории | US-01/04/05/06 |
| TC-23 | E2E | Повторно открыть RESOLVED | 409, данные/история неизменны | US-05 |

## 5. Граничные случаи

| Случай | Ожидаемое поведение |
|---|---|
| Пустая выборка отчёта | totalCreated=0, карты пустые, average=null |
| Нет завершённых | average=null |
| from == to или from > to | HTTP 400 |
| createdAt == from | Включить |
| createdAt == to | Исключить |
| dueAt == now | Не просрочена |
| dueAt < now, статус NEW/IN_PROGRESS | Просрочена |
| dueAt < now, статус RESOLVED/CANCELLED | Не открытая просрочка |
| Неизвестный ID | HTTP 404 |
| Неизвестная категория | HTTP 404 |
| Запрещённый переход | HTTP 409; статус и история без изменений |

## 6. Недостающие проверки

В найденных тестах не обнаружены отдельные проверки HTTP-валидации пустого title, неизвестной категории при создании, `overdue=true/false`, точных границ `[from,to)` и реального Java-адаптера отчёта. `TicketReportDataPortTest.kt` использует Mockito mock интерфейса, а не вызывает `JpaTicketReportDataAdapter`. Для требования о Kotlin-тесте Java-компонента следует добавить Kotlin integration test с Spring context/Testcontainers и реальным адаптером.

## 7. Запуск и отчётность

```bash
./gradlew test
./gradlew test --info
```

Для Testcontainers нужен работающий Docker daemon. После запуска сохранить итог Gradle, число passed/failed/skipped, XML/HTML отчёты из `build/test-results/` и `build/reports/tests/`, а также отчёт покрытия Java/Kotlin (например, JaCoCo). В рамках подготовки документа тесты не запускались; успешный результат не заявляется.