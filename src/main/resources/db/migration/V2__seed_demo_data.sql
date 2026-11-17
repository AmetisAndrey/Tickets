-- Демонстрационные данные для локальной разработки.
-- Миграция выполняется один раз Flyway и не затрагивает существующие записи.
INSERT INTO tickets (title, description, category_id, priority, status, created_at, due_at, closed_at)
SELECT demo.title, demo.description, c.id, demo.priority, demo.status,
       now() - demo.created_ago,
       now() + demo.due_in,
       CASE
         WHEN demo.status IN ('RESOLVED', 'CANCELLED') THEN now() - demo.closed_ago
         ELSE NULL
       END
FROM (VALUES
  ('Не работает VPN после обновления', 'После обновления клиента VPN соединение обрывается через 10–15 секунд. Проверить профиль и сертификат.', 'Network', 'HIGH', 'IN_PROGRESS', interval '2 hours', interval '3 hours', interval '1 hour'),
  ('Запрос доступа к Jira', 'Нужен доступ к проекту разработки для нового сотрудника отдела.', 'Access', 'MEDIUM', 'NEW', interval '3 hours', interval '1 day', interval '0 hours'),
  ('Ноутбук перегревается', 'Вентилятор постоянно работает на высокой скорости даже при небольшой нагрузке.', 'Hardware', 'MEDIUM', 'NEW', interval '7 hours', interval '1 day', interval '0 hours'),
  ('Ошибка при запуске 1С', 'Приложение закрывается сразу после ввода учетных данных.', 'Software', 'HIGH', 'IN_PROGRESS', interval '1 day', interval '2 hours', interval '5 hours'),
  ('Настроить второй монитор', 'Рабочая станция не определяет монитор через USB-C док-станцию.', 'Hardware', 'LOW', 'RESOLVED', interval '2 days', interval '1 day', interval '1 day 20 hours'),
  ('Нет доступа к общей папке', 'При открытии сетевого диска появляется сообщение об отказе в доступе.', 'Access', 'HIGH', 'NEW', interval '1 day 4 hours', interval '4 hours', interval '0 hours'),
  ('Медленно загружается корпоративный портал', 'Страницы портала открываются более 30 секунд из офисной сети.', 'Network', 'MEDIUM', 'IN_PROGRESS', interval '2 days', interval '5 hours', interval '1 day'),
  ('Установить обновление браузера', 'Требуется обновить браузер на рабочем месте переговорной комнаты.', 'Software', 'LOW', 'RESOLVED', interval '3 days', interval '2 days', interval '2 days 20 hours'),
  ('Сбросить пароль учетной записи', 'Пользователь забыл пароль и не может пройти MFA.', 'Access', 'MEDIUM', 'RESOLVED', interval '4 days', interval '2 days', interval '3 days 22 hours'),
  ('Периодически пропадает Wi-Fi', 'На третьем этаже пропадает беспроводная сеть, кабельное подключение работает.', 'Network', 'HIGH', 'NEW', interval '5 days', interval '2 hours', interval '0 hours'),
  ('Замена клавиатуры', 'Несколько клавиш не реагируют на нажатия. Нужна замена устройства.', 'Hardware', 'LOW', 'CANCELLED', interval '6 days', interval '3 days', interval '5 days'),
  ('Не открывается PDF-вложение', 'При скачивании вложения из почты файл отображается поврежденным.', 'Software', 'MEDIUM', 'RESOLVED', interval '7 days', interval '2 days', interval '6 days 20 hours'),
  ('Настроить MFA на телефоне', 'Помочь подключить приложение-аутентификатор на новом смартфоне.', 'Access', 'MEDIUM', 'IN_PROGRESS', interval '8 days', interval '1 day', interval '7 days'),
  ('Шумит блок питания', 'У системного блока появился сильный высокочастотный шум.', 'Hardware', 'HIGH', 'CANCELLED', interval '9 days', interval '2 days', interval '8 days'),
  ('Проверить сетевой принтер', 'Печать зависает в очереди у нескольких сотрудников бухгалтерии.', 'Network', 'MEDIUM', 'RESOLVED', interval '10 days', interval '3 days', interval '9 days 20 hours'),
  ('Запрос лицензии графического редактора', 'Нужна временная лицензия на время подготовки презентации.', 'Software', 'LOW', 'NEW', interval '12 days', interval '3 days', interval '0 hours'),
  ('Не синхронизируется календарь', 'События на телефоне не совпадают с календарем в веб-версии.', 'Software', 'MEDIUM', 'IN_PROGRESS', interval '13 days', interval '1 day', interval '12 days'),
  ('Подключить гостевой Wi-Fi', 'Подготовить доступ к гостевой сети для посетителей офиса.', 'Network', 'LOW', 'RESOLVED', interval '15 days', interval '5 days', interval '14 days 20 hours')
) AS demo(title, description, category_name, priority, status, created_ago, due_in, closed_ago)
JOIN categories c ON c.name = demo.category_name
WHERE NOT EXISTS (SELECT 1 FROM tickets existing WHERE existing.title = demo.title);

