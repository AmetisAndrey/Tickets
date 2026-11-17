const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const state = { page: 0, size: 8, totalPages: 0, totalElements: 0, categories: [], tickets: [], search: '', selected: null };
const STATUS = { NEW: 'Новая', IN_PROGRESS: 'В работе', RESOLVED: 'Решена', CANCELLED: 'Отменена' };
const PRIORITY = { HIGH: 'Высокий', MEDIUM: 'Средний', LOW: 'Низкий' };
const STATUS_CLASS = { NEW: 'status-new', IN_PROGRESS: 'status-progress', RESOLVED: 'status-resolved', CANCELLED: 'status-cancelled' };
const PRIORITY_CLASS = { HIGH: 'priority-high', MEDIUM: 'priority-medium', LOW: 'priority-low' };
const COLORS = { NEW: '#e8b45b', IN_PROGRESS: '#4779e8', RESOLVED: '#36b58a', CANCELLED: '#cbd2dc' };
const fmtDate = value => value ? new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value)) : '—';
const fmtDateTime = value => value ? new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '—';
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const now = new Date();
const startMonth = new Date(now.getFullYear(), now.getMonth(), 1);
const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
const isoLocal = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
const toIso = value => new Date(value).toISOString();

async function api(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { ...(options.body ? {'Content-Type':'application/json'} : {}), ...(options.headers || {}) } });
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  if (!response.ok) {
    const message = data?.message || data?.detail || data?.error || `Ошибка сервера (${response.status})`;
    throw new Error(message);
  }
  return data;
}
function toast(message, error = false) {
  const el = $('#toast'); el.textContent = message; el.classList.toggle('error', error); el.classList.add('show');
  clearTimeout(toast.timer); toast.timer = setTimeout(() => el.classList.remove('show'), 3200);
}
function statusBadge(status) { return `<span class="status-pill ${STATUS_CLASS[status] || ''}">${esc(STATUS[status] || status)}</span>`; }
function priorityBadge(priority) { return `<span class="priority-pill ${PRIORITY_CLASS[priority] || ''}">${esc(PRIORITY[priority] || priority)}</span>`; }
function categoryName(id) { return state.categories.find(c => Number(c.id) === Number(id))?.name || 'Без категории'; }
function isOverdue(ticket) { return ['NEW','IN_PROGRESS'].includes(ticket.status) && new Date(ticket.dueAt) < new Date(); }

