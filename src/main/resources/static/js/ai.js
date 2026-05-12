document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('ai-form');
  if (!form) return;
  const input = document.getElementById('ai-input');
  const result = document.getElementById('ai-result');
  const btn = document.getElementById('ai-submit');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const q = (input.value || '').trim();
    if (!q) return;
    btn.disabled = true;
    result.innerHTML = '<div>AI 推荐中…</div>';
    try {
      const resp = await fetch('/api/ai/recommend', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ query: q })
      });
      if (resp.status === 401) { result.innerHTML = '<div class="error">请先登录</div>'; return; }
      const data = await resp.json();
      if (data.error) { result.innerHTML = '<div class="error">' + escapeHtml(data.error) + '</div>'; return; }
      if (!data.items || data.items.length === 0) { result.innerHTML = '<div>暂无推荐</div>'; return; }
      result.innerHTML = data.items.map(it => `
        <div class="ai-result-item">
          <div>
            <strong>${escapeHtml(it.name)}</strong> <span class="price">￥${it.price}</span>
            <div style="color:#666;font-size:13px;margin-top:4px;">${escapeHtml(it.reason || '')}</div>
          </div>
          <form method="post" action="/cart/add">
            <input type="hidden" name="menuItemId" value="${it.id}"/>
            <input type="hidden" name="quantity" value="1"/>
            <input type="hidden" name="redirect" value="/menu"/>
            <button class="btn btn-primary" type="submit">加入购物车</button>
          </form>
        </div>`).join('');
    } catch (err) {
      result.innerHTML = '<div class="error">AI 暂不可用，请稍后再试</div>';
    } finally { btn.disabled = false; }
  });

  function escapeHtml(s){ return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
});
