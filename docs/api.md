# REST API сервиса обработки заявок

**Базовый URL:** `http://localhost:8080` · **Формат:** JSON · **Время:** ISO-8601, UTC. Swagger UI: `/swagger-ui.html`.

## Категории

### `GET /api/categories`

Пример ответа `200 OK`:

```json
[
  {"id":1,"name":"Hardware"},
  {"id":2,"name":"Software"},
  {"id":3,"name":"Network"},
  {"id":4,"name":"Access"}
]
```

ID назначаются БД при миграции.

## Заявки

### `POST /api/tickets` — создать заявку

```json
{
  "title":"Не работает принтер",
  "description":"Принтер не печатает документы",
  "categoryId":1,
  "priority":"HIGH",
  "dueAt":"2025-01-16T18:00:00Z"
}
```

Обязательны `title` (не пустой), `categoryId`, `priority`, `dueAt`; `description` необязателен. Приоритет: LOW/MEDIUM/HIGH. Успех — `201 Created`, `Location: /api/tickets/{id}` и тело:

```json
{
  "id":15,
  "title":"Не работает принтер",
  "description":"Принтер не печатает документы",
  "categoryId":1,
  "categoryName":"Hardware",
  "priority":"HIGH",
  "status":"NEW",
  "createdAt":"2025-01-15T12:00:00Z",
  "dueAt":"2025-01-16T18:00:00Z",
  "closedAt":null
}
```

`status` и `createdAt` назначает сервер. Ошибки: 400 — валидация; 404 — категория не найдена.

### `GET /api/tickets/{id}` — получить заявку

Ответ 200 — объект TicketResponse, как в примере выше. Неизвестный ID — 404.

### `PATCH /api/tickets/{id}` — редактировать заявку

```json
{"description":"Проверить подключение","priority":"MEDIUM","dueAt":"2025-01-17T12:00:00Z"}
```

Все поля необязательны; обновляются только ненулевые `description`, `priority`, `dueAt`. Заголовок и категория не редактируются. Ответ 200 — обновлённая заявка; 404 — нет заявки; 409 — заявка RESOLVED/CANCELLED.

### `PATCH /api/tickets/{id}/status` — изменить статус

```json
{"status":"IN_PROGRESS"}
```

Матрица переходов:

| Из | В | Результат |
|---|---|---|
| NEW | IN_PROGRESS | Разрешено |
| NEW | CANCELLED | Разрешено |
| NEW | RESOLVED | Запрещено |
| IN_PROGRESS | RESOLVED | Разрешено |
| IN_PROGRESS | CANCELLED | Разрешено |
| RESOLVED/CANCELLED | Любой | Запрещено |

Успех — 200; при RESOLVED/CANCELLED выставляется `closedAt`, история записывается в той же транзакции. Ошибки: 400 — некорректное тело; 404 — нет заявки; 409 — запрещённый переход.

### `GET /api/tickets/{id}/history` — история

Пример ответа 200:

```json
[
  {"id":1,"fromStatus":null,"toStatus":"NEW","changedAt":"2025-01-15T12:00:00Z"},
  {"id":2,"fromStatus":"NEW","toStatus":"IN_PROGRESS","changedAt":"2025-01-15T12:10:00Z"}
]
```

Сортировка по времени по возрастанию. Неизвестная заявка — 404.

### `GET /api/tickets` — список и фильтры

| Параметр | Тип | По умолчанию | Описание |
|---|---|---|---|
| status | enum | — | Статус |
| priority | enum | — | Приоритет |
| categoryId | Long | — | Категория |
| overdue | Boolean | — | `true` — просроченные открытые |
| page | int | 0 | Номер страницы с нуля |
| size | int | 20 | Размер страницы |

Пример: `GET /api/tickets?status=IN_PROGRESS&priority=HIGH&categoryId=1&page=0&size=10`.

Ответ — Spring Data `Page`: `content`, `totalElements`, `totalPages`, `number`, `size` и другие метаданные. Сортировка createdAt DESC. Замечание: `overdue=false` сейчас не добавляет фильтр непросроченных заявок (D-03).

## Отчёт

### `GET /api/reports/summary?from=...&to=...`

Пример: `GET /api/reports/summary?from=2025-01-01T00:00:00Z&to=2025-02-01T00:00:00Z`.

Выборка — `[from,to)`, то есть `createdAt >= from` и `createdAt < to`. `from` должен быть раньше `to`.

```json
{
  "totalCreated":12,
  "byStatus":{"NEW":3,"IN_PROGRESS":4,"RESOLVED":4,"CANCELLED":1},
  "byPriority":{"LOW":2,"MEDIUM":5,"HIGH":5},
  "openOverdue":2,
  "avgResolutionMinutes":84.5
}
```

Числа иллюстративные. `avgResolutionMinutes` — число или null, если нет завершённых заявок с closedAt. Неверный период — 400.

## Формат ошибок

Пример 409:

```json
{
  "timestamp":"2025-01-15T12:30:00Z",
  "status":409,
  "error":"Conflict",
  "message":"Illegal transition RESOLVED -> IN_PROGRESS",
  "path":"/api/tickets/15/status"
}
```

| HTTP | Значение |
|---|---|
| 200 | Успешное чтение/изменение |
| 201 | Создание |
| 400 | Ошибка валидации или период |
| 404 | Заявка/категория не найдена |
| 409 | Запрещённый переход или редактирование закрытой заявки |

## Примеры curl

```bash
curl http://localhost:8080/api/categories
curl -X POST http://localhost:8080/api/tickets -H 'Content-Type: application/json' -d '{"title":"Принтер","description":"Не печатает","categoryId":1,"priority":"HIGH","dueAt":"2025-01-16T18:00:00Z"}'
curl http://localhost:8080/api/tickets/1
curl -X PATCH http://localhost:8080/api/tickets/1/status -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}'
curl http://localhost:8080/api/tickets/1/history
curl 'http://localhost:8080/api/reports/summary?from=2025-01-01T00:00:00Z&to=2025-02-01T00:00:00Z'
```