async function loadCategories() {
  state.categories = await api('/api/categories');
  const options = state.categories.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  $('#category-filter').insertAdjacentHTML('beforeend', options);
  $('#create-category').innerHTML = options;
}
async function loadTickets() {
  const params = new URLSearchParams({ page: state.page, size: state.size });
  for (const [id, key] of [['status-filter','status'],['priority-filter','priority'],['category-filter','categoryId']]) {
    const value = $(`#${id}`).value; if (value) params.set(key, value);
  }
  const data = await api(`/api/tickets?${params}`);
  state.tickets = data.content || [];
  state.totalPages = data.totalPages || 0;
  state.totalElements = data.totalElements || 0;
  $('#nav-total').textContent = state.totalElements;
  $('#table-count').textContent = `${state.totalElements} заявок`;
  $('#page-label').textContent = state.totalPages ? `${state.page + 1} / ${state.totalPages}` : '0 / 0';
  $('#prev-page').disabled = state.page <= 0;
  $('#next-page').disabled = state.totalPages === 0 || state.page >= state.totalPages - 1;
  renderTickets();
}
function renderTickets() {
  const query = state.search.trim().toLowerCase();
  const rows = state.tickets.filter(t => !query || String(t.id).includes(query) || (t.title || '').toLowerCase().includes(query));
  if (!rows.length) {
    $('#tickets-body').innerHTML = `<tr><td colspan="6" class="empty-cell">${query ? 'По вашему запросу ничего не найдено' : 'Заявок пока нет. Создайте первую заявку.'}</td></tr>`;
    return;
  }
  $('#tickets-body').innerHTML = rows.map(t => `<tr data-ticket="${t.id}">
    <td><div class="ticket-cell"><div class="ticket-avatar">${esc((t.title || 'З').slice(0,1).toUpperCase())}</div><div><div class="ticket-title">${esc(t.title)}</div><div class="ticket-id">#T-${String(t.id).padStart(4,'0')} · ${fmtDate(t.createdAt)}</div></div></div></td>
    <td>${esc(t.categoryName || categoryName(t.categoryId))}</td>
    <td>${priorityBadge(t.priority)}</td><td>${statusBadge(t.status)}</td>
    <td class="${isOverdue(t) ? 'due-overdue' : 'due-normal'}">${fmtDate(t.dueAt)}${isOverdue(t) ? ' · просрочено' : ''}</td>
    <td><button class="row-more" aria-label="Открыть заявку">···</button></td></tr>`).join('');
  $$('tr[data-ticket]').forEach(row => row.addEventListener('click', () => openTicket(row.dataset.ticket)));
}
async function loadReport() {
  const from = startMonth.toISOString(), to = nextMonth.toISOString();
  $('#date-range-label').textContent = `${fmtDate(from)} — ${fmtDate(new Date(nextMonth.getTime()-1))}`;
  const report = await api(`/api/reports/summary?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
  $('#stat-total').textContent = report.totalCreated ?? 0;
  $('#stat-progress').textContent = report.byStatus?.IN_PROGRESS ?? 0;
  $('#stat-overdue').textContent = report.openOverdue ?? 0;
  $('#stat-average').innerHTML = report.avgResolutionMinutes == null ? '—<small> мин</small>' : `${Math.round(report.avgResolutionMinutes)}<small> мин</small>`;
  $('#donut-total').textContent = report.totalCreated ?? 0;
  $('#priority-total').textContent = report.totalCreated ?? 0;
  renderStatus(report.byStatus || {}, report.totalCreated || 0);
  renderPriorities(report.byPriority || {}, report.totalCreated || 0);
}
function renderStatus(values, total) {
  const keys = ['NEW','IN_PROGRESS','RESOLVED','CANCELLED'];
  const sum = keys.reduce((s,k)=>s+(values[k]||0),0);
  let offset = 0;
  const segments = keys.map(k => {
    const count = values[k] || 0, start = offset; offset += sum ? count/sum*100 : 0;
    return `${COLORS[k]} ${start}% ${offset}%`;
  });
  $('#status-donut').style.background = sum ? `conic-gradient(${segments.join(',')})` : '#e9edf2';
  $('#status-legend').innerHTML = keys.map(k => `<div class="legend-item"><span class="legend-dot" style="background:${COLORS[k]}"></span><span>${STATUS[k]}</span><b>${values[k] || 0}</b></div>`).join('');
}
function renderPriorities(values, total) {
  const keys = ['HIGH','MEDIUM','LOW'], colors = {HIGH:'high',MEDIUM:'medium',LOW:'low'};
  $('#priority-list').innerHTML = keys.map(k => {
    const n = values[k] || 0, width = total ? n/total*100 : 0;
    return `<div class="priority-row"><span>${PRIORITY[k]}</span><div class="priority-track"><div class="priority-fill ${colors[k]}" style="width:${width}%"></div></div><b>${n}</b></div>`;
  }).join('');
}
async function refreshAll() {
  $('#refresh-btn').classList.add('spinning');
  try { await Promise.all([loadTickets(), loadReport()]); }
  catch (e) { toast(e.message, true); }
  finally { $('#refresh-btn').classList.remove('spinning'); }
}
function openDialog(id) { $(`#${id}`).showModal(); }
function closeDialog(id) { $(`#${id}`).close(); }
$$('[data-close]').forEach(b => b.addEventListener('click', () => closeDialog(b.dataset.close)));
$('#create-btn').addEventListener('click', () => openDialog('create-modal'));
$('#create-due').value = isoLocal(new Date(Date.now() + 24*60*60*1000));
$('#create-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = new FormData(e.currentTarget);
  const payload = { title: form.get('title').trim(), description: form.get('description') || null, categoryId: Number(form.get('categoryId')), priority: form.get('priority'), dueAt: toIso(form.get('dueAt')) };
  $('#create-error').textContent = '';
  const submit = $('button[type="submit"]', e.currentTarget); submit.disabled = true; submit.textContent = 'Создаём…';
  try {
    await api('/api/tickets', { method:'POST', body:JSON.stringify(payload) });
    closeDialog('create-modal'); e.currentTarget.reset(); $('#create-due').value = isoLocal(new Date(Date.now()+86400000));
    state.page = 0; await refreshAll(); toast('Заявка успешно создана');
  } catch (err) { $('#create-error').textContent = err.message; }
  finally { submit.disabled = false; submit.innerHTML = 'Создать заявку <span>→</span>'; }
});
async function openTicket(id) {
  try {
    const [t, history] = await Promise.all([api(`/api/tickets/${id}`), api(`/api/tickets/${id}/history`)]);
    state.selected = t;
    $('#detail-id').textContent = `ЗАЯВКА #T-${String(t.id).padStart(4,'0')}`;
    $('#detail-title').textContent = t.title;
    $('#detail-subtitle').textContent = `${t.categoryName || categoryName(t.categoryId)} · создана ${fmtDateTime(t.createdAt)}`;
    const next = t.status === 'NEW' ? ['IN_PROGRESS','CANCELLED'] : t.status === 'IN_PROGRESS' ? ['RESOLVED','CANCELLED'] : [];
    $('#detail-content').innerHTML = `
      <div class="detail-grid">
        <div class="detail-item"><small>СТАТУС</small>${statusBadge(t.status)}</div>
        <div class="detail-item"><small>ПРИОРИТЕТ</small>${priorityBadge(t.priority)}</div>
        <div class="detail-item"><small>СРОК ВЫПОЛНЕНИЯ</small><b class="${isOverdue(t)?'due-overdue':''}">${fmtDateTime(t.dueAt)}${isOverdue(t)?' · просрочено':''}</b></div>
        <div class="detail-item"><small>ЗАКРЫТА</small><b>${fmtDateTime(t.closedAt)}</b></div>
      </div>
      <div><div class="eyebrow" style="margin-bottom:8px">ОПИСАНИЕ</div><div class="detail-description">${esc(t.description || 'Описание не добавлено')}</div></div>
      <div class="detail-actions">${next.map(s=>`<button class="button ${s==='RESOLVED'?'button-primary':'button-outline'}" data-next-status="${s}">${s==='IN_PROGRESS'?'Начать работу':s==='RESOLVED'?'Отметить решённой':'Отменить заявку'}</button>`).join('')}</div>
      <div><h3 class="history-title">История изменений</h3><div class="history-list">${history.length ? history.map(h=>`<div class="history-item"><span class="history-marker"></span><div><b>${h.fromStatus ? STATUS[h.fromStatus] : 'Создана'} → ${STATUS[h.toStatus] || h.toStatus}</b><small>${fmtDateTime(h.changedAt)}</small></div></div>`).join('') : '<div class="legend-empty">История пока пуста</div>'}</div></div>`;
    $$('[data-next-status]', $('#detail-content')).forEach(btn => btn.addEventListener('click', () => changeStatus(t.id, btn.dataset.nextStatus)));
    openDialog('detail-modal');
  } catch (e) { toast(e.message, true); }
}
async function changeStatus(id, status) {
  try {
    await api(`/api/tickets/${id}/status`, { method:'PATCH', body:JSON.stringify({status}) });
    toast(`Статус изменён: ${STATUS[status]}`); closeDialog('detail-modal'); await refreshAll(); await openTicket(id);
  } catch (e) { toast(e.message, true); }
}
$('#status-filter').addEventListener('change', () => { state.page=0; loadTickets().catch(e=>toast(e.message,true)); });
$('#priority-filter').addEventListener('change', () => { state.page=0; loadTickets().catch(e=>toast(e.message,true)); });
$('#category-filter').addEventListener('change', () => { state.page=0; loadTickets().catch(e=>toast(e.message,true)); });
$('#search-input').addEventListener('input', e => { state.search=e.target.value; renderTickets(); });
$('#prev-page').addEventListener('click', () => { if(state.page>0){state.page--;loadTickets().catch(e=>toast(e.message,true));} });
$('#next-page').addEventListener('click', () => { if(state.page+1<state.totalPages){state.page++;loadTickets().catch(e=>toast(e.message,true));} });
$('#refresh-btn').addEventListener('click', refreshAll);
$('#all-tickets-btn').addEventListener('click', () => $('#tickets').scrollIntoView({behavior:'smooth'}));
$('#today-label').textContent = new Intl.DateTimeFormat('ru-RU',{weekday:'short',day:'numeric',month:'long'}).format(new Date());
$('#crumb-current').textContent = 'Обзор';
(async function init(){
  try { await loadCategories(); await refreshAll(); }
  catch (e) { toast(`Не удалось загрузить данные API: ${e.message}`, true); $('#tickets-body').innerHTML='<tr><td colspan="6" class="empty-cell">Не удалось подключиться к серверу. Проверьте, что приложение запущено.</td></tr>'; }
})();