-- Начальное событие для каждой демонстрационной заявки.
INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at)
SELECT t.id, NULL, 'NEW', t.created_at
FROM tickets t
WHERE t.title IN (
  'Не работает VPN после обновления', 'Запрос доступа к Jira', 'Ноутбук перегревается',
  'Ошибка при запуске 1С', 'Настроить второй монитор', 'Нет доступа к общей папке',
  'Медленно загружается корпоративный портал', 'Установить обновление браузера',
  'Сбросить пароль учетной записи', 'Периодически пропадает Wi-Fi', 'Замена клавиатуры',
  'Не открывается PDF-вложение', 'Настроить MFA на телефоне', 'Шумит блок питания',
  'Проверить сетевой принтер', 'Запрос лицензии графического редактора',
  'Не синхронизируется календарь', 'Подключить гостевой Wi-Fi'
)
AND NOT EXISTS (
  SELECT 1 FROM ticket_status_history h
  WHERE h.ticket_id = t.id AND h.from_status IS NULL AND h.to_status = 'NEW'
);

-- История переходов для заявок, которые уже двигались по workflow.
INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at)
SELECT t.id, 'NEW', 'IN_PROGRESS', t.created_at + interval '30 minutes'
FROM tickets t
WHERE t.title IN (
  'Не работает VPN после обновления', 'Ошибка при запуске 1С',
  'Медленно загружается корпоративный портал', 'Настроить MFA на телефоне',
  'Не синхронизируется календарь'
)
AND NOT EXISTS (
  SELECT 1 FROM ticket_status_history h
  WHERE h.ticket_id = t.id AND h.to_status = 'IN_PROGRESS'
);

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at)
SELECT t.id, 'IN_PROGRESS', 'RESOLVED', COALESCE(t.closed_at, now())
FROM tickets t
WHERE t.title IN (
  'Настроить второй монитор', 'Установить обновление браузера',
  'Сбросить пароль учетной записи', 'Не открывается PDF-вложение',
  'Проверить сетевой принтер', 'Подключить гостевой Wi-Fi'
)
AND NOT EXISTS (
  SELECT 1 FROM ticket_status_history h
  WHERE h.ticket_id = t.id AND h.to_status = 'RESOLVED'
);

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at)
SELECT t.id,
       CASE WHEN t.title = 'Замена клавиатуры' THEN 'NEW' ELSE 'IN_PROGRESS' END,
       'CANCELLED', COALESCE(t.closed_at, now())
FROM tickets t
WHERE t.title IN ('Замена клавиатуры', 'Шумит блок питания')
AND NOT EXISTS (
  SELECT 1 FROM ticket_status_history h
  WHERE h.ticket_id = t.id AND h.to_status = 'CANCELLED'
);
